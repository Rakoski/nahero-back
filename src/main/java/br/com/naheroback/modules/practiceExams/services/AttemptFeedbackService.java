package br.com.naheroback.modules.practiceExams.services;

import br.com.naheroback.common.utils.Constants;
import br.com.naheroback.modules.practiceExams.entities.AttemptFeedback;
import br.com.naheroback.modules.practiceExams.entities.Question;
import br.com.naheroback.modules.practiceExams.entities.StudentAnswer;
import br.com.naheroback.modules.practiceExams.entities.StudentPracticeAttempt;
import br.com.naheroback.modules.practiceExams.entities.enums.AttemptFeedbackStatus;
import br.com.naheroback.modules.practiceExams.entities.enums.PracticeAttemptStatusesEnum;
import br.com.naheroback.modules.practiceExams.repositories.AttemptFeedbackRepository;
import br.com.naheroback.modules.practiceExams.repositories.QuestionRepository;
import br.com.naheroback.modules.practiceExams.repositories.StudentAnswerRepository;
import br.com.naheroback.modules.practiceExams.repositories.StudentPracticeAttemptRepository;
import br.com.naheroback.modules.practiceExams.services.AttemptDomainBreakdownService.DomainScore;
import br.com.naheroback.modules.practiceExams.services.StudyPlan.Priority;
import br.com.naheroback.providers.deepseek.DeepSeekClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j(topic = "ATTEMPT_FEEDBACK")
@Service
@RequiredArgsConstructor
public class AttemptFeedbackService {

    private static final int MAX_PREVIOUS_SCORES = 5;
    private static final int MAX_MISSED_QUESTIONS = 30;
    private static final int MAX_QUESTION_LENGTH = 600;
    private static final int PRIORITY_COUNT = 3;
    private static final int FAILURE_REASON_MAX_LENGTH = 500;

    private static final String SYSTEM_PROMPT = """
            You are a study coach for IT certification exams. The user message is a JSON object describing one \
            practice exam attempt: the exam title, the score, the number of correct answers per exam domain, the \
            text of the questions the student missed with their domain, and the student's previous scores on the \
            same exam.

            Write a personalized study plan in %s. Respond with a single JSON object and nothing else, in exactly \
            this shape:
            {"summary": "...", "priorities": [{"domain": "...", "why": "...", "whatToStudy": "..."}], "plan": ["...", "..."]}

            The student reads this on a phone in a few seconds. Be extremely brief: no filler, no repetition \
            between fields, no full paragraphs.

            Rules:
            - "summary": one sentence of at most 20 words saying where to focus first.
            - "priorities": exactly 3 items, weakest domain first (one item per domain if the input has fewer than \
            3 domains). "domain" must be copied exactly from the input domain names, in English. "why": one short \
            clause of at most 12 words, based on the missed questions. "whatToStudy": 3 or 4 topic or service \
            names separated by commas, not a sentence.
            - "plan": 3 to 5 steps in the order to study them, each at most 8 words, starting with a verb.
            - Use numbers only by copying them from the input. Never calculate, estimate or invent numbers, \
            percentages or statistics.
            - Never include links, URLs, course names, book titles, websites or training providers.
            - Keep exam domain names in English exactly as given, even when writing in another language.
            """;

    private static final Map<String, String> LANGUAGE_NAMES = Map.of(
            "pt", "Brazilian Portuguese",
            "en", "English");

    private final StudentPracticeAttemptRepository studentPracticeAttemptRepository;
    private final StudentAnswerRepository studentAnswerRepository;
    private final QuestionRepository questionRepository;
    private final AttemptFeedbackRepository attemptFeedbackRepository;
    private final DeepSeekClient deepSeekClient;
    private final ObjectMapper objectMapper;

    private final Set<Integer> inFlight = ConcurrentHashMap.newKeySet();

    public record MissedQuestion(String domain, String question) {}

    public record PreviousScore(String date, Integer correct, Integer total) {}

