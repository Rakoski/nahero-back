package br.com.naheroback.modules.user.useCases.user.verifyEmail;

import br.com.naheroback.common.exceptions.custom.UnprocessableEntityException;
import br.com.naheroback.common.utils.TokenHasher;
import br.com.naheroback.modules.user.entities.User;
import br.com.naheroback.modules.user.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VerifyEmailUseCase {

    private static final String INVALID_TOKEN = "auth.email_verification.invalid_token";

    private final UserRepository userRepository;

    @Transactional
    public void execute(VerifyEmailRequest input) {
        User user = userRepository.findByEmailVerificationToken(TokenHasher.hash(input.verificationToken()))
                .orElseThrow(() -> new UnprocessableEntityException(INVALID_TOKEN));

        if (isTokenExpired(user)) throw new UnprocessableEntityException(INVALID_TOKEN);

        if (user.isEmailConfirmed()) return;

        user.setEmailConfirmedAt(LocalDateTime.now());

        userRepository.save(user);
    }

    private boolean isTokenExpired(User user) {
        return user.getEmailVerificationTokenExpiresAt() == null || user.getEmailVerificationTokenExpiresAt().isBefore(LocalDateTime.now());
    }
}
