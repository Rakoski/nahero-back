package br.com.naheroback.modules.instructor.useCases.createQuestions;

import java.util.List;

public record CreateInstructorQuestionsResponse(int created, List<Integer> questionIds) {
}