    public record AttemptStats(String exam, String language, Integer correct, Integer total, List<DomainScore> domains, List<MissedQuestion> missedQuestions, List<PreviousScore> previousScores) {}

    public boolean claim(Integer attemptId) {
        return inFlight.add(attemptId);
    }

    public boolean isGenerating(Integer attemptId) {
        return inFlight.contains(attemptId);
    }

    @Async
    public void generate(Integer attemptId) {
        inFlight.add(attemptId);

        try {
            Optional<AttemptFeedback> existing = attemptFeedbackRepository.findByAttemptId(attemptId);
            if (existing.isPresent() && existing.get().getStatus() == AttemptFeedbackStatus.READY) return;

            Optional<StudentPracticeAttempt> attempt = studentPracticeAttemptRepository.findByIdWithDetails(attemptId);
            if (attempt.isEmpty() || !isFinished(attempt.get())) return;

            AttemptFeedback feedback = existing.orElseGet(() -> newFeedback(attempt.get()));
            feedback.setAttempts(feedback.getAttempts() + 1);

            try {
                writePlan(feedback, buildStats(attempt.get()));
            } catch (RuntimeException | JsonProcessingException e) {
                markFailed(feedback, e);
            }

            attemptFeedbackRepository.save(feedback);
        } catch (DataIntegrityViolationException e) {
            log.info("Study plan for attempt {} was stored concurrently", attemptId);
        } catch (RuntimeException e) {
            log.warn("Could not generate the study plan for attempt {}", attemptId, e);
        } finally {
            inFlight.remove(attemptId);
        }
    }

    private static boolean isFinished(StudentPracticeAttempt attempt) {
        return attempt.getScore() != null
                && !attempt.getAttemptStatus().getId().equals(PracticeAttemptStatusesEnum.IN_PROGRESS.getId());
    }

    private AttemptFeedback newFeedback(StudentPracticeAttempt attempt) {
        AttemptFeedback feedback = new AttemptFeedback();
        feedback.setAttemptId(attempt.getId());
        feedback.setLanguage(attempt.getLanguage());
        feedback.setModel(deepSeekClient.model());
        feedback.setStatus(AttemptFeedbackStatus.FAILED);
        feedback.setAttempts(0);
        return feedback;
    }

    private void writePlan(AttemptFeedback feedback, AttemptStats stats) throws JsonProcessingException {
        if (stats.domains().isEmpty()) {
            throw new IllegalStateException("Attempt has no answered questions with a domain");
        }

        String systemPrompt = SYSTEM_PROMPT.formatted(LANGUAGE_NAMES.getOrDefault(stats.language(), LANGUAGE_NAMES.get("en")));
        String userPrompt = objectMapper.writeValueAsString(stats);
        feedback.setPromptHash(sha256(systemPrompt + "\n" + userPrompt));

        StudyPlan plan = deepSeekClient.completeJson(systemPrompt, userPrompt, StudyPlan.class);
        String content = objectMapper.writeValueAsString(plan);
        checkPlan(plan, content, stats.domains());

        feedback.setStatus(AttemptFeedbackStatus.READY);
        feedback.setContent(content);
        feedback.setFailureReason(null);
    }

    private void markFailed(AttemptFeedback feedback, Exception e) {
        feedback.setStatus(AttemptFeedbackStatus.FAILED);
        feedback.setContent(null);
        feedback.setFailureReason(failureReason(e));
        log.warn("Study plan for attempt {} failed on try {}: {}",
                feedback.getAttemptId(), feedback.getAttempts(), feedback.getFailureReason());
    }

