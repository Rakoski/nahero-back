package br.com.naheroback.modules.practiceExams.entities;

import br.com.naheroback.common.utils.IntegerListConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "question_study_answers", indexes = {
    @Index(name = "idx_question_study_answers_student_question", columnList = "student_id, question_id")
})
public class QuestionStudyAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "student_id", nullable = false)
    private Integer studentId;

    @Column(name = "question_id", nullable = false)
    private Integer questionId;

    @Column(name = "question_version", nullable = false)
    private Integer questionVersion;

    @Column(name = "selected_alternative_ids", nullable = false, columnDefinition = "TEXT")
    @Convert(converter = IntegerListConverter.class)
    private List<Integer> selectedAlternativeIds;

    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect;

    @Column(name = "question_list_id")
    private Integer questionListId;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
}
