package br.com.naheroback.modules.reengagement.entities;

import br.com.naheroback.common.entities.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "reengagement_dispatch_runs", indexes = {
    @Index(name = "idx_reengagement_dispatch_runs_started_at", columnList = "started_at")
})
@SQLDelete(sql = "UPDATE reengagement_dispatch_runs SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class ReengagementDispatchRun extends BaseEntity {

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at", nullable = false)
    private LocalDateTime finishedAt;

    @Column(name = "duration_ms", nullable = false)
    private Long durationMs;

    @Column(name = "candidates", nullable = false)
    private Integer candidates;

    @Column(name = "sent", nullable = false)
    private Integer sent;

    @Column(name = "failed", nullable = false)
    private Integer failed;

    @Column(name = "skipped", nullable = false)
    private Integer skipped;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "funnel")
    private String funnel;
}