    private AttemptStats buildStats(StudentPracticeAttempt attempt) {
        List<StudentAnswer> answers = studentAnswerRepository.findAllByStudentPracticeAttemptId(attempt.getId());
        Map<Integer, Question> questionsById = answers.isEmpty()
                ? Map.of()
                : questionRepository.findAllByIdIn(answers.stream().map(StudentAnswer::getQuestionId).distinct().toList())
                        .stream()
                        .collect(Collectors.toMap(Question::getId, Function.identity()));

        Map<Integer, String> domainByQuestionId = questionsById.values().stream()
                .filter(question -> question.getDomain() != null && !question.getDomain().isBlank())
                .collect(Collectors.toMap(Question::getId, Question::getDomain));

        List<DomainScore> domains = AttemptDomainBreakdownService.breakdown(answers, domainByQuestionId);
        Map<String, Integer> weaknessRank = IntStream.range(0, domains.size()).boxed()
                .collect(Collectors.toMap(index -> domains.get(index).domain(), Function.identity()));

        Map<Integer, Boolean> correctByQuestionId = answers.stream()
                .collect(Collectors.toMap(StudentAnswer::getQuestionId,
                        answer -> Boolean.TRUE.equals(answer.getIsCorrect()),
                        (existing, replacement) -> existing,
                        LinkedHashMap::new));

        List<MissedQuestion> missedQuestions = correctByQuestionId.entrySet().stream()
                .filter(entry -> !entry.getValue())
                .map(entry -> questionsById.get(entry.getKey()))
                .filter(question -> question != null && domainByQuestionId.containsKey(question.getId()))
                .sorted(Comparator.comparing(question -> weaknessRank.get(question.getDomain())))
                .limit(MAX_MISSED_QUESTIONS)
                .map(question -> new MissedQuestion(question.getDomain(), plainText(question.getContent())))
                .toList();

        int total = attempt.getPracticeExam().getNumberOfQuestions() != null
                ? attempt.getPracticeExam().getNumberOfQuestions()
                : Constants.MAX_EXAM_QUESTIONS;

        List<PreviousScore> previousScores = studentPracticeAttemptRepository.findPreviousScored(
                        attempt.getEnrollment().getStudent().getId(),
                        attempt.getPracticeExam().getId(),
                        attempt.getId(),
                        attempt.getStartTime(),
                        PageRequest.of(0, MAX_PREVIOUS_SCORES)).stream()
                .map(previous -> new PreviousScore(
                        previous.getStartTime().toLocalDate().toString(),
                        previous.getScore(),
                        total))
                .toList();

        return new AttemptStats(
                attempt.getPracticeExam().getTitle(),
                attempt.getLanguage(),
                attempt.getScore(),
                total,
                domains,
                missedQuestions,
                previousScores);
    }

    private void checkPlan(StudyPlan plan, String content, List<DomainScore> domains) {
        int expectedPriorities = Math.min(PRIORITY_COUNT, domains.size());
        if (plan.priorities().size() != expectedPriorities) {
            throw new IllegalStateException("Study plan must have %d priorities".formatted(expectedPriorities));
        }

        Set<String> attemptDomains = domains.stream().map(DomainScore::domain).collect(Collectors.toSet());
        List<String> priorityDomains = plan.priorities().stream().map(Priority::domain).toList();

        priorityDomains.stream()
                .filter(domain -> !attemptDomains.contains(domain))
                .findFirst()
                .ifPresent(domain -> {
                    throw new IllegalStateException("Study plan priority domain is not in the attempt: " + domain);
                });

        if (new HashSet<>(priorityDomains).size() != priorityDomains.size()) {
            throw new IllegalStateException("Study plan repeats a priority domain");
        }

        String text = content.toLowerCase();
        if (text.contains("http://") || text.contains("https://") || text.contains("www.")) {
            throw new IllegalStateException("Study plan contains a link");
        }
    }

    private String failureReason(Exception e) {
        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }

        String reason = "%s: %s".formatted(root.getClass().getSimpleName(), root.getMessage());
        return reason.length() > FAILURE_REASON_MAX_LENGTH ? reason.substring(0, FAILURE_REASON_MAX_LENGTH) : reason;
    }

    private String plainText(String content) {
        if (content == null) return "";
        String text = content.replaceAll("<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return text.length() > MAX_QUESTION_LENGTH ? text.substring(0, MAX_QUESTION_LENGTH) : text;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
