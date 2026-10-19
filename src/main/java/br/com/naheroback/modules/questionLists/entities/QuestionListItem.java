package br.com.naheroback.modules.questionLists.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "question_list_items", indexes = {
    @Index(name = "idx_question_list_items_list_position", columnList = "question_list_id, position")
})
public class QuestionListItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "question_list_id", nullable = false)
    private Integer questionListId;

    @Column(name = "question_id", nullable = false)
    private Integer questionId;

    @Column(nullable = false)
    private Integer position;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
}
