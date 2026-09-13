package br.com.naheroback.modules.reengagement.repositories;

import br.com.naheroback.common.repositories.BaseRepository;
import br.com.naheroback.modules.reengagement.entities.ReengagementEmail;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ReengagementEmailRepository extends BaseRepository<ReengagementEmail, Integer> {
    List<ReengagementEmail> findByUserIdInAndSentAtAfter(Collection<Integer> userIds, LocalDateTime sentAt);
}
