package br.com.naheroback.modules.user.useCases.user.resendEmailVerification;

import br.com.naheroback.modules.user.entities.User;
import br.com.naheroback.modules.user.repositories.UserRepository;
import br.com.naheroback.modules.user.services.EmailVerificationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResendEmailVerificationUseCase {

    private final UserRepository userRepository;
    private final EmailVerificationService emailVerificationService;

    @Transactional
    public void execute(ResendEmailVerificationRequest input) {
        userRepository.findByEmail(input.email())
                .filter(user -> !user.isEmailConfirmed())
                .ifPresent(this::resend);
    }

    private void resend(User user) {
        emailVerificationService.issueAndSend(user);
    }
}
