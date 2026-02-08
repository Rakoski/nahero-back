package br.com.naheroback.modules.practiceExams.useCases.answer.listAnswered;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AnswerFilterDTO {
    private Boolean isCorrect;
    private String questionContent;
}

