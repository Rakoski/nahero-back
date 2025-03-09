package br.com.naheroback.modules.enrollment.entities;

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
@Table(name = "enrollment_statuses")
@SQLDelete(sql = "UPDATE enrollment_statuses SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class EnrollmentStatus extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;
    
    @Column
    private String description;
}