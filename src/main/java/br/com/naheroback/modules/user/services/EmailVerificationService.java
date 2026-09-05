package br.com.naheroback.modules.user.services;

import br.com.naheroback.common.services.EmailService;
import br.com.naheroback.common.utils.SecureTokenGenerator;
import br.com.naheroback.common.utils.TokenHasher;
import br.com.naheroback.modules.user.entities.User;
import br.com.naheroback.modules.user.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "EMAIL_VERIFICATION")
public class EmailVerificationService {

    private final UserRepository userRepository;
    private final EmailService emailService;

    @Value("${app.email-verification.expiration-hours}")
    private Integer expirationHours;

    public void issueAndSend(User user) {
        String token = SecureTokenGenerator.generate();

        user.setEmailVerificationToken(TokenHasher.hash(token));
        user.setEmailVerificationTokenExpiresAt(LocalDateTime.now().plusHours(expirationHours));
        userRepository.save(user);

        emailService.sendEmailVerificationEmail(user.getEmail(), user.getName(), token);
    }

    public void issueAndSendQuietly(User user) {
        try {
            issueAndSend(user);
        } catch (RuntimeException e) {
            log.error("Could not send the verification email to user {}", user.getId(), e);
        }
    }
}
