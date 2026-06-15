package com.daemonsets.resumeportal.service;

import com.daemonsets.resumeportal.cache.PublicResumeCacheService;
import com.daemonsets.resumeportal.model.UserProfile;
import com.daemonsets.resumeportal.pdf.PdfExportProperties;
import com.daemonsets.resumeportal.pdf.PdfExportResult;
import com.daemonsets.resumeportal.pdf.PdfExportService;
import com.daemonsets.resumeportal.repository.UserProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProfileService {
    private final UserProfileRepository userProfileRepository;
    private final PublicResumeCacheService publicResumeCacheService;
    private final PdfExportService pdfExportService;
    private final PdfExportProperties pdfExportProperties;
    private final ProfileMapper profileMapper;

    public ProfileService(
            UserProfileRepository userProfileRepository,
            PublicResumeCacheService publicResumeCacheService,
            PdfExportService pdfExportService,
            PdfExportProperties pdfExportProperties,
            ProfileMapper profileMapper
    ) {
        this.userProfileRepository = userProfileRepository;
        this.publicResumeCacheService = publicResumeCacheService;
        this.pdfExportService = pdfExportService;
        this.pdfExportProperties = pdfExportProperties;
        this.profileMapper = profileMapper;
    }

    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> getPrivateProfile(String username) {
        return userProfileRepository.findByUserName(username)
                .map(profileMapper::toPrivateProfile);
    }

    @Transactional
    public boolean updateProfile(String username, UserProfile userProfile) {
        Optional<UserProfile> existingProfileOpt = userProfileRepository.findByUserNameForUpdate(username);
        if (existingProfileOpt.isEmpty()) {
            return false;
        }

        UserProfile existingProfile = existingProfileOpt.get();
        String shareToken = existingProfile.getShareToken();
        existingProfile.setFirstName(trimToNull(userProfile.getFirstName()));
        existingProfile.setLastName(trimToNull(userProfile.getLastName()));
        existingProfile.setEmail(trimToNull(userProfile.getEmail()));
        existingProfile.setPhone(trimToNull(userProfile.getPhone()));
        existingProfile.setDesignation(trimToNull(userProfile.getDesignation()));
        existingProfile.setSummary(trimToNull(userProfile.getSummary()));
        existingProfile.setTheme(normalizeTheme(userProfile.getTheme()));

        existingProfile.getJobs().clear();
        if (userProfile.getJobs() != null) {
            existingProfile.getJobs().addAll(userProfile.getJobs());
        }

        existingProfile.getEducations().clear();
        if (userProfile.getEducations() != null) {
            existingProfile.getEducations().addAll(userProfile.getEducations());
        }

        existingProfile.getSkills().clear();
        if (userProfile.getSkills() != null) {
            existingProfile.getSkills().addAll(userProfile.getSkills().stream()
                    .map(this::trimToNull)
                    .filter(skill -> skill != null)
                    .collect(Collectors.toList()));
        }

        userProfileRepository.save(existingProfile);
        evictPublicCacheAfterCommit(shareToken);
        return true;
    }

    @Transactional(readOnly = true)
    public Optional<PdfExportResult> exportPdf(String username) throws IOException {
        Optional<UserProfile> userProfileOptional = userProfileRepository.findByUserName(username);
        if (userProfileOptional.isEmpty()) {
            return Optional.empty();
        }

        UserProfile userProfile = userProfileOptional.get();
        if (profileTextCharacters(userProfile) > pdfExportProperties.getMaxProfileCharacters()) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Profile content is too large to export as PDF");
        }

        byte[] pdfBytes = pdfExportService.generatePdf(userProfile);
        if (pdfBytes.length > pdfExportProperties.getMaxOutputSize().toBytes()) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Generated PDF is too large");
        }
        return Optional.of(new PdfExportResult(pdfBytes, safePdfName(userProfile)));
    }

    private void evictPublicCacheAfterCommit(String shareToken) {
        if (shareToken == null || shareToken.isBlank()) {
            return;
        }

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publicResumeCacheService.evict(shareToken);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publicResumeCacheService.evict(shareToken);
            }
        });
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private int normalizeTheme(int theme) {
        if (theme < 1 || theme > 3) {
            return 1;
        }
        return theme;
    }

    private String safePdfName(UserProfile profile) {
        String first = Optional.ofNullable(profile.getFirstName()).orElse("");
        String last = Optional.ofNullable(profile.getLastName()).orElse("");
        String fullName = (first + "_" + last).replaceAll("[^a-zA-Z0-9_-]", "_");
        if (fullName.replace("_", "").isEmpty()) {
            fullName = profile.getUserName();
        }
        return fullName + "_Resume.pdf";
    }

    private long profileTextCharacters(UserProfile profile) {
        long total = 0;
        total += length(profile.getFirstName());
        total += length(profile.getLastName());
        total += length(profile.getEmail());
        total += length(profile.getPhone());
        total += length(profile.getDesignation());
        total += length(profile.getSummary());
        total += safeList(profile.getSkills()).stream().mapToLong(this::length).sum();
        total += safeList(profile.getJobs()).stream().mapToLong(job ->
                length(job.getCompany())
                        + length(job.getDesignation())
                        + safeList(job.getResponsibilities()).stream().mapToLong(this::length).sum()
        ).sum();
        total += safeList(profile.getEducations()).stream().mapToLong(education ->
                length(education.getCollege())
                        + length(education.getQualification())
                        + length(education.getSummary())
        ).sum();
        return total;
    }

    private long length(String value) {
        return value == null ? 0 : value.length();
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }
}
