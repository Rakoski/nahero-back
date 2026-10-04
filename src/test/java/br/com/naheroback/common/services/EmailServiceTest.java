package br.com.naheroback.common.services;

import br.com.naheroback.common.configs.ErrorMessageConfig;
import freemarker.template.Configuration;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailServiceTest {

    private JavaMailSender mailSender;
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        Configuration freemarker = new Configuration(Configuration.VERSION_2_3_32);
        freemarker.setClassForTemplateLoading(EmailServiceTest.class, "/templates/");
        freemarker.setDefaultEncoding("UTF-8");

        mailSender = mock(JavaMailSender.class);
        when(mailSender.createMimeMessage()).thenAnswer(invocation -> new MimeMessage(Session.getInstance(new Properties())));

        emailService = new EmailService(freemarker, mailSender, new ErrorMessageConfig().messageSource());
        ReflectionTestUtils.setField(emailService, "fromSupport", "contato@nahero.site");
        ReflectionTestUtils.setField(emailService, "frontendUrl", "https://nahero.site");
    }

    @Test
    @DisplayName("Should fill the result follow-up subject from the model in Portuguese")
    void shouldRenderTheResultFollowupInPortuguese() throws Exception {
        emailService.sendReengagementEmail("aluno@example.com", "Ana", "email.reengagement.result_followup",
                Locale.forLanguageTag("pt"), followupModel());

        assertEquals("Você fez 40/65 no AWS Cloud Practitioner — Security and Compliance foi seu ponto fraco",
                sentMessage().getSubject());
    }

    @Test
    @DisplayName("Should fill the result follow-up subject from the model in English")
    void shouldRenderTheResultFollowupInEnglish() throws Exception {
        emailService.sendReengagementEmail("student@example.com", "Ana", "email.reengagement.result_followup",
                Locale.ENGLISH, followupModel());

        assertEquals("You scored 40/65 on AWS Cloud Practitioner — Security and Compliance was your weak spot",
                sentMessage().getSubject());
    }

    @Test
    @DisplayName("Should keep rendering the static re-engagement emails through the old signature")
    void shouldKeepTheOldSignatureWorking() throws Exception {
        emailService.sendReengagementEmail("student@example.com", "Ana", "email.reengagement.we_miss_you",
                "https://nahero.site/en/practice-exams", Locale.ENGLISH);

        assertEquals("We saved your spot | Nahero", sentMessage().getSubject());
    }

    @Test
    @DisplayName("Should render the announcement with its feature list and the unsubscribe link")
    void shouldRenderTheAnnouncement() throws Exception {
        emailService.sendAnnouncementEmail("aluno@example.com", "Ana", "email.announcement.feedback_launch",
                Locale.forLanguageTag("pt"), "https://nahero.site/pt/how-it-works",
                "https://api.nahero.site/reengagement/unsubscribe?token=abc");

        MimeMessage sent = sentMessage();
        sent.saveChanges();
        String body = html(sent);

        assertEquals("Novidade na NaHero: saiba exatamente o que estudar", sent.getSubject());
        assertNotNull(body);
        assertTrue(body.contains("Página de feedback: suas últimas tentativas"));
        assertTrue(body.contains("Um painel com quantas questões você respondeu"));
        assertTrue(body.contains("https://api.nahero.site/reengagement/unsubscribe?token=abc"));
        assertTrue(body.contains("Parar de receber lembretes e novidades"));
    }

    @Test
    @DisplayName("Should keep the static re-engagement emails free of an unsubscribe block they did not ask for")
    void shouldNotRenderAnUnsubscribeBlockWithoutALink() throws Exception {
        emailService.sendReengagementEmail("student@example.com", "Ana", "email.reengagement.we_miss_you",
                "https://nahero.site/en/practice-exams", Locale.ENGLISH);

        MimeMessage sent = sentMessage();
        sent.saveChanges();

        assertFalse(html(sent).contains("Stop receiving reminders and updates"));
    }

    private static String html(Part part) throws Exception {
        if (part.isMimeType("text/html")) return (String) part.getContent();
        if (part.getContent() instanceof Multipart multipart) {
            for (int index = 0; index < multipart.getCount(); index++) {
                String found = html(multipart.getBodyPart(index));
                if (found != null) return found;
            }
        }
        return null;
    }

    private Map<String, Object> followupModel() {
        return Map.of(
                "examTitle", "AWS Cloud Practitioner",
                "score", 40,
                "total", 65,
                "weakestDomain", "Security and Compliance",
                "weakestCorrect", 3,
                "weakestTotal", 12,
                "actionLink", "https://nahero.site/pt/practice-exams/aws-cloud-practitioner-clf-02");
    }

    private MimeMessage sentMessage() {
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        return captor.getValue();
    }
}
