package com.daemonsets.resumeportal;

import com.daemonsets.resumeportal.models.Education;
import com.daemonsets.resumeportal.models.Job;
import com.daemonsets.resumeportal.models.User;
import com.daemonsets.resumeportal.models.UserProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api")
public class ApiController {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private PdfExportService pdfExportService;

    @Autowired
    private PublicResumeCacheService publicResumeCacheService;

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials, HttpServletRequest request) {
        String username = credentials.getOrDefault("username", "").trim();
        String password = credentials.getOrDefault("password", "");

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            request.getSession(true).setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    SecurityContextHolder.getContext()
            );
            upgradeLegacyPasswordIfNeeded(username, password);

            return ResponseEntity.ok(Map.of(
                    "message", "Login successful",
                    "username", username
            ));
        } catch (Exception e) {
            log.warn("Login failed for user: {}. Reason: {}", username, e.getMessage());
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }
    }

    @GetMapping("/auth/me")
    public ResponseEntity<?> currentUser(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("authenticated", false));
        }

        return ResponseEntity.ok(Map.of(
                "authenticated", true,
                "username", principal.getName()
        ));
    }

    @PostMapping("/auth/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> userData) {
        String userName = userData.getOrDefault("username", "").trim();
        String password = userData.get("password");
        String confirmPassword = userData.get("confirmPassword");

        if (userName.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username cannot be empty"));
        }
        if (password == null || password.length() < 8) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 8 characters long"));
        }
        if (!password.equals(confirmPassword)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Passwords do not match"));
        }
        if (userRepository.findByUserName(userName).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username already exists"));
        }

        User newUser = new User();
        newUser.setUserName(userName);
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setActive(true);
        newUser.setRoles("USER");
        userRepository.save(newUser);

        UserProfile userProfile = new UserProfile();
        userProfile.setUserName(userName);
        userProfile.setTheme(1);
        userProfile.setJobs(new ArrayList<>());
        userProfile.setEducations(new ArrayList<>());
        userProfile.setSkills(new ArrayList<>());
        userProfileRepository.save(userProfile);

        return ResponseEntity.ok(Map.of("message", "Registration successful"));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        request.getSession().invalidate();
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of("message", "Logout successful"));
    }

    @GetMapping("/profile")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getProfile(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));
        }

        return userProfileRepository.findByUserName(principal.getName())
                .map(profile -> ResponseEntity.ok(toPrivateProfile(profile)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/profile")
    @Transactional
    public ResponseEntity<?> updateProfile(@RequestBody UserProfile userProfile, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        Optional<UserProfile> existingProfileOpt = userProfileRepository.findByUserNameForUpdate(principal.getName());
        if (existingProfileOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
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
        return ResponseEntity.ok(Map.of("message", "Profile updated successfully"));
    }

    @GetMapping("/profile/export/pdf")
    public ResponseEntity<byte[]> exportPdf(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Not authenticated".getBytes());
        }

        Optional<UserProfile> userProfileOptional = userProfileRepository.findByUserName(principal.getName());
        if (userProfileOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            UserProfile userProfile = userProfileOptional.get();
            byte[] pdfBytes = pdfExportService.generatePdf(userProfile);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", safePdfName(userProfile));

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
        } catch (Exception e) {
            log.error("Failed to generate PDF for user {}", principal.getName(), e);
            return ResponseEntity.status(500).body("Failed to generate PDF".getBytes());
        }
    }

    @GetMapping("/profile/share")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getShareStatus(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        return userProfileRepository.findByUserName(principal.getName())
                .map(profile -> ResponseEntity.ok(toShareStatus(profile)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/profile/share/generate")
    @Transactional
    public ResponseEntity<?> generateShareToken(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        Optional<UserProfile> profileOpt = userProfileRepository.findByUserNameForUpdate(principal.getName());
        if (profileOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        UserProfile profile = profileOpt.get();
        String previousShareToken = profile.getShareToken();
        profile.generateShareToken();
        profile.setPublic(true);
        userProfileRepository.save(profile);
        evictPublicCacheAfterCommit(previousShareToken);
        evictPublicCacheAfterCommit(profile.getShareToken());

        return ResponseEntity.ok(toShareStatus(profile));
    }

    @PostMapping("/profile/share/revoke")
    @Transactional
    public ResponseEntity<?> revokeShareToken(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        Optional<UserProfile> profileOpt = userProfileRepository.findByUserNameForUpdate(principal.getName());
        if (profileOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        UserProfile profile = profileOpt.get();
        String previousShareToken = profile.getShareToken();
        profile.revokeShareToken();
        userProfileRepository.save(profile);
        evictPublicCacheAfterCommit(previousShareToken);

        return ResponseEntity.ok(toShareStatus(profile));
    }

    @GetMapping("/public/{shareToken}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> publicView(@PathVariable String shareToken) {
        Optional<Map<String, Object>> cachedProfile = publicResumeCacheService.get(shareToken);
        if (cachedProfile.isPresent()) {
            return ResponseEntity.ok(cachedProfile.get());
        }

        Optional<UserProfile> profileOpt = userProfileRepository.findByShareToken(shareToken);

        if (profileOpt.isEmpty() || !profileOpt.get().isPublic()) {
            return ResponseEntity.status(404).body(Map.of("error", "Resume not found or private"));
        }

        Map<String, Object> response = toPublicProfile(profileOpt.get());
        publicResumeCacheService.put(shareToken, response);
        return ResponseEntity.ok(response);
    }

    private void upgradeLegacyPasswordIfNeeded(String username, String rawPassword) {
        userRepository.findByUserName(username)
                .filter(user -> !isBcryptHash(user.getPassword()))
                .ifPresent(user -> {
                    user.setPassword(passwordEncoder.encode(rawPassword));
                    userRepository.save(user);
                });
    }

    private Map<String, Object> toPrivateProfile(UserProfile profile) {
        Map<String, Object> response = toPublicProfile(profile);
        response.put("userName", profile.getUserName());
        response.put("email", profile.getEmail());
        response.put("phone", profile.getPhone());
        response.putAll(toShareStatus(profile));
        return response;
    }

    private Map<String, Object> toPublicProfile(UserProfile profile) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("firstName", profile.getFirstName());
        response.put("lastName", profile.getLastName());
        response.put("designation", profile.getDesignation());
        response.put("summary", profile.getSummary());
        response.put("jobs", safeList(profile.getJobs()).stream().map(this::toJobResponse).collect(Collectors.toList()));
        response.put("educations", safeList(profile.getEducations()).stream().map(this::toEducationResponse).collect(Collectors.toList()));
        response.put("skills", safeList(profile.getSkills()));
        response.put("theme", profile.getTheme());
        return response;
    }

    private Map<String, Object> toShareStatus(UserProfile profile) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", profile.isPublic() ? "Share link active" : "Share link disabled");
        response.put("shareToken", profile.getShareToken());
        response.put("shareUrl", profile.getShareToken() == null ? null : "/app/public-share?token=" + profile.getShareToken());
        response.put("isPublic", profile.isPublic());
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
        response.put("responsibilities", job.getResponsibilities());
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

    private String formatIsoDate(LocalDate value) {
        return value == null ? null : value.toString();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
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

    private boolean isBcryptHash(String password) {
        return password != null
                && (password.startsWith("$2a$")
                || password.startsWith("$2b$")
                || password.startsWith("$2y$"));
    }
}
