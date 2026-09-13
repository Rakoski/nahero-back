package br.com.naheroback.modules.reengagement.useCases.unsubscribe;

import br.com.naheroback.modules.user.entities.User;
import br.com.naheroback.modules.user.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j(topic = "REENGAGEMENT")
@Service
@RequiredArgsConstructor
public class UnsubscribeFromReengagementUseCase {

    private final UserRepository userRepository;

    @Transactional
    public void execute(UnsubscribeFromReengagementRequest input) {
        userRepository.findByReengagementUnsubscribeToken(input.token())
                .filter(user -> !user.isOptedOutOfReengagement())
                .ifPresent(this::optOut);
    }

    private void optOut(User user) {
        user.setReengagementOptedOutAt(LocalDateTime.now());
        userRepository.save(user);
    }
}
