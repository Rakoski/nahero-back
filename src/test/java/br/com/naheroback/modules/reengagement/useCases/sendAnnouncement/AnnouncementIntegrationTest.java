package br.com.naheroback.modules.reengagement.useCases.sendAnnouncement;

import br.com.naheroback.common.services.EmailService;
import br.com.naheroback.modules.auth.entities.AuthenticatedUser;
import br.com.naheroback.modules.reengagement.entities.enums.AnnouncementCampaign;
import br.com.naheroback.modules.reengagement.entities.enums.ReengagementEmailStatus;
import br.com.naheroback.modules.reengagement.repositories.AnnouncementEmailRepository;
import br.com.naheroback.modules.reengagement.services.AnnouncementSender;
import br.com.naheroback.modules.user.entities.Role;
import br.com.naheroback.modules.user.entities.User;
import br.com.naheroback.modules.user.entities.enums.RolesEnum;
import br.com.naheroback.modules.user.repositories.RoleRepository;
import br.com.naheroback.modules.user.repositories.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@TestPropertySource(properties = {
        "reengagement.enabled=false",
        "payment.reconcile.enabled=false"
})
class AnnouncementIntegrationTest {

    private static final AnnouncementCampaign CAMPAIGN = AnnouncementCampaign.FEEDBACK_LAUNCH;

    @Autowired
    private SendAnnouncementUseCase sendAnnouncement;

    @Autowired
    private AnnouncementSender announcementSender;

    @Autowired
    private AnnouncementEmailRepository announcementEmailRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private EmailService emailService;

    private final List<Integer> userIds = new ArrayList<>();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        for (Integer id : userIds) {
            jdbcTemplate.update("DELETE FROM announcement_emails WHERE user_id = ?", id);
            jdbcTemplate.update("DELETE FROM user_roles WHERE user_id = ?", id);
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", id);
        }
    }

    @Test
    @DisplayName("Should address confirmed students who did not opt out and did not get the announcement yet")
    void shouldSelectTheRightRecipients() {
        Integer confirmed = createUser(RolesEnum.IS_STUDENT, true, false);
        Integer unconfirmed = createUser(RolesEnum.IS_STUDENT, false, false);
        Integer optedOut = createUser(RolesEnum.IS_STUDENT, true, true);
        Integer alreadySent = createUser(RolesEnum.IS_STUDENT, true, false);
        Integer previouslyFailed = createUser(RolesEnum.IS_STUDENT, true, false);
        Integer teacher = createUser(RolesEnum.IS_TEACHER, true, false);
        insertAnnouncement(alreadySent, ReengagementEmailStatus.SENT);
        insertAnnouncement(previouslyFailed, ReengagementEmailStatus.FAILED);

        List<Integer> recipients = announcementEmailRepository.findRecipientIds(CAMPAIGN.getSlug(), ReengagementEmailStatus.SENT);

        assertTrue(recipients.contains(confirmed));
        assertTrue(recipients.contains(previouslyFailed));
        assertFalse(recipients.contains(unconfirmed));
        assertFalse(recipients.contains(optedOut));
        assertFalse(recipients.contains(alreadySent));
        assertFalse(recipients.contains(teacher));
    }

    @Test
    @DisplayName("Should record the send and link to the landing page and to a working unsubscribe token")
    void shouldSendAndRecordTheAnnouncement() {
        Integer userId = createUser(RolesEnum.IS_STUDENT, true, false);

        assertTrue(announcementSender.send(userId, CAMPAIGN));

        String token = userRepository.findById(userId).orElseThrow().getReengagementUnsubscribeToken();
        assertNotNull(token);
        verify(emailService).sendAnnouncementEmail(anyString(), eq("Announcement Student"), eq(CAMPAIGN.messagePrefix()),
                eq(Locale.forLanguageTag("pt")),
                endsWith("/pt/how-it-works?utm_source=email&utm_medium=announcement&utm_campaign=feedback_launch"),
                contains("/reengagement/unsubscribe?token=" + token));
        assertEquals("SENT", statusOf(userId));
        assertFalse(announcementEmailRepository.findRecipientIds(CAMPAIGN.getSlug(), ReengagementEmailStatus.SENT)
                .contains(userId));
    }

    @Test
    @DisplayName("Should record a failure and keep the user as a recipient for the next run")
    void shouldRecordAFailedSend() {
        Integer userId = createUser(RolesEnum.IS_STUDENT, true, false);
        doThrow(new IllegalStateException("mail server unavailable"))
                .when(emailService).sendAnnouncementEmail(anyString(), anyString(), anyString(), any(Locale.class),
                        anyString(), anyString());

        assertFalse(announcementSender.send(userId, CAMPAIGN));

        assertEquals("FAILED", statusOf(userId));
        assertTrue(announcementEmailRepository.findRecipientIds(CAMPAIGN.getSlug(), ReengagementEmailStatus.SENT)
                .contains(userId));
    }

    @Test
    @DisplayName("Should only count recipients on a dry run, without sending anything")
    void shouldOnlyCountOnADryRun() {
        createUser(RolesEnum.IS_STUDENT, true, false);
        authenticateAs(createUser(RolesEnum.IS_ADMIN, true, false));

        SendAnnouncementResponse response = sendAnnouncement.execute(CAMPAIGN, true);

        assertTrue(response.dryRun());
        assertTrue(response.recipients() >= 1);
        verify(emailService, after(500).never()).sendAnnouncementEmail(anyString(), anyString(), anyString(),
                any(Locale.class), anyString(), anyString());
    }

    @Test
    @DisplayName("Should refuse the announcement to anyone who is not an admin")
    void shouldRefuseNonAdmins() {
        authenticateAs(createUser(RolesEnum.IS_STUDENT, true, false));

        assertThrows(AccessDeniedException.class, () -> sendAnnouncement.execute(CAMPAIGN, true));
    }

    private Integer createUser(RolesEnum roleName, boolean confirmed, boolean optedOut) {
        Role role = roleRepository.findByName(roleName.name()).orElseThrow();
        User user = new User();
        user.setName("Announcement Student");
        user.setEmail("announcement-%s@example.com".formatted(System.nanoTime()));
        user.setPassword("irrelevant");
        user.setRoles(new HashSet<>(Set.of(role)));
        if (confirmed) user.setEmailConfirmedAt(LocalDateTime.now().minusDays(1));
        if (optedOut) user.setReengagementOptedOutAt(LocalDateTime.now().minusDays(1));
        Integer id = userRepository.save(user).getId();
        userIds.add(id);
        return id;
    }

    private void authenticateAs(Integer userId) {
        AuthenticatedUser principal = new AuthenticatedUser(userRepository.findById(userId).orElseThrow());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private void insertAnnouncement(Integer userId, ReengagementEmailStatus status) {
        jdbcTemplate.update("""
                INSERT INTO announcement_emails (user_id, campaign, status, sent_at) VALUES (?, ?, ?, ?)
                """, userId, CAMPAIGN.getSlug(), status.name(), LocalDateTime.now().minusHours(1));
    }

    private String statusOf(Integer userId) {
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT status FROM announcement_emails WHERE user_id = ? AND campaign = ?", userId, CAMPAIGN.getSlug());
        return (String) row.get("status");
    }
}
