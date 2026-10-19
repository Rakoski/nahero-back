package br.com.naheroback.modules.practiceExams.services;

import br.com.naheroback.common.exceptions.custom.BadRequestException;
import br.com.naheroback.common.exceptions.custom.NotFoundException;
import br.com.naheroback.modules.practiceExams.entities.Alternative;
import br.com.naheroback.modules.practiceExams.entities.Question;
import br.com.naheroback.modules.practiceExams.entities.QuestionStudyAnswer;
import br.com.naheroback.modules.practiceExams.entities.enums.QuestionTypeEnum;
import br.com.naheroback.modules.practiceExams.repositories.AlternativeRepository;
import br.com.naheroback.modules.practiceExams.repositories.QuestionBankRepository;
import br.com.naheroback.modules.practiceExams.repositories.QuestionStudyAnswerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudyAnswerService {
    private final QuestionBankRepository questionBankRepository;
    private final AlternativeRepository alternativeRepository;
    private final QuestionStudyAnswerRepository questionStudyAnswerRepository;

    public record Graded(boolean correct, List<Integer> correctAlternativeIds, String explanation) {}

    @Transactional
    public Graded answer(Integer studentId, Integer questionId, List<Integer> alternativeIds, Integer questionListId) {
        Question question = questionBankRepository.findVisible(questionId)
                .orElseThrow(() -> NotFoundException.with(Question.class, "id", questionId));

        List<Alternative> alternatives = alternativeRepository.findAllActiveByQuestionIdIn(List.of(questionId));
        Set<Integer> ownIds = alternatives.stream().map(Alternative::getId).collect(Collectors.toSet());
        List<Integer> selected = alternativeIds.stream().distinct().toList();
        if (!ownIds.containsAll(selected)) throw new BadRequestException("alternativeIds must belong to question " + questionId);

        List<Integer> correctIds = alternatives.stream()
                .filter(alternative -> Boolean.TRUE.equals(alternative.getIsCorrect()))
                .map(Alternative::getId)
                .toList();

        boolean correct = AttemptScoringService.determineIfCorrect(selected, correctIds, QuestionTypeEnum.fromId(question.getQuestionType().getId()));

        QuestionStudyAnswer answer = new QuestionStudyAnswer();
        answer.setStudentId(studentId);
        answer.setQuestionId(questionId);
        answer.setQuestionVersion(question.getVersion());
        answer.setSelectedAlternativeIds(selected);
        answer.setIsCorrect(correct);
        answer.setQuestionListId(questionListId);
        questionStudyAnswerRepository.save(answer);

        return new Graded(correct, correctIds, question.getExplanation());
    }
}
