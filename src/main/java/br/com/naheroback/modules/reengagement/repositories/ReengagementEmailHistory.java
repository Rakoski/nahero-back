package br.com.naheroback.modules.reengagement.repositories;

import br.com.naheroback.modules.reengagement.entities.enums.ReengagementEmailStatus;
import br.com.naheroback.modules.reengagement.entities.enums.ReengagementEmailType;

import java.time.LocalDateTime;

public interface ReengagementEmailHistory {
    Integer getUserId();
    ReengagementEmailType getEmailType();
    ReengagementEmailStatus getStatus();
    LocalDateTime getSentAt();
}
