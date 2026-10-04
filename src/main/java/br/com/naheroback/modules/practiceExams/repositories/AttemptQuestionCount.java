package br.com.naheroback.modules.practiceExams.repositories;

import java.time.LocalDateTime;

public interface AttemptQuestionCount {
    LocalDateTime getEndTime();
    Long getAnswered();
    Long getCorrect();
}
