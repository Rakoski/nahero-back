package br.com.naheroback.modules.practiceExams.useCases.practiceExam.list;

public record ListPracticeExamsRequest(
    String search,
    String category,
    String difficultyLevel
) {
}
