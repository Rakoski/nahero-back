package br.com.naheroback.modules.practiceExams.useCases.feedback.answerPracticeQuestion;

import java.util.List;

public record AnswerPracticeQuestionResponse(Boolean correct, List<Integer> correctAlternativeIds, String explanation) {}
