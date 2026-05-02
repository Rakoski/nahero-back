package br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.getDashboardSummary;

import br.com.naheroback.common.utils.Constants;
import br.com.naheroback.modules.auth.services.AuthService;
import br.com.naheroback.modules.practiceExams.entities.PracticeExam;
import br.com.naheroback.modules.practiceExams.entities.StudentPracticeAttempt;
import br.com.naheroback.modules.practiceExams.entities.enums.PracticeAttemptStatusesEnum;
import br.com.naheroback.modules.practiceExams.repositories.StudentPracticeAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetDashboardSummaryUseCase {

    private static final int RECENT_SCORE_POINTS = 20;
    private static final int ACTIVITY_DAYS = 30;

    private static final Set<Integer> FINALIZED_STATUS_IDS = Set.of(
            PracticeAttemptStatusesEnum.COMPLETED.getId(),
            PracticeAttemptStatusesEnum.TIMED_OUT.getId()
    );

    private final StudentPracticeAttemptRepository studentPracticeAttemptRepository;

    @Transactional(readOnly = true)
    public GetDashboardSummaryResponse execute() {
        Integer studentId = AuthService.getUserFromToken().getId();

        List<StudentPracticeAttempt> attempts =
                studentPracticeAttemptRepository.findAllForStudentDashboard(studentId);

        List<StudentPracticeAttempt> finalized = attempts.stream()
                .filter(a -> a.getAttemptStatus() != null
                        && FINALIZED_STATUS_IDS.contains(a.getAttemptStatus().getId()))
                .toList();

        return GetDashboardSummaryResponse.builder()
                .totalAttempts(attempts.size())
                .completedAttempts(finalized.size())
                .passRate(passRate(finalized))
                .averageScore(averageScore(finalized))
                .bestScore(bestScore(finalized))
                .totalStudyMinutes(totalStudyMinutes(finalized))
                .currentStreakDays(currentStreakDays(attempts))
                .attemptsByStatus(attemptsByStatus(attempts))
                .scoreOverTime(scoreOverTime(finalized))
                .byPracticeExam(byPracticeExam(finalized))
                .activityLast30Days(activityLast30Days(attempts))
                .currentInProgress(currentInProgress(attempts))
                .lastFailed(lastFailed(finalized))
                .build();
    }

    private Double passRate(List<StudentPracticeAttempt> finalized) {
        if (finalized.isEmpty()) return null;
        long passed = finalized.stream().filter(a -> Boolean.TRUE.equals(a.getPassed())).count();
        return (double) passed / finalized.size();
    }

    private Double averageScore(List<StudentPracticeAttempt> finalized) {
        return finalized.stream()
                .map(GetDashboardSummaryUseCase::scorePercent)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .stream().boxed().findFirst().orElse(null);
    }

    private Integer bestScore(List<StudentPracticeAttempt> finalized) {
        return finalized.stream()
                .map(GetDashboardSummaryUseCase::scorePercent)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(null);
    }

    private static Integer scorePercent(StudentPracticeAttempt attempt) {
        if (attempt.getScore() == null || attempt.getPracticeExam() == null) {
            return null;
        }
        Integer numberOfQuestions = attempt.getPracticeExam().getNumberOfQuestions();
        int total = numberOfQuestions != null ? numberOfQuestions : Constants.MAX_EXAM_QUESTIONS;
        if (total == 0) return null;
        return (int) Math.round(100.0 * attempt.getScore() / total);
    }

    private Long totalStudyMinutes(List<StudentPracticeAttempt> finalized) {
        return finalized.stream()
                .filter(a -> a.getStartTime() != null && a.getEndTime() != null)
                .mapToLong(a -> Duration.between(a.getStartTime(), a.getEndTime()).toMinutes())
                .sum();
    }

    private Integer currentStreakDays(List<StudentPracticeAttempt> attempts) {
        Set<LocalDate> activeDays = attempts.stream()
                .map(StudentPracticeAttempt::getStartTime)
                .filter(Objects::nonNull)
                .map(LocalDateTime::toLocalDate)
                .collect(Collectors.toSet());

        int streak = 0;
        LocalDate cursor = LocalDate.now();
        if (!activeDays.contains(cursor)) {
            cursor = cursor.minusDays(1);
            if (!activeDays.contains(cursor)) return 0;
        }
        while (activeDays.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private Map<String, Integer> attemptsByStatus(List<StudentPracticeAttempt> attempts) {
        Map<String, Integer> result = new HashMap<>();
        for (StudentPracticeAttempt a : attempts) {
            if (a.getAttemptStatus() == null) continue;
            result.merge(a.getAttemptStatus().getName(), 1, Integer::sum);
        }
        return result;
    }

    private List<GetDashboardSummaryResponse.ScorePoint> scoreOverTime(List<StudentPracticeAttempt> finalized) {
        return finalized.stream()
                .filter(a -> a.getEndTime() != null)
                .sorted(Comparator.comparing(StudentPracticeAttempt::getEndTime))
                .skip(Math.max(0, finalized.size() - RECENT_SCORE_POINTS))
                .map(a -> GetDashboardSummaryResponse.ScorePoint.builder()
                        .attemptId(a.getId())
                        .endTime(a.getEndTime())
                        .score(scorePercent(a))
                        .passed(a.getPassed())
                        .practiceExamTitle(a.getPracticeExam() != null ? a.getPracticeExam().getTitle() : null)
                        .build())
                .toList();
    }

    private List<GetDashboardSummaryResponse.PracticeExamPerformance> byPracticeExam(
            List<StudentPracticeAttempt> finalized) {
        Map<Integer, List<StudentPracticeAttempt>> grouped = new HashMap<>();
        for (StudentPracticeAttempt a : finalized) {
            PracticeExam pe = a.getPracticeExam();
            if (pe == null) continue;
            grouped.computeIfAbsent(pe.getId(), k -> new java.util.ArrayList<>()).add(a);
        }

        return grouped.entrySet().stream()
                .map(entry -> {
                    List<StudentPracticeAttempt> group = entry.getValue();
                    StudentPracticeAttempt last = group.stream()
                            .max(Comparator.comparing(StudentPracticeAttempt::getEndTime,
                                    Comparator.nullsFirst(Comparator.naturalOrder())))
                            .orElseThrow();
                    long passed = group.stream().filter(a -> Boolean.TRUE.equals(a.getPassed())).count();
                    Integer best = group.stream()
                            .map(GetDashboardSummaryUseCase::scorePercent)
                            .filter(Objects::nonNull)
                            .max(Integer::compareTo)
                            .orElse(null);

                    return GetDashboardSummaryResponse.PracticeExamPerformance.builder()
                            .practiceExamId(entry.getKey())
                            .title(last.getPracticeExam().getTitle())
                            .attempts(group.size())
                            .bestScore(best)
                            .passRate((double) passed / group.size())
                            .lastScore(scorePercent(last))
                            .lastEndTime(last.getEndTime())
                            .build();
                })
                .sorted(Comparator.comparing(
                        GetDashboardSummaryResponse.PracticeExamPerformance::getLastEndTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private List<GetDashboardSummaryResponse.DailyActivity> activityLast30Days(
            List<StudentPracticeAttempt> attempts) {
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(ACTIVITY_DAYS - 1L);

        Map<LocalDate, Integer> counts = new TreeMap<>();
        for (long i = 0; i < ACTIVITY_DAYS; i++) {
            counts.put(from.plusDays(i), 0);
        }

        for (StudentPracticeAttempt a : attempts) {
            if (a.getStartTime() == null) continue;
            LocalDate day = a.getStartTime().toLocalDate();
            if (day.isBefore(from) || day.isAfter(today)) continue;
            counts.merge(day, 1, Integer::sum);
        }

        return counts.entrySet().stream()
                .map(e -> GetDashboardSummaryResponse.DailyActivity.builder()
                        .date(e.getKey())
                        .attempts(e.getValue())
                        .build())
                .toList();
    }

    private GetDashboardSummaryResponse.InProgressAttempt currentInProgress(
            List<StudentPracticeAttempt> attempts) {
        return attempts.stream()
                .filter(a -> a.getAttemptStatus() != null
                        && Objects.equals(a.getAttemptStatus().getId(),
                                          PracticeAttemptStatusesEnum.IN_PROGRESS.getId()))
                .max(Comparator.comparing(StudentPracticeAttempt::getStartTime,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(a -> GetDashboardSummaryResponse.InProgressAttempt.builder()
                        .attemptId(a.getId())
                        .practiceExamId(a.getPracticeExam() != null ? a.getPracticeExam().getId() : null)
                        .practiceExamTitle(a.getPracticeExam() != null ? a.getPracticeExam().getTitle() : null)
                        .startTime(a.getStartTime())
                        .build())
                .orElse(null);
    }

    private GetDashboardSummaryResponse.RecentAttempt lastFailed(List<StudentPracticeAttempt> finalized) {
        return finalized.stream()
                .filter(a -> Boolean.FALSE.equals(a.getPassed()))
                .max(Comparator.comparing(StudentPracticeAttempt::getEndTime,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(a -> GetDashboardSummaryResponse.RecentAttempt.builder()
                        .attemptId(a.getId())
                        .practiceExamId(a.getPracticeExam() != null ? a.getPracticeExam().getId() : null)
                        .practiceExamTitle(a.getPracticeExam() != null ? a.getPracticeExam().getTitle() : null)
                        .score(scorePercent(a))
                        .endTime(a.getEndTime())
                        .build())
                .orElse(null);
    }
}
