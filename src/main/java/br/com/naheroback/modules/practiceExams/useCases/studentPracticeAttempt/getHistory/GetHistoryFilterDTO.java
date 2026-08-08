package br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.getHistory;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GetHistoryFilterDTO {
    private Integer practiceExamId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer score;
}

