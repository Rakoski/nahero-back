package br.com.naheroback.modules.questionLists.entities;

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
@Table(name = "question_lists", indexes = {
    @Index(name = "idx_question_lists_student", columnList = "student_id")
})
@SQLDelete(sql = "UPDATE question_lists SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class QuestionList extends BaseEntity {

    @Column(name = "student_id", nullable = false)
    private Integer studentId;

    @Column(nullable = false, length = 120)
    private String name;
}
