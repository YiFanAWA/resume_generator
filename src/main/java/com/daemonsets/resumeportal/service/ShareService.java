package com.daemonsets.resumeportal.service;

import com.daemonsets.resumeportal.cache.PublicResumeCacheService;
import com.daemonsets.resumeportal.model.UserProfile;
import com.daemonsets.resumeportal.repository.UserProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Optional;

@Service
public class ShareService {
    private final UserProfileRepository userProfileRepository;
    private final PublicResumeCacheService publicResumeCacheService;
    private final PasswordEncoder passwordEncoder;
    private final ProfileMapper profileMapper;

    public ShareService(
            UserProfileRepository userProfileRepository,
            PublicResumeCacheService publicResumeCacheService,
            PasswordEncoder passwordEncoder,
            ProfileMapper profileMapper
    ) {
        this.userProfileRepository = userProfileRepository;
        this.publicResumeCacheService = publicResumeCacheService;
        this.passwordEncoder = passwordEncoder;
        this.profileMapper = profileMapper;
    }

    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> getShareStatus(String username) {
        return userProfileRepository.findByUserName(username)
                .map(profileMapper::toShareStatus);
    }

    @Transactional
    public Optional<Map<String, Object>> generateShareToken(String username, Map<String, Object> shareSettings) {
        Optional<UserProfile> profileOpt = userProfileRepository.findByUserNameForUpdate(username);
        if (profileOpt.isEmpty()) {
            return Optional.empty();
        }

        UserProfile profile = profileOpt.get();
        String previousShareToken = profile.getShareToken();
        profile.generateShareToken();
        profile.setPublic(true);
        applyShareSettings(profile, shareSettings);
        userProfileRepository.save(profile);
        evictPublicCacheAfterCommit(previousShareToken);
        evictPublicCacheAfterCommit(profile.getShareToken());

        return Optional.of(profileMapper.toShareStatus(profile));
    }

    @Transactional
    public Optional<Map<String, Object>> updateShareSettings(String username, Map<String, Object> shareSettings) {
        Optional<UserProfile> profileOpt = userProfileRepository.findByUserNameForUpdate(username);
        if (profileOpt.isEmpty()) {
            return Optional.empty();
        }

        UserProfile profile = profileOpt.get();
        if (profile.getShareToken() == null || !profile.isPublic()) {
            throw new IllegalStateException("Generate a share link before updating share settings");
        }

        applyShareSettings(profile, shareSettings);
        userProfileRepository.save(profile);
        evictPublicCacheAfterCommit(profile.getShareToken());

        return Optional.of(profileMapper.toShareStatus(profile));
    }

    @Transactional
    public Optional<Map<String, Object>> revokeShareToken(String username) {
        Optional<UserProfile> profileOpt = userProfileRepository.findByUserNameForUpdate(username);
        if (profileOpt.isEmpty()) {
            return Optional.empty();
        }

        UserProfile profile = profileOpt.get();
        String previousShareToken = profile.getShareToken();
        profile.revokeShareToken();
        userProfileRepository.save(profile);
        evictPublicCacheAfterCommit(previousShareToken);

        return Optional.of(profileMapper.toShareStatus(profile));
    }

    @Transactional
    public PublicResumeResponse resolvePublicView(String shareToken, String password) {
        Optional<UserProfile> profileOpt = userProfileRepository.findByShareTokenForUpdate(shareToken);
        if (profileOpt.isEmpty() || !profileOpt.get().isPublic()) {
            return new PublicResumeResponse(HttpStatus.NOT_FOUND, Map.of("error", "Resume not found or private"));
        }

        UserProfile profile = profileOpt.get();
        if (profile.isShareExpired()) {
            return new PublicResumeResponse(HttpStatus.GONE, Map.of("error", "Share link has expired"));
        }
        if (profile.isShareViewLimitReached()) {
            return new PublicResumeResponse(HttpStatus.GONE, Map.of("error", "Share link view limit has been reached"));
        }
        if (profile.hasSharePassword()) {
            if (password == null || password.isBlank()) {
                return new PublicResumeResponse(HttpStatus.UNAUTHORIZED, Map.of(
                        "error", "Share password required",
                        "requiresPassword", true
                ));
            }
            if (!passwordEncoder.matches(password, profile.getSharePasswordHash())) {
                return new PublicResumeResponse(HttpStatus.UNAUTHORIZED, Map.of(
                        "error", "Incorrect share password",
                        "requiresPassword", true
                ));
            }
        }

        profile.recordShareView();
        userProfileRepository.save(profile);

        Optional<Map<String, Object>> cachedProfile = publicResumeCacheService.get(shareToken);
        if (cachedProfile.isPresent()) {
            return new PublicResumeResponse(HttpStatus.OK, cachedProfile.get());
        }

        Map<String, Object> response = profileMapper.toPublicProfile(profile);
        publicResumeCacheService.put(shareToken, response);
        return new PublicResumeResponse(HttpStatus.OK, response);
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

    private void applyShareSettings(UserProfile profile, Map<String, Object> shareSettings) {
        if (shareSettings == null) {
            return;
        }

        if (shareSettings.containsKey("expiresAt")) {
            profile.setShareExpiresAt(parseExpiresAt(shareSettings.get("expiresAt")));
        }
        if (shareSettings.containsKey("maxViews")) {
            profile.setShareMaxViews(parseMaxViews(shareSettings.get("maxViews")));
        }
        if (Boolean.TRUE.equals(asBoolean(shareSettings.get("clearPassword")))) {
            profile.setSharePasswordHash(null);
        } else if (shareSettings.containsKey("password")) {
            Object passwordValue = shareSettings.get("password");
            String password = passwordValue == null ? null : trimToNull(String.valueOf(passwordValue));
            if (password != null) {
                profile.setSharePasswordHash(passwordEncoder.encode(password));
            }
        }
    }

    private LocalDateTime parseExpiresAt(Object value) {
        String rawValue = value == null ? null : trimToNull(String.valueOf(value));
        if (rawValue == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(rawValue);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("share expiration must use ISO local datetime format, for example 2026-06-05T18:30", exception);
        }
    }

    private Integer parseMaxViews(Object value) {
        if (value == null) {
            return null;
        }
        String rawValue = trimToNull(String.valueOf(value));
        if (rawValue == null) {
            return null;
        }
        try {
            int maxViews = Integer.parseInt(rawValue);
            return maxViews > 0 ? maxViews : null;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("share max views must be a positive number", exception);
        }
    }

    private Boolean asBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return value == null ? Boolean.FALSE : Boolean.parseBoolean(String.valueOf(value));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public record PublicResumeResponse(HttpStatus status, Map<String, Object> body) {
    }
}
