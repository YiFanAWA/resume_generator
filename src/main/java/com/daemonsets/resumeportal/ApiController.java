package com.daemonsets.resumeportal;


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
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.springframework.util.ClassUtils.isPresent;
@Slf4j
@RestController
@RequestMapping("/api")
public class ApiController {

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthenticationManager authenticationManager;

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody Map<String , String> credentials, HttpServletRequest  request){
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            credentials.get("username"),
                            credentials.get("password")
                    )
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Login successful");
            response.put("username", credentials.get("username"));
            return ResponseEntity.ok(response);
        } catch (Exception e){
            log.error("Login failed for user: {}. Reason: {}", credentials.get("username"), e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid credentials");

            return ResponseEntity.status(401).body(error);
        }
    }
    @PostMapping("/auth/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> userData){
        String userName = userData.get("username");
        String password = userData.get("password");
        String confirmPassword = userData.get("confirmPassword");

        if (userName == null || userName.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "UserName cannot be empty"));
        }
        if (password == null || password.length() <4) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 4 characters long"));
        }
        if (!password.equals(confirmPassword)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Passwords do not match"));
        }
        if (userRepository.findByUserName(userName).isPresent()){
            return ResponseEntity.badRequest().body(Map.of("error", "Username already exists"));
        }
//当前数据持久化 使用的是明文密码 有风险（可修改）
        User newUser = new User();
        newUser.setUserName(userName);
        newUser.setPassword(password);
        newUser.setActive(true);
        newUser.setRoles("USER");
        userRepository.save(newUser);

        UserProfile userProfile = new UserProfile();
        userProfile.setUserName(userName);
        userProfile.setTheme(1);
        userProfile.setJobs(new java.util.ArrayList<>());
        userProfile.setEducations(new java.util.ArrayList<>());
        userProfile.setSkills(new java.util.ArrayList<>());
        userProfileRepository.save(userProfile);

        return ResponseEntity.ok(Map.of("message", "Registration successful"));
    }


    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Principal  principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));
        }
        String userName = principal.getName();
        Optional<UserProfile> userProfileOptional = userProfileRepository.findByUserName(userName);

        if (userProfileOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(userProfileOptional.get());
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody UserProfile userProfile, Principal  principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));

        }
        String userName = principal.getName();
        Optional<UserProfile> existingProfileOpt = userProfileRepository.findByUserName(userName);

        if (existingProfileOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        UserProfile existingProfile = existingProfileOpt.get();
        existingProfile.setFirstName(userProfile.getFirstName());
        existingProfile.setLastName(userProfile.getLastName());
        existingProfile.setEmail(userProfile.getEmail());
        existingProfile.setPhone(userProfile.getPhone());
        existingProfile.setDesignation(userProfile.getDesignation());
        existingProfile.setSummary(userProfile.getSummary());
        existingProfile.setTheme(userProfile.getTheme());

        if (userProfile.getJobs() != null) {
            existingProfile.getJobs().clear();
            existingProfile.getJobs().addAll(userProfile.getJobs());
        }
        if (userProfile.getEducations() != null) {
            existingProfile.getEducations().clear();
            existingProfile.getEducations().addAll(userProfile.getEducations());
        }
        if (userProfile.getSkills() != null) {
            existingProfile.getSkills().clear();
            existingProfile.getSkills().addAll(userProfile.getSkills());
        }
        userProfileRepository.save(existingProfile);
        return ResponseEntity.ok(Map.of("message", "profile updated successfully"));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        request.getSession().invalidate();
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of("message", "Logout successful"));
    }

    @Autowired
    private PdfExportService pdfExportService;

    @GetMapping("/profile/export/pdf")
    public ResponseEntity<byte[]> exportPdf(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated").toString().getBytes());
        }

        String userName = principal.getName();
        Optional<UserProfile> userProfileOptional = userProfileRepository.findByUserName(userName);

        if (userProfileOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        UserProfile userProfile = userProfileOptional.get();

        try {
            byte[] pdfBytes = pdfExportService.generatePdf(userProfile);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment",
                    userProfile.getFirstName() + "_" + userProfile.getLastName() + "_Resume.pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Failed to generate PDF").toString().getBytes());
        }
    }

    @PostMapping("/profile/share/generate")
    public ResponseEntity<?> generateShareToken(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        String userName = principal.getName();
        Optional<UserProfile> profileOpt = userProfileRepository.findByUserName(userName);

        if (profileOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        UserProfile profile = profileOpt.get();
        profile.generateShareToken();
        profile.setPublic(true);
        userProfileRepository.save(profile);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Share link generated");
        response.put("shareToken", profile.getShareToken());
        response.put("shareUrl", "/public-share.html?token=" + profile.getShareToken());
        response.put("isPublic", String.valueOf(profile.isPublic()));

        return ResponseEntity.ok(response);
    }



    @PostMapping("/profile/share/revoke")
    public ResponseEntity<?> revokeShareToken(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        String userName = principal.getName();
        Optional<UserProfile> profileOpt = userProfileRepository.findByUserName(userName);

        if (profileOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        UserProfile profile = profileOpt.get();
        profile.revokeShareToken();
        userProfileRepository.save(profile);

        return ResponseEntity.ok(Map.of("message", "Share link revoked"));
    }

    @GetMapping("/public/{shareToken}")
    public ResponseEntity<?> publicView(@PathVariable String shareToken) {
        Optional<UserProfile> profileOpt = userProfileRepository.findByShareToken(shareToken);

        if (profileOpt.isEmpty() || !profileOpt.get().isPublic()) {
            return ResponseEntity.status(404).body(Map.of("error", "Resume not found or private"));
        }

        UserProfile profile = profileOpt.get();

        // 只返回公开信息，过滤敏感数据
        Map<String, Object> publicProfile = new HashMap<>();
        publicProfile.put("firstName", profile.getFirstName());
        publicProfile.put("lastName", profile.getLastName());
        publicProfile.put("designation", profile.getDesignation());
        publicProfile.put("summary", profile.getSummary());
        publicProfile.put("jobs", profile.getJobs());
        publicProfile.put("educations", profile.getEducations());
        publicProfile.put("skills", profile.getSkills());
        publicProfile.put("theme", profile.getTheme());
        // 不返回 email、phone 等敏感信息

        return ResponseEntity.ok(publicProfile);
    }

}


