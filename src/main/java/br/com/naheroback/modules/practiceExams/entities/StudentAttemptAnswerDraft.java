package br.com.naheroback.modules.practiceExams.entities;

import br.com.naheroback.common.entities.BaseEntity;
import br.com.naheroback.common.utils.IntegerListConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "student_attempt_answer_drafts", indexes = {
    @Index(name = "idx_answer_drafts_attempt", columnList = "student_practice_attempt_id")
})
@SQLDelete(sql = "UPDATE student_attempt_answer_drafts SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class StudentAttemptAnswerDraft extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_practice_attempt_id")
    private StudentPracticeAttempt studentPracticeAttempt;

    @Column(name = "question_id", nullable = false)
    private Integer questionId;

    @Column(name = "selected_alternative_ids", nullable = false, columnDefinition = "TEXT")
    @Convert(converter = IntegerListConverter.class)
    private List<Integer> selectedAlternativeIds;
}
