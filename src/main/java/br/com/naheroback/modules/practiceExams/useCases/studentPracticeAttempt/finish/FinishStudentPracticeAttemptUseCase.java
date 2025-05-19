package br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.finish;

import br.com.naheroback.common.exceptions.custom.NotFoundException;
import br.com.naheroback.common.exceptions.custom.ValidationException;
import br.com.naheroback.common.utils.Constants;
import br.com.naheroback.modules.practiceExams.entities.*;
import br.com.naheroback.modules.practiceExams.entities.enums.PracticeAttemptStatusesEnum;
import br.com.naheroback.modules.practiceExams.entities.enums.QuestionTypeEnum;
import br.com.naheroback.modules.practiceExams.repositories.AlternativeRepository;
import br.com.naheroback.modules.practiceExams.repositories.PracticeAttemptStatusRepository;
import br.com.naheroback.modules.practiceExams.repositories.QuestionRepository;
import br.com.naheroback.modules.practiceExams.repositories.StudentAnswerRepository;
import br.com.naheroback.modules.practiceExams.repositories.StudentPracticeAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FinishStudentPracticeAttemptUseCase {
    private final StudentPracticeAttemptRepository studentPracticeAttemptRepository;
    private final StudentAnswerRepository studentAnswerRepository;
    private final QuestionRepository questionRepository;
    private final AlternativeRepository alternativeRepository;
    private final PracticeAttemptStatusRepository practiceAttemptStatusRepository;

    @Transactional
    public FinishStudentPracticeAttemptResponse execute(FinishStudentPracticeAttemptRequest request) {
        StudentPracticeAttempt attempt = validateAndRetrieveAttempt(request.studentPracticeAttemptId());

        updateAttemptStatus(attempt);

        List<StudentAnswer> answers = processAnswers(attempt, request.answers());

        calculateScoreAndUpdateAttempt(attempt, answers);

        int questions = questionRepository.countAllByPracticeExamId(attempt.getPracticeExam().getId());

        studentPracticeAttemptRepository.save(attempt);
        studentAnswerRepository.saveAll(answers);

        return FinishStudentPracticeAttemptResponse.toPresentation(attempt, answers, questions);
    }

    private StudentPracticeAttempt validateAndRetrieveAttempt(Integer attemptId) {
        StudentPracticeAttempt attempt = studentPracticeAttemptRepository.findById(attemptId)
                .orElseThrow(() -> NotFoundException.with(StudentPracticeAttempt.class, "id", attemptId));

        if (!Objects.equals(attempt.getAttemptStatus().getId(), PracticeAttemptStatusesEnum.IN_PROGRESS.getId())) {
            throw new ValidationException("Cannot finish an attempt that is not in progress");
        }

        return attempt;
    }

    private void updateAttemptStatus(StudentPracticeAttempt attempt) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime maxEndTime = attempt.getStartTime().plusMinutes(attempt.getPracticeExam().getTimeLimit());

        boolean isTimedOut = now.isAfter(maxEndTime);
        Integer statusId = isTimedOut
                ? PracticeAttemptStatusesEnum.TIMED_OUT.getId()
                : PracticeAttemptStatusesEnum.COMPLETED.getId();

        PracticeAttemptStatus newStatus = practiceAttemptStatusRepository.findById(statusId)
                .orElseThrow(() -> NotFoundException.with(PracticeAttemptStatus.class, "id", statusId));

        attempt.setAttemptStatus(newStatus);
        attempt.setEndTime(now);
    }

    private List<StudentAnswer> processAnswers(StudentPracticeAttempt attempt,
                                               List<FinishStudentPracticeAttemptRequest.AnswerRequest> answerRequests) {
        List<Question> questions = questionRepository.findAllByPracticeExamId(attempt.getPracticeExam().getId());
        Map<Integer, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        Map<Integer, List<Alternative>> alternativesMap = new HashMap<>();
        Map<Integer, List<Integer>> correctAlternativesMap = new HashMap<>();
        loadAlternatives(questions, alternativesMap, correctAlternativesMap);

        List<StudentAnswer> answers = new ArrayList<>();

        for (FinishStudentPracticeAttemptRequest.AnswerRequest answerRequest : answerRequests) {
            Integer questionId = Integer.parseInt(answerRequest.questionId());
            Question question = questionMap.get(questionId);

            if (question == null) continue;

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
                               FinishStudentPracticeAttemptRequest.AnswerRequest answerRequest,
                               StudentPracticeAttempt attempt,
                               Map<Integer, List<Alternative>> alternativesMap,
                               Map<Integer, List<Integer>> correctAlternativesMap) {
        Integer questionTypeId = question.getQuestionType().getId();

        QuestionTypeEnum questionType = getQuestionTypeFromId(questionTypeId);

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

    private QuestionTypeEnum getQuestionTypeFromId(Integer typeId) {
        for (QuestionTypeEnum type : QuestionTypeEnum.values()) {
            if (type.getId() == typeId) {
                return type;
            }
        }
        return QuestionTypeEnum.MULTIPLE_CHOICE;
    }

    private void processChoiceQuestion(List<StudentAnswer> answers,
                                       Question question,
                                       FinishStudentPracticeAttemptRequest.AnswerRequest answerRequest,
                                       StudentPracticeAttempt attempt,
                                       Map<Integer, List<Alternative>> alternativesMap,
                                       Map<Integer, List<Integer>> correctAlternativesMap,
                                       QuestionTypeEnum questionType) {
        Integer questionId = question.getId();
        List<Integer> selectedIds = answerRequest.alternativeIds() != null ?
                answerRequest.alternativeIds().stream().map(Integer::parseInt).collect(Collectors.toList()) :
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

    private StudentAnswer createEmptyAnswer(Question question,
                                            StudentPracticeAttempt attempt,
                                            Boolean isCorrect) {
        StudentAnswer answer = new StudentAnswer();
        answer.setStudentPracticeAttempt(attempt);
        answer.setQuestionId(question.getId());
        answer.setQuestionVersion(question.getVersion());
        answer.setIsCorrect(isCorrect);
        return answer;
    }

    private void calculateScoreAndUpdateAttempt(StudentPracticeAttempt attempt, List<StudentAnswer> answers) {
        int score = 0;

        for (StudentAnswer answer : answers) {
            if (Boolean.TRUE.equals(answer.getIsCorrect())) {
                score++;
            }
        }

        Integer passingScore = attempt.getPracticeExam().getPassingScore();
        int requiredCorrectAnswers = (int)((passingScore / 100.0) * Constants.MAX_EXAM_QUESTIONS);
        boolean passed = score >= requiredCorrectAnswers;

        attempt.setScore(score);
        attempt.setPassed(passed);
    }
}