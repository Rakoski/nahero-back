package br.com.naheroback.common.services;

import freemarker.template.Configuration;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "EMAIL_SERVICE")
public class EmailService {

    private static final String LOGO_CONTENT_ID = "naheroLogo";
    private static final String LOGO_RESOURCE = "static/nahero-logo.png";
    private static final String PASSWORD_RESET_TEMPLATE = "forgotPassword.ftlh";
    private static final String EMAIL_VERIFICATION_TEMPLATE = "emailVerification.ftlh";
    private static final String REENGAGEMENT_TEMPLATE = "reengagement.ftlh";

    private static final List<String> PASSWORD_RESET_KEYS = List.of(
            "email.password_reset.subject",
            "email.password_reset.preheader",
            "email.password_reset.greeting",
            "email.password_reset.intro",
            "email.password_reset.cta",
            "email.password_reset.fallback",
            "email.password_reset.expiration",
            "email.password_reset.ignore",
            "email.password_reset.signature",
            "email.common.team",
            "email.common.tagline"
    );

    private static final List<String> EMAIL_VERIFICATION_KEYS = List.of(
            "email.email_verification.subject",
            "email.email_verification.preheader",
            "email.email_verification.greeting",
            "email.email_verification.intro",
            "email.email_verification.highlight",
            "email.email_verification.cta",
            "email.email_verification.fallback",
            "email.email_verification.expiration",
            "email.email_verification.ignore",
            "email.email_verification.signature",
            "email.common.team",
            "email.common.tagline"
    );

    private static final List<String> REENGAGEMENT_COMMON_KEYS = List.of(
            "email.reengagement.common.fallback",
            "email.reengagement.common.signature",
            "email.reengagement.common.unsubscribe_intro",
            "email.reengagement.common.unsubscribe_cta",
            "email.common.team",
            "email.common.tagline"
    );

    private static final List<String> REENGAGEMENT_CONTENT_KEYS = List.of(
            "subject",
            "preheader",
            "greeting",
            "intro",
            "highlight",
            "cta"
    );

    private static final List<String> REENGAGEMENT_MESSAGE_ARGUMENTS = List.of(
            "score",
            "total",
            "examTitle",
            "weakestDomain",
            "weakestCorrect",
            "weakestTotal"
    );

    private final Configuration freemarkerConfiguration;
    private final JavaMailSender mailSender;
    private final MessageSource messageSource;

    @Value("${app.support-email}")
    private String fromSupport;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${app.password-reset.expiration-minutes}")
    private Integer expirationMinutes;

    @Value("${app.email-verification.expiration-hours}")
    private Integer verificationExpirationHours;

    public void sendPasswordResetEmail(String to, String name, String token) {
        Locale locale = LocaleContextHolder.getLocale();
        Map<String, Object> model = translate(PASSWORD_RESET_KEYS, locale, name, expirationMinutes);

        model.put("name", name);
        model.put("logoUrl", "cid:" + LOGO_CONTENT_ID);
        model.put("supportEmail", fromSupport);
        model.put("siteUrl", frontendUrl);
        model.put("link", buildPasswordResetLink(token, locale));

        String body = render(PASSWORD_RESET_TEMPLATE, model);
        sendHtmlWithLogo(to, (String) model.get("subject"), body);
    }

    public void sendEmailVerificationEmail(String to, String name, String token) {
        Locale locale = LocaleContextHolder.getLocale();
        Map<String, Object> model = translate(EMAIL_VERIFICATION_KEYS, locale, name, verificationExpirationHours);

        model.put("name", name);
        model.put("logoUrl", "cid:" + LOGO_CONTENT_ID);
        model.put("supportEmail", fromSupport);
        model.put("siteUrl", frontendUrl);
        model.put("link", buildEmailVerificationLink(token, locale));

        String body = render(EMAIL_VERIFICATION_TEMPLATE, model);
        sendHtmlWithLogo(to, (String) model.get("subject"), body);
    }

    public void sendReengagementEmail(String to, String name, String messagePrefix, String actionLink, Locale locale) {
        sendReengagementEmail(to, name, messagePrefix, locale, Map.of("actionLink", actionLink));
    }

    public void sendReengagementEmail(String to, String name, String messagePrefix, Locale locale, Map<String, Object> content) {
        Object[] contentArguments = REENGAGEMENT_MESSAGE_ARGUMENTS.stream().map(content::get).toArray();

        Map<String, Object> model = new HashMap<>(content);
        model.putAll(translate(REENGAGEMENT_COMMON_KEYS, locale, name));
        model.putAll(translate(List.of(messagePrefix + ".greeting"), locale, name));
        model.putAll(translate(REENGAGEMENT_CONTENT_KEYS.stream()
                .filter(key -> !key.equals("greeting"))
                .map(key -> "%s.%s".formatted(messagePrefix, key))
                .toList(), locale, contentArguments));

        model.put("name", name);
        model.put("logoUrl", "cid:" + LOGO_CONTENT_ID);
        model.put("supportEmail", fromSupport);
        model.put("siteUrl", frontendUrl);
        model.put("link", content.get("actionLink"));

        String body = render(REENGAGEMENT_TEMPLATE, model);
        sendHtmlWithLogo(to, (String) model.get("subject"), body);
    }

    public void sendAnnouncementEmail(String to, String name, String messagePrefix, Locale locale, String actionLink, String unsubscribeLink) {
        Map<String, Object> content = new HashMap<>();
        content.put("actionLink", actionLink);
        content.put("unsubscribeLink", unsubscribeLink);
        content.put("features", translateList(messagePrefix + ".feature", locale));

        sendReengagementEmail(to, name, messagePrefix, locale, content);
    }

    private List<String> translateList(String keyPrefix, Locale locale) {
        List<String> items = new ArrayList<>();
        for (int index = 1; ; index++) {
            String item = messageSource.getMessage("%s_%d".formatted(keyPrefix, index), null, null, locale);
            if (item == null) return items;
            items.add(item);
        }
    }

    private String buildEmailVerificationLink(String token, Locale locale) {
        return "%s/%s/verify-email?token=%s".formatted(
                frontendUrl,
                locale.getLanguage(),
                URLEncoder.encode(token, StandardCharsets.UTF_8));
    }

    private String buildPasswordResetLink(String token, Locale locale) {
        return "%s/%s/password-recovery/definition?token=%s".formatted(
                frontendUrl,
                locale.getLanguage(),
                URLEncoder.encode(token, StandardCharsets.UTF_8));
    }

    private Map<String, Object> translate(List<String> keys, Locale locale, Object... arguments) {
        Map<String, Object> model = new HashMap<>();

        keys.forEach(key -> {
            String property = key.substring(key.lastIndexOf('.') + 1);
            model.put(property, messageSource.getMessage(key, arguments, locale));
        });

        return model;
    }

    private String render(String template, Map<String, Object> model) {
        try {
            StringWriter writer = new StringWriter();
            freemarkerConfiguration.getTemplate(template).process(model, writer);
            return writer.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to render email template " + template, e);
        }
    }

    private void sendHtmlWithLogo(String to, String subject, String body) {
        sendHtmlWithLogo(to, subject, body, Map.of());
    }

    private void sendHtmlWithLogo(String to, String subject, String body, Map<String, String> headers) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

            helper.setFrom(new InternetAddress(fromSupport));
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);
            helper.addInline(LOGO_CONTENT_ID, new ClassPathResource(LOGO_RESOURCE), "image/png");

            for (Map.Entry<String, String> header : headers.entrySet()) {
                message.setHeader(header.getKey(), header.getValue());
            }

            mailSender.send(message);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to send email to " + to, e);
        }
    }
}
