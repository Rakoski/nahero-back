package br.com.naheroback.modules.practiceExams.entities;

import br.com.naheroback.common.entities.BaseEntity;
import br.com.naheroback.modules.exams.entities.Exam;
import br.com.naheroback.modules.user.entities.User;
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
@Table(name = "practice_exams", indexes = {
    @Index(name = "idx_practice_exams_exam_id", columnList = "exam_id"),
    @Index(name = "idx_practice_exams_teacher_id", columnList = "teacher_id"),
    @Index(name = "idx_practice_exams_is_active", columnList = "is_active"),
    @Index(name = "idx_practice_exams_deleted_at", columnList = "deleted_at"),
    @Index(name = "idx_practice_exams_title", columnList = "title"),
    @Index(name = "idx_practice_exams_slug", columnList = "slug", unique = true)
})
@SQLDelete(sql = "UPDATE practice_exams SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class PracticeExam extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id")
    private Exam exam;
    
    @Column(nullable = false)
    private String title;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column
    private String description;
    
    @Column(name = "passing_score", nullable = false)
    private Integer passingScore;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private User teacher;
    
    @Column(name = "time_limit")
    private Integer timeLimit;
    
    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "number_of_questions")
    private Integer numberOfQuestions;
}