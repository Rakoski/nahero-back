package br.com.naheroback.modules.emailOutbox.services;

import br.com.naheroback.modules.emailOutbox.entities.EmailOutbox;
import br.com.naheroback.modules.emailOutbox.entities.enums.EmailOutboxStatus;
import br.com.naheroback.modules.emailOutbox.entities.enums.EmailOutboxType;
import br.com.naheroback.modules.emailOutbox.repositories.EmailOutboxRepository;
import br.com.naheroback.modules.user.entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EmailOutboxService {

    private final EmailOutboxRepository emailOutboxRepository;

    public void enqueue(User user, EmailOutboxType type) {
        EmailOutbox email = new EmailOutbox();
        email.setUser(user);
        email.setEmailType(type);
        email.setLocale(LocaleContextHolder.getLocale().toLanguageTag());
        email.setStatus(EmailOutboxStatus.PENDING);
        email.setAttempts(0);
        email.setNextAttemptAt(LocalDateTime.now());

        emailOutboxRepository.save(email);
    }
}
