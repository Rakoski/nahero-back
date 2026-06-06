package br.com.naheroback.modules.practiceExams.services;

import br.com.naheroback.common.exceptions.custom.NotFoundException;
import br.com.naheroback.common.utils.Constants;
import br.com.naheroback.modules.practiceExams.entities.*;
import br.com.naheroback.modules.practiceExams.entities.enums.QuestionTypeEnum;
import br.com.naheroback.modules.practiceExams.repositories.AlternativeRepository;
import br.com.naheroback.modules.practiceExams.repositories.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Shared answer-processing and scoring logic used by the finish and time-out flows.
 * Both flows score a student's answers identically; only the resulting attempt status differs,
 * which is decided by the calling use case before invoking {@link #scoreAttempt}.
 */
@Service
@RequiredArgsConstructor
public class AttemptScoringService {
    private final QuestionRepository questionRepository;
    private final AlternativeRepository alternativeRepository;

    /**
     * Neutral answer payload so callers don't have to depend on each other's request records.
     */
    public record AnswerData(
            String questionId,
            List<String> alternativeIds,
            String descriptiveAnswer,
            Double sumAnswer
    ) {}

    /**
     * Processes the supplied answers, mutates the attempt's score/passed fields,
     * and returns the persisted-ready {@link StudentAnswer} list (not yet saved).
     */
    public List<StudentAnswer> scoreAttempt(StudentPracticeAttempt attempt, List<AnswerData> answerRequests) {
        List<StudentAnswer> answers = processAnswers(attempt, answerRequests);
        calculateScoreAndUpdateAttempt(attempt, answers);
        return answers;
    }

    private List<StudentAnswer> processAnswers(StudentPracticeAttempt attempt, List<AnswerData> answerRequests) {
        List<Integer> questionIds = answerRequests.stream()
                .map(request -> Integer.parseInt(request.questionId()))
                .toList();

        List<Question> questions = questionRepository.findAllById(questionIds);

        Map<Integer, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, question -> question));

        Map<Integer, List<Alternative>> alternativesMap = new HashMap<>();
        Map<Integer, List<Integer>> correctAlternativesMap = new HashMap<>();

        loadAlternatives(questions, alternativesMap, correctAlternativesMap);

        List<StudentAnswer> answers = new ArrayList<>();

        for (AnswerData answerRequest : answerRequests) {
            Integer questionId = Integer.parseInt(answerRequest.questionId());
            Question question = questionMap.get(questionId);

            if (question == null) throw NotFoundException.with(Question.class, "id", answerRequest.questionId());

            processAnswer(
                    answers,
                    question,
                    answerRequest,
                    attempt,
                    alternativesMap,
                    correctAlternativesMap
            );
        }

        return answers;
    }

    private void loadAlternatives(List<Question> questions,
                                  Map<Integer, List<Alternative>> alternativesMap,
                                  Map<Integer, List<Integer>> correctAlternativesMap) {
        for (Question question : questions) {
            List<Alternative> alternatives = alternativeRepository.findAllByQuestionId(question.getId());
            alternativesMap.put(question.getId(), alternatives);

            List<Integer> correctIds = alternatives.stream()
                    .filter(a -> Boolean.TRUE.equals(a.getIsCorrect()))
                    .map(Alternative::getId)
                    .toList();

            correctAlternativesMap.put(question.getId(), correctIds);
        }
    }

    private void processAnswer(List<StudentAnswer> answers,
                               Question question,
                               AnswerData answerRequest,
                               StudentPracticeAttempt attempt,
                               Map<Integer, List<Alternative>> alternativesMap,
                               Map<Integer, List<Integer>> correctAlternativesMap) {
        Integer questionTypeId = question.getQuestionType().getId();

        QuestionTypeEnum questionType = QuestionTypeEnum.fromId(questionTypeId);

        switch (questionType) {
            case MULTIPLE_CHOICE, TRUE_FALSE, OBJECTIVE ->
                    processChoiceQuestion(
                            answers,
                            question,
                            answerRequest,
                            attempt,
                            alternativesMap,
                            correctAlternativesMap,
                            questionType
                    );
            case DESCRIPTIVE ->
                    processDescriptiveQuestion(answers, question, attempt);
            case SUM ->
                    processSumQuestion(answers, question, attempt);
        }
    }

    private void processChoiceQuestion(List<StudentAnswer> answers,
                                       Question question,
                                       AnswerData answerRequest,
                                       StudentPracticeAttempt attempt,
                                       Map<Integer, List<Alternative>> alternativesMap,
                                       Map<Integer, List<Integer>> correctAlternativesMap,
                                       QuestionTypeEnum questionType) {
        Integer questionId = question.getId();
        List<Integer> selectedIds = answerRequest.alternativeIds() != null ?
                answerRequest.alternativeIds().stream().map(Integer::parseInt).toList() :
                List.of();

        List<Integer> correctIds = correctAlternativesMap.getOrDefault(questionId, List.of());

        boolean isCorrect = determineIfCorrect(selectedIds, correctIds, questionType);

        if (!selectedIds.isEmpty()) {
            for (Integer altId : selectedIds) {
                StudentAnswer answer = createAnswer(question, attempt, altId, isCorrect, alternativesMap);
                answers.add(answer);
            }
        } else {
            StudentAnswer answer = createEmptyAnswer(question, attempt, false);
            answers.add(answer);
        }
    }

    private boolean determineIfCorrect(List<Integer> selectedIds,
                                       List<Integer> correctIds,
                                       QuestionTypeEnum questionType) {
        return switch (questionType) {
            case MULTIPLE_CHOICE ->
                    selectedIds.size() == correctIds.size() && new HashSet<>(selectedIds).containsAll(correctIds);
            case TRUE_FALSE, OBJECTIVE ->
                    selectedIds.size() == 1 && correctIds.contains(selectedIds.getFirst());
            default -> false;
        };
    }

    private void processDescriptiveQuestion(List<StudentAnswer> answers,
                                            Question question,
                                            StudentPracticeAttempt attempt) {
        StudentAnswer answer = createEmptyAnswer(question, attempt, null);
        answers.add(answer);
    }

    private void processSumQuestion(List<StudentAnswer> answers,
                                    Question question,
                                    StudentPracticeAttempt attempt) {
        StudentAnswer answer = createEmptyAnswer(question, attempt, false);
        answers.add(answer);
    }

    private StudentAnswer createAnswer(Question question,
                                       StudentPracticeAttempt attempt,
                                       Integer alternativeId,
                                       Boolean isCorrect,
                                       Map<Integer, List<Alternative>> alternativesMap) {
        StudentAnswer answer = new StudentAnswer();
        answer.setStudentPracticeAttempt(attempt);
        answer.setQuestionId(question.getId());
        answer.setQuestionVersion(question.getVersion());
        answer.setSelectedAlternativeId(alternativeId);
        answer.setIsCorrect(isCorrect);

        for (Alternative alt : alternativesMap.getOrDefault(question.getId(), List.of())) {
            if (alt.getId().equals(alternativeId)) {
                answer.setSelectedAlternativeVersion(alt.getVersion());
                break;
            }
        }

        return answer;
    }

    private StudentAnswer createEmptyAnswer(Question question, StudentPracticeAttempt attempt, Boolean isCorrect) {
        StudentAnswer answer = new StudentAnswer();
        answer.setStudentPracticeAttempt(attempt);
        answer.setQuestionId(question.getId());
        answer.setQuestionVersion(question.getVersion());
        answer.setIsCorrect(isCorrect);
        return answer;
    }

    private void calculateScoreAndUpdateAttempt(StudentPracticeAttempt attempt, List<StudentAnswer> answers) {
        int actualScore = (int) answers.stream()
                .filter(a -> Boolean.TRUE.equals(a.getIsCorrect()))
                .map(StudentAnswer::getQuestionId)
                .distinct()
                .count();

        PracticeExam practiceExam = attempt.getPracticeExam();
        Integer passingScore = practiceExam.getPassingScore();

        int numberOfAttemptQuestions = practiceExam.getNumberOfQuestions() != null
                ? practiceExam.getNumberOfQuestions() : Constants.MAX_EXAM_QUESTIONS;

        double requiredCorrect = (passingScore / 100.0) * numberOfAttemptQuestions;
        boolean passed = actualScore >= requiredCorrect;

        attempt.setScore(actualScore);
        attempt.setPassed(passed);
    }
}
