package br.com.naheroback.modules.auth.useCases.forgotPassword;

import br.com.naheroback.modules.emailOutbox.entities.enums.EmailOutboxType;
import br.com.naheroback.modules.emailOutbox.services.EmailOutboxService;
import br.com.naheroback.modules.user.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "FORGOT_PASSWORD")
public class ForgotPasswordUseCase {

    private final UserRepository userRepository;
    private final EmailOutboxService emailOutboxService;

    @Transactional
    public void execute(ForgotPasswordRequest input) {
        userRepository.findByEmail(input.email())
                .ifPresent(user -> emailOutboxService.enqueue(user, EmailOutboxType.PASSWORD_RESET));
    }
}
