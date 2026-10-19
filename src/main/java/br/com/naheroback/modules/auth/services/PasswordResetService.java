package br.com.naheroback.modules.auth.services;

import br.com.naheroback.common.services.EmailService;
import br.com.naheroback.common.utils.SecureTokenGenerator;
import br.com.naheroback.common.utils.TokenHasher;
import br.com.naheroback.modules.user.entities.User;
import br.com.naheroback.modules.user.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final EmailService emailService;

    @Value("${app.password-reset.expiration-minutes}")
    private Integer expirationMinutes;

    public void issueAndSend(User user, Locale locale) {
        String token = SecureTokenGenerator.generate();

        emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), token, locale);

        user.setForgotPasswordToken(TokenHasher.hash(token));
        user.setForgotPasswordTokenExpiresAt(LocalDateTime.now().plusMinutes(expirationMinutes));
        userRepository.save(user);
    }
}
