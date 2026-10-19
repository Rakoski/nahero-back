package br.com.naheroback.modules.questionLists.useCases.listMine;

import java.time.LocalDateTime;

public record ListMyQuestionListsResponse(
        Integer id,
        String name,
        int questionCount,
        int answeredCount,
        LocalDateTime createdAt
) {}
