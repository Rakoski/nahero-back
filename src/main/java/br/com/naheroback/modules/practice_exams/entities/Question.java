package br.com.naheroback.modules.practice_exams.entities;

import br.com.naheroback.common.entities.BaseEntity;
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
@Table(name = "questions")
@SQLDelete(sql = "UPDATE questions SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Question extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "base_question_id")
    private Question baseQuestion;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "practice_exam_id")
    private PracticeExam practiceExam;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_type_id")
    private QuestionType questionType;
    
    @Column(nullable = false)
    private String content;
    
    @Column(name = "image_url")
    private String imageUrl;
    
    @Column
    private String explanation;
    
    @Column
    private Integer points = 1;
    
    @Column(nullable = false)
    private Integer version = 1;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private User teacher;
}