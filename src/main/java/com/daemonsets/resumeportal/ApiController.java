package com.daemonsets.resumeportal;

import com.daemonsets.resumeportal.models.UserProfile;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
public class ApiController {

    private final AuthService authService;
    private final ProfileService profileService;
    private final ShareService shareService;

    public ApiController(AuthService authService, ProfileService profileService, ShareService shareService) {
        this.authService = authService;
        this.profileService = profileService;
        this.shareService = shareService;
    }

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials, HttpServletRequest request) {
        try {
            return ResponseEntity.ok(authService.login(credentials, request));
        } catch (AuthenticationException exception) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }
    }

    @GetMapping("/csrf")
    public ResponseEntity<?> csrf(CsrfToken csrfToken) {
        return ResponseEntity.ok(Map.of(
                "token", csrfToken.getToken(),
                "headerName", csrfToken.getHeaderName(),
                "parameterName", csrfToken.getParameterName()
        ));
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
        try {
            authService.register(userData);
            return ResponseEntity.ok(Map.of("message", "Registration successful"));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));
        }

        return profileService.getPrivateProfile(principal.getName())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody UserProfile userProfile, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        if (!profileService.updateProfile(principal.getName(), userProfile)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("message", "Profile updated successfully"));
    }

    @GetMapping("/profile/export/pdf")
    public ResponseEntity<byte[]> exportPdf(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Not authenticated".getBytes());
        }

        try {
            return profileService.exportPdf(principal.getName())
                    .map(result -> ResponseEntity.ok()
                            .headers(pdfHeaders(result.filename()))
                            .body(result.content()))
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("Failed to generate PDF for user {}", principal.getName(), exception);
            return ResponseEntity.status(500).body("Failed to generate PDF".getBytes());
        }
    }

    @GetMapping("/profile/share")
    public ResponseEntity<?> getShareStatus(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        return shareService.getShareStatus(principal.getName())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/profile/share/generate")
    public ResponseEntity<?> generateShareToken(@RequestBody(required = false) Map<String, Object> shareSettings, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        return shareService.generateShareToken(principal.getName(), shareSettings)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/profile/share/settings")
    public ResponseEntity<?> updateShareSettings(@RequestBody(required = false) Map<String, Object> shareSettings, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        try {
            return shareService.updateShareSettings(principal.getName(), shareSettings)
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (IllegalStateException exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        }
    }

    @PostMapping("/profile/share/revoke")
    public ResponseEntity<?> revokeShareToken(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        return shareService.revokeShareToken(principal.getName())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/public/{shareToken}")
    public ResponseEntity<?> publicView(@PathVariable String shareToken) {
        ShareService.PublicResumeResponse response = shareService.resolvePublicView(shareToken, null);
        return ResponseEntity.status(response.status()).body(response.body());
    }

    @PostMapping("/public/{shareToken}/access")
    public ResponseEntity<?> publicViewWithPassword(
            @PathVariable String shareToken,
            @RequestBody(required = false) Map<String, String> accessData
    ) {
        String password = accessData == null ? null : accessData.get("password");
        ShareService.PublicResumeResponse response = shareService.resolvePublicView(shareToken, password);
        return ResponseEntity.status(response.status()).body(response.body());
    }

    private HttpHeaders pdfHeaders(String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        return headers;
    }
}
