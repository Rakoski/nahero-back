package br.com.naheroback.modules.auth.useCases.forgotPassword;

import br.com.naheroback.common.services.EmailService;
import br.com.naheroback.modules.user.entities.User;
import br.com.naheroback.modules.user.repositories.UserRepository;
import br.com.naheroback.common.utils.TokenHasher;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "FORGOT_PASSWORD")
public class ForgotPasswordUseCase {

    private static final int TOKEN_BYTES = 32;

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.password-reset.expiration-minutes}")
    private Integer expirationMinutes;

    @Transactional
    public void execute(ForgotPasswordRequest input) {
        userRepository.findByEmail(input.email()).ifPresent(this::issueResetToken);
    }

    private void issueResetToken(User user) {
        String token = generateToken();

        user.setForgotPasswordToken(TokenHasher.hash(token));
        user.setForgotPasswordTokenExpiresAt(LocalDateTime.now().plusMinutes(expirationMinutes));
        userRepository.save(user);

        emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), token);
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
