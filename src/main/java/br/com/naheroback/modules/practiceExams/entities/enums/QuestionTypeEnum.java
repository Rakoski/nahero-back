package br.com.naheroback.modules.practiceExams.entities.enums;

import lombok.Getter;

@Getter
public enum QuestionTypeEnum {
    MULTIPLE_CHOICE(1),
    TRUE_FALSE(2),
    OBJECTIVE(3),
    DESCRIPTIVE(4),
    SUM(5);

    private final int id;

    QuestionTypeEnum(int id) {
        this.id = id;
    }
}
