package br.com.naheroback.modules.practiceExams.useCases.studentPracticeAttempt.getDashboardSummary;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetDashboardSummaryResponse {
    private Integer totalAttempts;
    private Integer completedAttempts;
    private Double passRate;
    private Double averageScore;
    private Integer bestScore;
    private Long totalStudyMinutes;
    private Integer currentStreakDays;

    private Map<String, Integer> attemptsByStatus;

    private List<ScorePoint> scoreOverTime;
    private List<PracticeExamPerformance> byPracticeExam;
    private List<DailyActivity> activityLast30Days;

    private InProgressAttempt currentInProgress;
    private RecentAttempt lastFailed;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScorePoint {
        private Integer attemptId;
        private LocalDateTime endTime;
        private Integer score;
        private Boolean passed;
        private String practiceExamTitle;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PracticeExamPerformance {
        private Integer practiceExamId;
        private String title;
        private Integer attempts;
        private Integer bestScore;
        private Double passRate;
        private Integer lastScore;
        private LocalDateTime lastEndTime;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyActivity {
        private LocalDate date;
        private Integer attempts;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InProgressAttempt {
        private Integer attemptId;
        private Integer practiceExamId;
        private String practiceExamTitle;
        private LocalDateTime startTime;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentAttempt {
        private Integer attemptId;
        private Integer practiceExamId;
        private String practiceExamTitle;
        private Integer score;
        private LocalDateTime endTime;
    }
}
