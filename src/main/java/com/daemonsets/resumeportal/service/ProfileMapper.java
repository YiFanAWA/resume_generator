package com.daemonsets.resumeportal.service;

import com.daemonsets.resumeportal.model.Education;
import com.daemonsets.resumeportal.model.Job;
import com.daemonsets.resumeportal.model.UserProfile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ProfileMapper {

    public Map<String, Object> toPrivateProfile(UserProfile profile) {
        Map<String, Object> response = toPublicProfile(profile);
        response.put("userName", profile.getUserName());
        response.put("email", profile.getEmail());
        response.put("phone", profile.getPhone());
        response.putAll(toShareStatus(profile));
        return response;
    }

    public Map<String, Object> toPublicProfile(UserProfile profile) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("firstName", profile.getFirstName());
        response.put("lastName", profile.getLastName());
        response.put("designation", profile.getDesignation());
        response.put("summary", profile.getSummary());
        response.put("jobs", safeList(profile.getJobs()).stream().map(this::toJobResponse).collect(Collectors.toList()));
        response.put("educations", safeList(profile.getEducations()).stream().map(this::toEducationResponse).collect(Collectors.toList()));
        response.put("skills", new ArrayList<>(safeList(profile.getSkills())));
        response.put("theme", profile.getTheme());
        return response;
    }

    public Map<String, Object> toShareStatus(UserProfile profile) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", profile.isPublic() ? "Share link active" : "Share link disabled");
        response.put("shareToken", profile.getShareToken());
        response.put("shareUrl", profile.getShareToken() == null ? null : "/app/public-share?token=" + profile.getShareToken());
        response.put("isPublic", profile.isPublic());
        response.put("shareExpiresAt", formatDateTime(profile.getShareExpiresAt()));
        response.put("shareMaxViews", profile.getShareMaxViews());
        response.put("shareViewCount", profile.getShareViewCount());
        response.put("shareLastViewedAt", formatDateTime(profile.getShareLastViewedAt()));
        response.put("sharePasswordProtected", profile.hasSharePassword());
        response.put("shareExpired", profile.isShareExpired());
        response.put("shareLimitReached", profile.isShareViewLimitReached());
        return response;
    }

    private Map<String, Object> toJobResponse(Job job) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("company", job.getCompany());
        response.put("designation", job.getDesignation());
        response.put("startDate", formatIsoDate(job.getStartDate()));
        response.put("endDate", formatIsoDate(job.getEndDate()));
        response.put("currentJob", job.isCurrentJob());
        response.put("formattedStartDate", job.getFormattedStartDate());
        response.put("formattedEndDate", job.getFormattedEndDate());
        response.put("responsibilities", new ArrayList<>(safeList(job.getResponsibilities())));
        return response;
    }

    private Map<String, Object> toEducationResponse(Education education) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("college", education.getCollege());
        response.put("qualification", education.getQualification());
        response.put("startDate", formatIsoDate(education.getStartDate()));
        response.put("endDate", formatIsoDate(education.getEndDate()));
        response.put("formattedStartDate", education.getFormattedStartDate());
        response.put("formattedEndDate", education.getFormattedEndDate());
        response.put("summary", education.getSummary());
        return response;
    }

    private String formatIsoDate(LocalDate value) {
        return value == null ? null : value.toString();
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.toString();
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }
}
