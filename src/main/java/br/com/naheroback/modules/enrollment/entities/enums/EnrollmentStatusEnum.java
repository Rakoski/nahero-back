package br.com.naheroback.modules.enrollment.entities.enums;

import lombok.Getter;

@Getter
public enum EnrollmentStatusEnum {
    ACTIVE(1),
    COMPLETED(2),
    EXPIRED(3),
    SUSPENDED(4),
    CANCELLED(5);

    private final Integer id;

    EnrollmentStatusEnum(Integer id) {
        this.id = id;
    }
}
