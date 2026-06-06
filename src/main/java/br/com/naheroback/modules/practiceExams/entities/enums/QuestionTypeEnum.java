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

    public static QuestionTypeEnum fromId(Integer id) {
        if (id == null) return MULTIPLE_CHOICE;
        for (QuestionTypeEnum type : values()) if (type.getId() == id) return type;
        return MULTIPLE_CHOICE;
    }
}
