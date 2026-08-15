package br.com.naheroback.modules.auth.useCases.resetPassword;

import br.com.naheroback.common.exceptions.custom.UnauthorizedException;
import br.com.naheroback.common.exceptions.custom.UnprocessableEntityException;
import br.com.naheroback.common.utils.TokenHasher;
import br.com.naheroback.modules.user.entities.User;
import br.com.naheroback.modules.user.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ResetPasswordUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void execute(ResetPasswordRequest input) {
        if (!input.password().equals(input.confirmPassword())) throw new UnprocessableEntityException("auth.password.mismatch");

        User user = userRepository.findByForgotPasswordToken(TokenHasher.hash(input.resetToken())).orElseThrow(UnauthorizedException::new);

        if (isTokenExpired(user)) throw new UnauthorizedException();

        user.setPassword(passwordEncoder.encode(input.password()));
        user.setForgotPasswordToken(null);
        user.setForgotPasswordTokenExpiresAt(null);

        userRepository.save(user);
    }

    private boolean isTokenExpired(User user) {
        return user.getForgotPasswordTokenExpiresAt() == null || user.getForgotPasswordTokenExpiresAt().isBefore(LocalDateTime.now());
    }
}
