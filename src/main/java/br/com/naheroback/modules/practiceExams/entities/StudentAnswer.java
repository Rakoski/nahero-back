package br.com.naheroback.modules.practiceExams.entities;

import br.com.naheroback.common.entities.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "student_answers")
@SQLDelete(sql = "UPDATE student_answers SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class StudentAnswer extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_practice_attempt_id")
    private StudentPracticeAttempt studentPracticeAttempt;
    
    @Column(name = "question_id", nullable = false)
    private Integer questionId;
    
    @Column(name = "question_version", nullable = false)
    private Integer questionVersion;
    
    @Column(name = "selected_alternative_id")
    private Integer selectedAlternativeId;
    
    @Column(name = "selected_alternative_version")
    private Integer selectedAlternativeVersion;
    
    @Column(name = "is_correct")
    private Boolean isCorrect;
}