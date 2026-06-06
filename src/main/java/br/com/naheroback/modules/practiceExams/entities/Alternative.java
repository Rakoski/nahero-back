package br.com.naheroback.modules.practiceExams.entities;

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
@Table(name = "alternatives", indexes = {
    @Index(name = "idx_alternatives_question_id", columnList = "question_id"),
    @Index(name = "idx_alternatives_base_alternative_id", columnList = "base_alternative_id"),
    @Index(name = "idx_alternatives_teacher_id", columnList = "teacher_id"),
    @Index(name = "idx_alternatives_is_correct", columnList = "is_correct"),
    @Index(name = "idx_alternatives_is_active", columnList = "is_active"),
    @Index(name = "idx_alternatives_deleted_at", columnList = "deleted_at"),
    @Index(name = "idx_alternatives_version", columnList = "base_alternative_id, version")
})
@SQLDelete(sql = "UPDATE alternatives SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Alternative extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "base_alternative_id")
    private Alternative baseAlternative;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private Question question;
    
    @Column(name = "image_url")
    private String imageUrl;
    
    @Column(nullable = false)
    private String content;
    
    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect;
    
    @Column(nullable = false)
    private Integer version = 1;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private User teacher;
}