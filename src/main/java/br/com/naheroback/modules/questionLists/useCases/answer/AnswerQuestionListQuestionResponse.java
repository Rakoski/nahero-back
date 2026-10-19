package br.com.naheroback.modules.questionLists.useCases.answer;

import java.util.List;

public record AnswerQuestionListQuestionResponse(boolean correct, List<Integer> correctAlternativeIds, String explanation) {}
