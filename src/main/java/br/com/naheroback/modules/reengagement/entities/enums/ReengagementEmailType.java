package br.com.naheroback.modules.reengagement.entities.enums;

import lombok.Getter;

import java.time.Duration;
import java.util.List;

@Getter
public enum ReengagementEmailType {
    WE_MISS_YOU(7, "we_miss_you", "/practice-exams");

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
