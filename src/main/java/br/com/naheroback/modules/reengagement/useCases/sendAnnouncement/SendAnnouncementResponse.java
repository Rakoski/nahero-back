package br.com.naheroback.modules.reengagement.useCases.sendAnnouncement;

public record SendAnnouncementResponse(String campaign, int recipients, boolean dryRun) {}
