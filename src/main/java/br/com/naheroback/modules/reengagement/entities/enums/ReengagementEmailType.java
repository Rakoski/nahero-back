package br.com.naheroback.modules.reengagement.entities.enums;

import lombok.Getter;

import java.time.Duration;
import java.util.List;

@Getter
public enum ReengagementEmailType {
    RESULT_FOLLOWUP(3, "result_followup", "/practice-exams"),
    WE_MISS_YOU(7, "we_miss_you", "/practice-exams"),
    STREAK_BROKEN(14, "streak_broken", "/student/dashboard"),
    NEW_CONTENT(30, "new_content", "/practice-exams"),
    PROGRESS_RECAP(90, "progress_recap", "/student/history"),
    LAST_CALL(180, "last_call", "/practice-exams");

    private static final String MESSAGE_PREFIX = "email.reengagement.";

    private final Duration delayAfterLastActivity;
    private final String slug;
    private final String landingPath;

    ReengagementEmailType(int delayDays, String slug, String landingPath) {
        this.delayAfterLastActivity = Duration.ofDays(delayDays);
        this.slug = slug;
        this.landingPath = landingPath;
    }

    public String messagePrefix() {
        return MESSAGE_PREFIX + slug;
    }

    public static List<ReengagementEmailType> sequence() {
        return List.of(values());
    }

    public static ReengagementEmailType first() {
        return values()[0];
    }
}
