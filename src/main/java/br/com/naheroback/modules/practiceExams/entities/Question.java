package br.com.naheroback.modules.practiceExams.entities;

import br.com.naheroback.common.entities.BaseEntity;
import br.com.naheroback.modules.user.entities.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
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
@Table(name = "questions", indexes = {
    @Index(name = "idx_questions_practice_exam_id", columnList = "practice_exam_id"),
    @Index(name = "idx_questions_question_type_id", columnList = "question_type_id"),
    @Index(name = "idx_questions_base_question_id", columnList = "base_question_id"),
    @Index(name = "idx_questions_base_question_version", columnList = "base_question_id, version"),
    @Index(name = "idx_questions_teacher_id", columnList = "teacher_id"),
    @Index(name = "idx_questions_is_active", columnList = "is_active"),
    @Index(name = "idx_questions_deleted_at", columnList = "deleted_at"),
    @Index(name = "idx_questions_version", columnList = "version"),
    @Index(name = "idx_questions_practice_exam_domain", columnList = "practice_exam_id, domain"),
    @Index(name = "idx_questions_practice_exam_domain_id", columnList = "practice_exam_id, domain_id"),
    @Index(name = "idx_questions_domain_id", columnList = "domain_id")
})
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

    @Column
    private String language;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "domain_id")
    private Domain domain;

    @Setter(AccessLevel.NONE)
    @Column(name = "domain", length = 160)
    private String legacyDomain;

    public void setDomain(Domain domain) {
        this.domain = domain;
        this.legacyDomain = domain == null ? null : domain.getName();
    }

    public String getDomainName() {
        return domain == null ? null : domain.getName();
    }
}