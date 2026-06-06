package br.com.naheroback.modules.practiceExams.entities;

import br.com.naheroback.common.entities.BaseEntity;
import br.com.naheroback.common.utils.IntegerListConverter;
import br.com.naheroback.modules.enrollment.entities.Enrollment;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "student_practice_attempts", indexes = {
    @Index(name = "idx_student_practice_attempts_enrollment", columnList = "enrollment_id"),
    @Index(name = "idx_student_practice_attempts_practice_exam", columnList = "practice_exam_id")
})
@SQLDelete(sql = "UPDATE student_practice_attempts SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class StudentPracticeAttempt extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id")
    private Enrollment enrollment;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "practice_exam_id")
    private PracticeExam practiceExam;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status")
    private PracticeAttemptStatus attemptStatus;
    
    @Column(name = "start_time")
    private LocalDateTime startTime;
    
    @Column(name = "end_time")
    private LocalDateTime endTime;
    
    @Column
    private Integer score;
    
    @Column
    private Boolean passed;

    @Column(name = "shuffled_question_ids", columnDefinition = "TEXT")
    @Convert(converter = IntegerListConverter.class)
    private List<Integer> shuffledQuestionIds;

    @Column(nullable = false, length = 10)
    private String language;
}