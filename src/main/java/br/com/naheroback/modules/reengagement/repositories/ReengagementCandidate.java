package br.com.naheroback.modules.reengagement.repositories;

import java.time.LocalDateTime;

public interface ReengagementCandidate {
    Integer getUserId();
    String getName();
    String getEmail();
    LocalDateTime getLastActivityAt();
}
