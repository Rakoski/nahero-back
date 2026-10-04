package br.com.naheroback.modules.practiceExams.useCases.feedback.getFeedbackPage;

import br.com.naheroback.common.exceptions.custom.NotFoundException;
import br.com.naheroback.common.utils.Constants;
import br.com.naheroback.modules.auth.services.AuthService;
import br.com.naheroback.modules.practiceExams.entities.Alternative;
import br.com.naheroback.modules.practiceExams.entities.PracticeExam;
import br.com.naheroback.modules.practiceExams.entities.Question;
import br.com.naheroback.modules.practiceExams.entities.StudentAnswer;
import br.com.naheroback.modules.practiceExams.entities.StudentPracticeAttempt;
import br.com.naheroback.modules.practiceExams.entities.enums.QuestionTypeEnum;
import br.com.naheroback.modules.practiceExams.repositories.AlternativeRepository;
import br.com.naheroback.modules.practiceExams.repositories.FeedbackExamOption;
import br.com.naheroback.modules.practiceExams.repositories.QuestionRepository;
import br.com.naheroback.modules.practiceExams.repositories.StudentAnswerRepository;
import br.com.naheroback.modules.practiceExams.repositories.StudentPracticeAttemptRepository;
import br.com.naheroback.modules.practiceExams.services.AttemptDomainBreakdownService;
import br.com.naheroback.modules.practiceExams.services.AttemptDomainBreakdownService.DomainScore;
import br.com.naheroback.modules.practiceExams.services.PracticeAttemptEntitlementService;
import br.com.naheroback.modules.practiceExams.useCases.feedback.getFeedbackPage.GetFeedbackPageResponse.AttemptSummary;
import br.com.naheroback.modules.practiceExams.useCases.feedback.getFeedbackPage.GetFeedbackPageResponse.ExamOption;
import br.com.naheroback.modules.practiceExams.useCases.feedback.getFeedbackPage.GetFeedbackPageResponse.PracticeAlternative;
import br.com.naheroback.modules.practiceExams.useCases.feedback.getFeedbackPage.GetFeedbackPageResponse.PracticeQuestion;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetFeedbackPageUseCase {
    private static final int RECENT_ATTEMPTS = 5;
    private static final int PRACTICE_QUESTIONS = 5;

    private final StudentPracticeAttemptRepository studentPracticeAttemptRepository;
    private final StudentAnswerRepository studentAnswerRepository;
    private final QuestionRepository questionRepository;
    private final AlternativeRepository alternativeRepository;
    private final AttemptDomainBreakdownService domainBreakdownService;
    private final PracticeAttemptEntitlementService entitlement;

    @Transactional(readOnly = true)
    public GetFeedbackPageResponse execute(String practiceExamSlug) {
        entitlement.ensureCanSeeFeedback();

        Integer studentId = AuthService.getUserFromToken().getId();

        List<FeedbackExamOption> options = studentPracticeAttemptRepository.findFeedbackExamOptions(studentId);
        if (options.isEmpty()) return GetFeedbackPageResponse.empty();

        List<ExamOption> exams = options.stream()
                .map(option -> new ExamOption(option.getSlug(), option.getTitle()))
                .toList();
        FeedbackExamOption selected = selectExam(options, practiceExamSlug);
        ExamOption practiceExam = new ExamOption(selected.getSlug(), selected.getTitle());

        List<StudentPracticeAttempt> attempts = studentPracticeAttemptRepository.findRecentScored(
                studentId, selected.getPracticeExamId(), PageRequest.of(0, RECENT_ATTEMPTS));
        List<DomainScore> domains = aggregateDomains(attempts);
        String weakestDomain = AttemptDomainBreakdownService.weakest(domains).map(DomainScore::domain).orElse(null);

        StudentPracticeAttempt latest = attempts.getFirst();

        return new GetFeedbackPageResponse(
                exams,
                practiceExam,
                weakestDomain,
                latest.getId(),
                attempts.stream().map(this::toSummary).toList(),
                domains,
                weakestDomain == null
                        ? List.of()
                        : practiceQuestions(selected.getPracticeExamId(), weakestDomain, latest.getLanguage(), studentId));
    }

    private FeedbackExamOption selectExam(List<FeedbackExamOption> options, String practiceExamSlug) {
        if (practiceExamSlug == null || practiceExamSlug.isBlank()) return options.getFirst();

        return options.stream()
                .filter(option -> option.getSlug().equals(practiceExamSlug))
                .findFirst()
                .orElseThrow(() -> NotFoundException.with(PracticeExam.class, "slug", practiceExamSlug));
    }

    private List<DomainScore> aggregateDomains(List<StudentPracticeAttempt> attempts) {
        List<StudentAnswer> answers = studentAnswerRepository.findAllByStudentPracticeAttemptIdIn(
                attempts.stream().map(StudentPracticeAttempt::getId).toList());
        Map<Integer, String> domainByQuestionId = domainBreakdownService.domainsOf(answers);

        Map<Integer, List<StudentAnswer>> answersByAttempt = answers.stream()
                .collect(Collectors.groupingBy(answer -> answer.getStudentPracticeAttempt().getId()));

        return AttemptDomainBreakdownService.combine(answersByAttempt.values().stream()
                .map(attemptAnswers -> AttemptDomainBreakdownService.breakdown(attemptAnswers, domainByQuestionId))
                .toList());
    }

    private AttemptSummary toSummary(StudentPracticeAttempt attempt) {
        Integer total = attempt.getPracticeExam().getNumberOfQuestions() != null
                ? attempt.getPracticeExam().getNumberOfQuestions()
                : Constants.MAX_EXAM_QUESTIONS;
        return new AttemptSummary(attempt.getId(), attempt.getEndTime(), attempt.getScore(), total, attempt.getPassed());
    }

    private List<PracticeQuestion> practiceQuestions(Integer practiceExamId, String domain, String language, Integer studentId) {
        List<Integer> questionIds = questionRepository.findPracticeQuestionIds(
                practiceExamId, domain, language, studentId, PRACTICE_QUESTIONS);
        if (questionIds.isEmpty()) return List.of();

        Map<Integer, Question> questionsById = questionRepository.findAllByIdIn(questionIds).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));
        Map<Integer, List<PracticeAlternative>> alternativesByQuestionId = alternativeRepository
                .findAllActiveByQuestionIdIn(questionIds).stream()
                .collect(Collectors.groupingBy(
                        alternative -> alternative.getQuestion().getId(),
                        Collectors.mapping(this::toPracticeAlternative, Collectors.toList())));

        return questionIds.stream()
                .map(questionsById::get)
                .map(question -> new PracticeQuestion(
                        question.getId(),
                        QuestionTypeEnum.fromId(question.getQuestionType().getId()).name(),
                        question.getContent(),
                        question.getImageUrl(),
                        alternativesByQuestionId.getOrDefault(question.getId(), List.of())))
                .toList();
    }

    private PracticeAlternative toPracticeAlternative(Alternative alternative) {
        return new PracticeAlternative(alternative.getId(), alternative.getContent(), alternative.getImageUrl());
    }
}
