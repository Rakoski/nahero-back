package br.com.naheroback.modules.user.entities.enums;

import lombok.Getter;

@Getter
public enum RolesEnum {
    IS_STUDENT(1),
    IS_TEACHER(2),
    IS_ADMIN(3);

    private final int value;

    RolesEnum(int value) {
        this.value = value;
    }
}
