package br.com.naheroback.modules.reengagement.useCases.sendAnnouncement;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SendAnnouncementTestRequest(
        @Email(message = "{auth.email.invalid}")
        @NotBlank(message = "{auth.email.required}")
        String email,
        String language
) {}
