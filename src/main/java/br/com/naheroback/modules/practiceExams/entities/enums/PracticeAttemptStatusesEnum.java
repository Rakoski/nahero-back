package br.com.naheroback.modules.practiceExams.entities.enums;

import lombok.Getter;

@Getter
public enum PracticeAttemptStatusesEnum {
    IN_PROGRESS(1),
    COMPLETED(2),
    ABANDONED(3),
    TIMED_OUT(4);

    private final Integer id;

    PracticeAttemptStatusesEnum(Integer id) {
        this.id = id;
    }
}
