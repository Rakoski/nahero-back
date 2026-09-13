package br.com.naheroback.modules.reengagement.useCases.dispatchReengagementEmails;

import br.com.naheroback.common.services.EmailService;
import br.com.naheroback.modules.practiceExams.repositories.StudentPracticeAttemptRepository;
import br.com.naheroback.modules.reengagement.entities.ReengagementEmail;
import br.com.naheroback.modules.reengagement.entities.enums.ReengagementEmailType;
import br.com.naheroback.modules.reengagement.repositories.ReengagementCandidate;
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
    private StudentPracticeAttemptRepository studentPracticeAttemptRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private DispatchReengagementEmailsUseCase dispatchReengagementEmailsUseCase;

    private User mockUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(dispatchReengagementEmailsUseCase, "frontendUrl", "https://nahero.site");
        ReflectionTestUtils.setField(dispatchReengagementEmailsUseCase, "apiUrl", "https://nahero.site/api/v1");
        ReflectionTestUtils.setField(dispatchReengagementEmailsUseCase, "scheduledEnabled", true);
        ReflectionTestUtils.setField(dispatchReengagementEmailsUseCase, "batchSize", 200);
        ReflectionTestUtils.setField(dispatchReengagementEmailsUseCase, "campaignMonths", 3);
        ReflectionTestUtils.setField(dispatchReengagementEmailsUseCase, "minDaysBetweenEmails", 7);
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
                eq("https://nahero.site/pt/practice-exams"),
                eq(Locale.forLanguageTag("pt")));
    }

    @Test
    @DisplayName("Should not send anything while the next step is not due yet")
    void shouldNotSendWhileNextStepIsNotDue() {
        LocalDateTime lastActivity = LocalDateTime.now().minusDays(20);
        givenCandidates(candidate(lastActivity));
        when(reengagementEmailRepository.findByUserIdInAndSentAtAfter(anyCollection(), any(LocalDateTime.class)))
                .thenReturn(List.of(
                        sentEmail(ReengagementEmailType.WE_MISS_YOU, lastActivity.plusDays(7))));

        DispatchReengagementEmailsResponse response = dispatchReengagementEmailsUseCase.execute();
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("Should restart the sequence when the emails belong to a campaign before the last activity")
    void shouldRestartSequenceForANewCampaign() {
        LocalDateTime lastActivity = LocalDateTime.now().minusDays(10);
        givenCandidates(candidate(lastActivity));
        when(reengagementEmailRepository.findByUserIdInAndSentAtAfter(anyCollection(), any(LocalDateTime.class)))
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

    private void givenCandidates(ReengagementCandidate... candidates) {
        when(reengagementUserRepository.findCampaignCandidates(any(LocalDateTime.class), any(LocalDateTime.class),
                any(LocalDateTime.class), any(Pageable.class))).thenReturn(List.of(candidates));
    }

    private void givenNoHistory() {
        when(reengagementEmailRepository.findByUserIdInAndSentAtAfter(anyCollection(), any(LocalDateTime.class)))
                .thenReturn(List.of());
    }

    private void givenUserIsLoadable() {
        when(userRepository.findById(1)).thenReturn(Optional.of(mockUser));
        when(studentPracticeAttemptRepository.findRecentLanguagesForStudent(eq(1), any(Pageable.class)))
                .thenReturn(List.of());
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

    private ReengagementEmail sentEmail(ReengagementEmailType type, LocalDateTime sentAt) {
        ReengagementEmail email = new ReengagementEmail();
        email.setUser(mockUser);
        email.setEmailType(type);
        email.setSentAt(sentAt);
        email.setCampaignStartedAt(sentAt);
        return email;
    }
}
