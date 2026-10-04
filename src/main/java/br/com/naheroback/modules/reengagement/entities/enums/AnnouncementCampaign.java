package br.com.naheroback.modules.reengagement.entities.enums;

import lombok.Getter;

@Getter
public enum AnnouncementCampaign {
    FEEDBACK_LAUNCH("feedback_launch", "/how-it-works");

    private static final String MESSAGE_PREFIX = "email.announcement.";

    private final String slug;
    private final String landingPath;

    AnnouncementCampaign(String slug, String landingPath) {
        this.slug = slug;
        this.landingPath = landingPath;
    }

    public String messagePrefix() {
        return MESSAGE_PREFIX + slug;
    }
}
