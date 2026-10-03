package br.com.naheroback.modules.reengagement.useCases.dispatchReengagementEmails;

import br.com.naheroback.common.services.EmailService;
import br.com.naheroback.modules.practiceExams.entities.PracticeAttemptStatus;
import br.com.naheroback.modules.practiceExams.entities.PracticeExam;
import br.com.naheroback.modules.practiceExams.entities.StudentPracticeAttempt;
import br.com.naheroback.modules.practiceExams.entities.enums.PracticeAttemptStatusesEnum;
import br.com.naheroback.modules.practiceExams.repositories.StudentPracticeAttemptRepository;
import br.com.naheroback.modules.practiceExams.services.AttemptDomainBreakdownService;
import br.com.naheroback.modules.practiceExams.services.AttemptDomainBreakdownService.DomainScore;
import br.com.naheroback.modules.reengagement.entities.ReengagementDispatchRun;
import br.com.naheroback.modules.reengagement.entities.ReengagementEmail;
import br.com.naheroback.modules.reengagement.entities.enums.ReengagementEmailStatus;
import br.com.naheroback.modules.reengagement.entities.enums.ReengagementEmailType;
import br.com.naheroback.modules.reengagement.repositories.ReengagementCandidate;
import br.com.naheroback.modules.reengagement.repositories.ReengagementDispatchRunRepository;
import br.com.naheroback.modules.reengagement.repositories.ReengagementEmailHistory;
import br.com.naheroback.modules.reengagement.repositories.ReengagementEmailRepository;
import br.com.naheroback.modules.reengagement.repositories.ReengagementUserRepository;
import br.com.naheroback.modules.user.entities.User;
import br.com.naheroback.modules.user.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DispatchReengagementEmailsUseCaseTest {

    @Mock
    private ReengagementUserRepository reengagementUserRepository;

    @Mock
    private ReengagementEmailRepository reengagementEmailRepository;

    @Mock
    private ReengagementDispatchRunRepository reengagementDispatchRunRepository;

    @Mock
    private StudentPracticeAttemptRepository studentPracticeAttemptRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private AttemptDomainBreakdownService attemptDomainBreakdownService;

    @InjectMocks
    private DispatchReengagementEmailsUseCase dispatchReengagementEmailsUseCase;

    private User mockUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(dispatchReengagementEmailsUseCase, "frontendUrl", "https://nahero.site");
        ReflectionTestUtils.setField(dispatchReengagementEmailsUseCase, "apiUrl", "https://nahero.site/api/v1");
        ReflectionTestUtils.setField(dispatchReengagementEmailsUseCase, "scheduledEnabled", true);
        ReflectionTestUtils.setField(dispatchReengagementEmailsUseCase, "batchSize", 200);
        ReflectionTestUtils.setField(dispatchReengagementEmailsUseCase, "campaignMonths", 24);
        ReflectionTestUtils.setField(dispatchReengagementEmailsUseCase, "minDaysBetweenEmails", 7);
        ReflectionTestUtils.setField(dispatchReengagementEmailsUseCase, "maxAttemptsPerStep", 3);
        ReflectionTestUtils.setField(dispatchReengagementEmailsUseCase, "defaultLanguage", "pt");

        mockUser = new User();
        mockUser.setId(1);
        mockUser.setName("Test User");
        mockUser.setEmail("test@example.com");
    }

    @Test
    @DisplayName("Should send the first step once the user has been inactive for a week")
    void shouldSendFirstStepAfterOneWeekOfInactivity() {
        LocalDateTime lastActivity = LocalDateTime.now().minusDays(8);
        givenCandidates(candidate(lastActivity));
        givenNoHistory();
        givenUserIsLoadable();

        DispatchReengagementEmailsResponse response = dispatchReengagementEmailsUseCase.execute();

        assertEquals(1, response.candidates());

        verify(emailService).sendReengagementEmail(
                eq("test@example.com"),
                eq("Test User"),
                eq(ReengagementEmailType.WE_MISS_YOU.messagePrefix()),
                eq("https://nahero.site/pt/practice-exams?utm_source=email&utm_medium=reengagement&utm_campaign=we_miss_you"),
                eq(Locale.forLanguageTag("pt")));
    }

    @Test
    @DisplayName("Should not send anything while the next step is not due yet")
    void shouldNotSendWhileNextStepIsNotDue() {
        LocalDateTime lastActivity = LocalDateTime.now().minusDays(8);
        givenCandidates(candidate(lastActivity));
        when(reengagementEmailRepository.findHistoryForUsers(anyCollection(), any(LocalDateTime.class)))
                .thenReturn(List.of(
                        sentEmail(ReengagementEmailType.WE_MISS_YOU, lastActivity.plusDays(7))));

        DispatchReengagementEmailsResponse response = dispatchReengagementEmailsUseCase.execute();

        assertEquals(1, response.skipped());
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("Should advance to the next step once its delay has elapsed")
    void shouldAdvanceToTheNextStep() {
        LocalDateTime lastActivity = LocalDateTime.now().minusDays(15);
        givenCandidates(candidate(lastActivity));
        when(reengagementEmailRepository.findHistoryForUsers(anyCollection(), any(LocalDateTime.class)))
                .thenReturn(List.of(sentEmail(ReengagementEmailType.WE_MISS_YOU, lastActivity.plusDays(7))));
        givenUserIsLoadable();

        dispatchReengagementEmailsUseCase.execute();

        verify(emailService).sendReengagementEmail(anyString(), anyString(),
                eq(ReengagementEmailType.STREAK_BROKEN.messagePrefix()),
                eq("https://nahero.site/pt/student/dashboard?utm_source=email&utm_medium=reengagement&utm_campaign=streak_broken"), any(Locale.class));
    }

    @Test
    @DisplayName("Should stop after the last step of the sequence has been delivered")
    void shouldStopAfterTheLastStep() {
        LocalDateTime lastActivity = LocalDateTime.now().minusDays(400);
        givenCandidates(candidate(lastActivity));
        when(reengagementEmailRepository.findHistoryForUsers(anyCollection(), any(LocalDateTime.class)))
                .thenReturn(ReengagementEmailType.sequence().stream()
                        .map(type -> sentEmail(type, lastActivity.plusDays(200)))
                        .toList());

        DispatchReengagementEmailsResponse response = dispatchReengagementEmailsUseCase.execute();

        assertEquals(1, response.skipped());
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("Should walk a long-dormant user through the sequence one step at a time")
    void shouldWalkALongDormantUserThroughTheSequence() {
        LocalDateTime lastActivity = LocalDateTime.now().minusDays(400);
        givenCandidates(candidate(lastActivity));
        when(reengagementEmailRepository.findHistoryForUsers(anyCollection(), any(LocalDateTime.class)))
                .thenReturn(List.of(
                        sentEmail(ReengagementEmailType.WE_MISS_YOU, lastActivity.plusDays(200)),
                        sentEmail(ReengagementEmailType.STREAK_BROKEN, lastActivity.plusDays(207))));
        givenUserIsLoadable();

        dispatchReengagementEmailsUseCase.execute();

        verify(emailService).sendReengagementEmail(anyString(), anyString(),
                eq(ReengagementEmailType.NEW_CONTENT.messagePrefix()), anyString(), any(Locale.class));
    }

    @Test
    @DisplayName("Should restart the sequence when the emails belong to a campaign before the last activity")
    void shouldRestartSequenceForANewCampaign() {
        LocalDateTime lastActivity = LocalDateTime.now().minusDays(10);
        givenCandidates(candidate(lastActivity));
        when(reengagementEmailRepository.findHistoryForUsers(anyCollection(), any(LocalDateTime.class)))
                .thenReturn(List.of(sentEmail(ReengagementEmailType.WE_MISS_YOU, lastActivity.minusDays(5))));
        givenUserIsLoadable();

        dispatchReengagementEmailsUseCase.execute();

        verify(emailService).sendReengagementEmail(anyString(), anyString(),
                eq(ReengagementEmailType.WE_MISS_YOU.messagePrefix()), anyString(), any(Locale.class));
    }

    @Test
    @DisplayName("Should record the dispatch before handing the email over to the mail server")
    void shouldRecordDispatchBeforeSending() {
        LocalDateTime lastActivity = LocalDateTime.now().minusDays(8);
        givenCandidates(candidate(lastActivity));
        givenNoHistory();
        givenUserIsLoadable();

        dispatchReengagementEmailsUseCase.execute();

        InOrder inOrder = inOrder(reengagementEmailRepository, emailService);
        inOrder.verify(reengagementEmailRepository).save(any(ReengagementEmail.class));
        inOrder.verify(emailService).sendReengagementEmail(anyString(), anyString(), anyString(), anyString(), any(Locale.class));

        ArgumentCaptor<ReengagementEmail> captor = ArgumentCaptor.forClass(ReengagementEmail.class);
        verify(reengagementEmailRepository).save(captor.capture());

        ReengagementEmail dispatched = captor.getValue();
        assertEquals(ReengagementEmailType.WE_MISS_YOU, dispatched.getEmailType());
        assertEquals(lastActivity, dispatched.getCampaignStartedAt());
        assertEquals(mockUser, dispatched.getUser());
    }

    @Test
    @DisplayName("Should keep the unsubscribe token that the user already has")
    void shouldReuseTheExistingUnsubscribeToken() {
        mockUser.setReengagementUnsubscribeToken("existing-token");

        LocalDateTime lastActivity = LocalDateTime.now().minusDays(8);
        givenCandidates(candidate(lastActivity));
        givenNoHistory();
        givenUserIsLoadable();

        dispatchReengagementEmailsUseCase.execute();

        verify(userRepository, never()).save(any(User.class));
        verify(emailService).sendReengagementEmail(anyString(), anyString(), anyString(), anyString(), any(Locale.class));
    }

    @Test
    @DisplayName("Should not scan for candidates when the scheduled dispatch is disabled")
    void shouldNotScanWhenDisabled() {
        ReflectionTestUtils.setField(dispatchReengagementEmailsUseCase, "scheduledEnabled", false);

        dispatchReengagementEmailsUseCase.runScheduled();

        verifyNoInteractions(reengagementUserRepository);
    }

    @Test
    @DisplayName("Should send in Portuguese with a /pt link when the last attempt was in Portuguese")
    void shouldSendInPortugueseWhenTheLastAttemptWasInPortuguese() {
        givenCandidates(candidate(LocalDateTime.now().minusDays(8)));
        givenNoHistory();
        givenUserIsLoadableWithLanguages("pt");

        dispatchReengagementEmailsUseCase.execute();

        verify(emailService).sendReengagementEmail(anyString(), anyString(), anyString(),
                eq("https://nahero.site/pt/practice-exams?utm_source=email&utm_medium=reengagement&utm_campaign=we_miss_you"), eq(Locale.forLanguageTag("pt")));
    }

    @Test
    @DisplayName("Should send in English with an /en link when the last attempt was in English")
    void shouldSendInEnglishWhenTheLastAttemptWasInEnglish() {
        givenCandidates(candidate(LocalDateTime.now().minusDays(8)));
        givenNoHistory();
        givenUserIsLoadableWithLanguages("en");

        dispatchReengagementEmailsUseCase.execute();

        verify(emailService).sendReengagementEmail(anyString(), anyString(), anyString(),
                eq("https://nahero.site/en/practice-exams?utm_source=email&utm_medium=reengagement&utm_campaign=we_miss_you"), eq(Locale.forLanguageTag("en")));
    }

    @Test
    @DisplayName("Should fall back to the most recent supported language of the attempts")
    void shouldSkipUnsupportedAttemptLanguages() {
        givenCandidates(candidate(LocalDateTime.now().minusDays(8)));
        givenNoHistory();
        givenUserIsLoadableWithLanguages("es", "en");

        dispatchReengagementEmailsUseCase.execute();

        verify(emailService).sendReengagementEmail(anyString(), anyString(), anyString(),
                eq("https://nahero.site/en/practice-exams?utm_source=email&utm_medium=reengagement&utm_campaign=we_miss_you"), eq(Locale.forLanguageTag("en")));
    }

    @Test
    @DisplayName("Should normalize regional tags coming from the attempts")
    void shouldNormalizeRegionalAttemptLanguages() {
        givenCandidates(candidate(LocalDateTime.now().minusDays(8)));
        givenNoHistory();
        givenUserIsLoadableWithLanguages("pt-BR");

        dispatchReengagementEmailsUseCase.execute();

        verify(emailService).sendReengagementEmail(anyString(), anyString(), anyString(),
                eq("https://nahero.site/pt/practice-exams?utm_source=email&utm_medium=reengagement&utm_campaign=we_miss_you"), eq(Locale.forLanguageTag("pt")));
    }

    @Test
    @DisplayName("Should use the default language when the user has no attempt language")
    void shouldUseDefaultLanguageWithoutAttempts() {
        givenCandidates(candidate(LocalDateTime.now().minusDays(8)));
        givenNoHistory();
        givenUserIsLoadableWithLanguages();

        dispatchReengagementEmailsUseCase.execute();

        verify(emailService).sendReengagementEmail(anyString(), anyString(), anyString(),
                eq("https://nahero.site/pt/practice-exams?utm_source=email&utm_medium=reengagement&utm_campaign=we_miss_you"), eq(Locale.forLanguageTag("pt")));
    }

    @Test
    @DisplayName("Should mark the dispatch record as failed when the mail server rejects the message")
    void shouldMarkTheRecordAsFailedWhenTheSendFails() {
        givenCandidates(candidate(LocalDateTime.now().minusDays(8)));
        givenNoHistory();
        givenUserIsLoadable();

        doThrow(new IllegalStateException("mail server unavailable"))
                .when(emailService).sendReengagementEmail(anyString(), anyString(), anyString(), anyString(), any(Locale.class));

        DispatchReengagementEmailsResponse response = dispatchReengagementEmailsUseCase.execute();

        assertEquals(0, response.sent());
        assertEquals(1, response.failed());

        ArgumentCaptor<ReengagementEmail> captor = ArgumentCaptor.forClass(ReengagementEmail.class);
        verify(reengagementEmailRepository, times(2)).save(captor.capture());

        ReengagementEmail recorded = captor.getValue();
        assertEquals(ReengagementEmailStatus.FAILED, recorded.getStatus());
        assertTrue(recorded.getFailureReason().contains("mail server unavailable"));
        verify(reengagementEmailRepository, never()).delete(any(ReengagementEmail.class));
    }

    @Test
    @DisplayName("Should stop retrying a step once it has burned through its attempts")
    void shouldStopRetryingAfterTheAttemptLimit() {
        LocalDateTime lastActivity = LocalDateTime.now().minusDays(8);
        givenCandidates(candidate(lastActivity));
        when(reengagementEmailRepository.findHistoryForUsers(anyCollection(), any(LocalDateTime.class)))
                .thenReturn(List.of(
                        failedEmail(ReengagementEmailType.WE_MISS_YOU, lastActivity.plusDays(7)),
                        failedEmail(ReengagementEmailType.WE_MISS_YOU, lastActivity.plusDays(7).plusHours(1)),
                        failedEmail(ReengagementEmailType.WE_MISS_YOU, lastActivity.plusDays(7).plusHours(2))));

        DispatchReengagementEmailsResponse response = dispatchReengagementEmailsUseCase.execute();

        assertEquals(1, response.skipped());
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("Should retry a step that has failed fewer times than the attempt limit")
    void shouldRetryBelowTheAttemptLimit() {
        LocalDateTime lastActivity = LocalDateTime.now().minusDays(8);
        givenCandidates(candidate(lastActivity));
        when(reengagementEmailRepository.findHistoryForUsers(anyCollection(), any(LocalDateTime.class)))
                .thenReturn(List.of(failedEmail(ReengagementEmailType.WE_MISS_YOU, lastActivity.plusDays(7))));
        givenUserIsLoadable();

        DispatchReengagementEmailsResponse response = dispatchReengagementEmailsUseCase.execute();

        assertEquals(1, response.sent());
        verify(emailService).sendReengagementEmail(anyString(), anyString(), anyString(), anyString(), any(Locale.class));
    }

    @Test
    @DisplayName("Should record the run with its counters and eligibility funnel")
    void shouldRecordTheRun() {
        givenCandidates(candidate(LocalDateTime.now().minusDays(8)));
        givenNoHistory();
        givenUserIsLoadable();
        when(reengagementUserRepository.findEligibilityFunnel(any(LocalDateTime.class), any(LocalDateTime.class),
                any(LocalDateTime.class))).thenReturn("{\"eligible\": 1}");

        dispatchReengagementEmailsUseCase.execute();

        ArgumentCaptor<ReengagementDispatchRun> captor = ArgumentCaptor.forClass(ReengagementDispatchRun.class);
        verify(reengagementDispatchRunRepository).save(captor.capture());

        ReengagementDispatchRun run = captor.getValue();
        assertEquals(1, run.getCandidates());
        assertEquals(1, run.getSent());
        assertEquals(0, run.getFailed());
        assertEquals(0, run.getSkipped());
        assertEquals("{\"eligible\": 1}", run.getFunnel());
        assertNotNull(run.getStartedAt());
        assertNotNull(run.getFinishedAt());
    }

    @Test
    @DisplayName("Should send the result follow-up with the attempt's numbers three days after a completed attempt")
    void shouldSendTheResultFollowupForARecentCompletedAttempt() {
        LocalDateTime startedAt = LocalDateTime.now().minusDays(4);
        StudentPracticeAttempt attempt = attempt(PracticeAttemptStatusesEnum.COMPLETED, startedAt, 40, "en");
        givenCandidates(candidate(startedAt));
        givenNoHistory();
        givenLatestAttempt(attempt);
        when(attemptDomainBreakdownService.breakdown(attempt)).thenReturn(List.of(
                new DomainScore("Security and Compliance", 3, 12),
                new DomainScore("Cloud Concepts", 14, 16)));
        when(userRepository.findById(1)).thenReturn(Optional.of(mockUser));

        DispatchReengagementEmailsResponse response = dispatchReengagementEmailsUseCase.execute();

        assertEquals(1, response.sent());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> model = ArgumentCaptor.forClass(Map.class);
        verify(emailService).sendReengagementEmail(
                eq("test@example.com"),
                eq("Test User"),
                eq(ReengagementEmailType.RESULT_FOLLOWUP.messagePrefix()),
                eq(Locale.forLanguageTag("en")),
                model.capture());
        verify(emailService, never()).sendReengagementEmail(anyString(), anyString(), anyString(), anyString(), any(Locale.class));
        verify(studentPracticeAttemptRepository, never()).findRecentLanguagesForStudent(anyInt(), any(Pageable.class));

        assertEquals("AWS Cloud Practitioner", model.getValue().get("examTitle"));
        assertEquals(40, model.getValue().get("score"));
        assertEquals(65, model.getValue().get("total"));
        assertEquals("Security and Compliance", model.getValue().get("weakestDomain"));
        assertEquals(3, model.getValue().get("weakestCorrect"));
        assertEquals(12, model.getValue().get("weakestTotal"));
        assertEquals("https://nahero.site/en/practice-exams/aws-cloud-practitioner-clf-02"
                        + "?utm_source=email&utm_medium=reengagement&utm_campaign=result_followup",
                model.getValue().get("actionLink"));

        ArgumentCaptor<ReengagementEmail> recorded = ArgumentCaptor.forClass(ReengagementEmail.class);
        verify(reengagementEmailRepository).save(recorded.capture());
        assertEquals(ReengagementEmailType.RESULT_FOLLOWUP, recorded.getValue().getEmailType());
    }

    @Test
    @DisplayName("Should skip the result follow-up for a signup-only user without sending anything early")
    void shouldSkipTheResultFollowupForASignupOnlyUser() {
        givenCandidates(candidate(LocalDateTime.now().minusDays(4)));
        givenNoHistory();
        when(studentPracticeAttemptRepository.findLatestForStudent(eq(1), any(Pageable.class))).thenReturn(List.of());

        DispatchReengagementEmailsResponse response = dispatchReengagementEmailsUseCase.execute();

        assertEquals(1, response.skipped());
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("Should not let the skipped result follow-up block the next step for a signup-only user")
    void shouldNotBlockTheSequenceForASignupOnlyUser() {
        givenCandidates(candidate(LocalDateTime.now().minusDays(8)));
        givenNoHistory();
        givenUserIsLoadable();

        dispatchReengagementEmailsUseCase.execute();

        verify(emailService).sendReengagementEmail(anyString(), anyString(),
                eq(ReengagementEmailType.WE_MISS_YOU.messagePrefix()), anyString(), any(Locale.class));
    }

    @Test
    @DisplayName("Should skip the result follow-up and continue the sequence when the attempt is older than seven days")
    void shouldSkipTheResultFollowupForAnAttemptOlderThanSevenDays() {
        LocalDateTime startedAt = LocalDateTime.now().minusDays(8);
        givenCandidates(candidate(startedAt));
        givenNoHistory();
        lenient().when(studentPracticeAttemptRepository.findLatestForStudent(eq(1), any(Pageable.class)))
                .thenReturn(List.of(attempt(PracticeAttemptStatusesEnum.COMPLETED, startedAt, 40, "en")));
        givenUserIsLoadable();

        DispatchReengagementEmailsResponse response = dispatchReengagementEmailsUseCase.execute();

        assertEquals(1, response.sent());
        verify(emailService).sendReengagementEmail(anyString(), anyString(),
                eq(ReengagementEmailType.WE_MISS_YOU.messagePrefix()), anyString(), any(Locale.class));
        verify(emailService, never()).sendReengagementEmail(anyString(), anyString(), anyString(), any(Locale.class), anyMap());
    }

    @Test
    @DisplayName("Should skip the result follow-up when the latest attempt was not completed")
    void shouldSkipTheResultFollowupWhenTheLatestAttemptWasNotCompleted() {
        LocalDateTime startedAt = LocalDateTime.now().minusDays(4);
        givenCandidates(candidate(startedAt));
        givenNoHistory();
        givenLatestAttempt(attempt(PracticeAttemptStatusesEnum.ABANDONED, startedAt, null, "en"));

        DispatchReengagementEmailsResponse response = dispatchReengagementEmailsUseCase.execute();

        assertEquals(1, response.skipped());
        verifyNoInteractions(emailService, attemptDomainBreakdownService);
    }

    @Test
    @DisplayName("Should move on to the next step once the result follow-up has been delivered")
    void shouldMoveOnAfterTheResultFollowup() {
        LocalDateTime startedAt = LocalDateTime.now().minusDays(8);
        givenCandidates(candidate(startedAt));
        when(reengagementEmailRepository.findHistoryForUsers(anyCollection(), any(LocalDateTime.class)))
                .thenReturn(List.of(sentEmail(ReengagementEmailType.RESULT_FOLLOWUP, startedAt.plusDays(3))));
        givenUserIsLoadable();

        dispatchReengagementEmailsUseCase.execute();

        verify(emailService).sendReengagementEmail(anyString(), anyString(),
                eq(ReengagementEmailType.WE_MISS_YOU.messagePrefix()), anyString(), any(Locale.class));
    }

    private void givenCandidates(ReengagementCandidate... candidates) {
        when(reengagementUserRepository.findCampaignCandidates(any(LocalDateTime.class), any(LocalDateTime.class),
                any(LocalDateTime.class), eq(ReengagementEmailStatus.SENT), any(Pageable.class)))
                .thenReturn(List.of(candidates));
    }

    private void givenNoHistory() {
        when(reengagementEmailRepository.findHistoryForUsers(anyCollection(), any(LocalDateTime.class)))
                .thenReturn(List.of());
    }

    private void givenUserIsLoadable() {
        givenUserIsLoadableWithLanguages();
    }

    private void givenUserIsLoadableWithLanguages(String... languages) {
        when(userRepository.findById(1)).thenReturn(Optional.of(mockUser));
        when(studentPracticeAttemptRepository.findRecentLanguagesForStudent(eq(1), any(Pageable.class)))
                .thenReturn(List.of(languages));
    }

    private void givenLatestAttempt(StudentPracticeAttempt attempt) {
        when(studentPracticeAttemptRepository.findLatestForStudent(eq(1), any(Pageable.class)))
                .thenReturn(List.of(attempt));
    }

    private StudentPracticeAttempt attempt(PracticeAttemptStatusesEnum status, LocalDateTime startedAt,
                                           Integer score, String language) {
        PracticeExam practiceExam = new PracticeExam();
        practiceExam.setTitle("AWS Cloud Practitioner");
        practiceExam.setSlug("aws-cloud-practitioner-clf-02");
        practiceExam.setNumberOfQuestions(65);

        PracticeAttemptStatus attemptStatus = new PracticeAttemptStatus();
        attemptStatus.setId(status.getId());

        StudentPracticeAttempt attempt = new StudentPracticeAttempt();
        attempt.setId(99);
        attempt.setPracticeExam(practiceExam);
        attempt.setAttemptStatus(attemptStatus);
        attempt.setStartTime(startedAt);
        attempt.setScore(score);
        attempt.setLanguage(language);
        return attempt;
    }

    private ReengagementCandidate candidate(LocalDateTime lastActivity) {
        return new CandidateProjection(1, "Test User", "test@example.com", lastActivity);
    }

    private record CandidateProjection(Integer userId, String name, String email, LocalDateTime lastActivityAt)
            implements ReengagementCandidate {

        @Override
        public Integer getUserId() {
            return userId;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getEmail() {
            return email;
        }

        @Override
        public LocalDateTime getLastActivityAt() {
            return lastActivityAt;
        }
    }

    private ReengagementEmailHistory sentEmail(ReengagementEmailType type, LocalDateTime sentAt) {
        return new HistoryProjection(1, type, ReengagementEmailStatus.SENT, sentAt);
    }

    private ReengagementEmailHistory failedEmail(ReengagementEmailType type, LocalDateTime sentAt) {
        return new HistoryProjection(1, type, ReengagementEmailStatus.FAILED, sentAt);
    }

    private record HistoryProjection(Integer userId, ReengagementEmailType emailType,
                                     ReengagementEmailStatus status, LocalDateTime sentAt)
            implements ReengagementEmailHistory {

        @Override
        public Integer getUserId() {
            return userId;
        }

        @Override
        public ReengagementEmailType getEmailType() {
            return emailType;
        }

        @Override
        public ReengagementEmailStatus getStatus() {
            return status;
        }

        @Override
        public LocalDateTime getSentAt() {
            return sentAt;
        }
    }
}
