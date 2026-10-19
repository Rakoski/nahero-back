package br.com.naheroback.modules.practiceExams.useCases.questionBank.answer;

import java.util.List;

public record AnswerQuestionBankQuestionResponse(boolean correct, List<Integer> correctAlternativeIds, String explanation) {}
