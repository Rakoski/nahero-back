package br.com.naheroback.modules.auth.useCases.forgotPassword;

import br.com.naheroback.common.services.EmailService;
import br.com.naheroback.modules.user.entities.User;
import br.com.naheroback.modules.user.repositories.UserRepository;
import br.com.naheroback.common.utils.SecureTokenGenerator;
import br.com.naheroback.common.utils.TokenHasher;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "FORGOT_PASSWORD")
public class ForgotPasswordUseCase {

    private final UserRepository userRepository;
    private final EmailService emailService;

    @Value("${app.password-reset.expiration-minutes}")
    private Integer expirationMinutes;

    @Transactional
    public void execute(ForgotPasswordRequest input) {
        userRepository.findByEmail(input.email()).ifPresent(this::issueResetToken);
    }

    private void issueResetToken(User user) {
        String token = SecureTokenGenerator.generate();

        user.setForgotPasswordToken(TokenHasher.hash(token));
        user.setForgotPasswordTokenExpiresAt(LocalDateTime.now().plusMinutes(expirationMinutes));
        userRepository.save(user);

        emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), token);
    }
}
