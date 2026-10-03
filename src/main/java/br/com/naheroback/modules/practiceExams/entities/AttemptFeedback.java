package br.com.naheroback.modules.practiceExams.entities;

import br.com.naheroback.modules.practiceExams.entities.enums.AttemptFeedbackStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "attempt_feedback")
public class AttemptFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "attempt_id", nullable = false, unique = true)
    private Integer attemptId;

    @Column(nullable = false, length = 10)
    private String language;

    @Column(nullable = false, length = 64)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AttemptFeedbackStatus status;

    @Column(nullable = false)
    private Integer attempts;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column
    private String content;

    @Column(name = "prompt_hash", length = 64)
    private String promptHash;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
