package com.daemonsets.resumeportal.web;

import com.daemonsets.resumeportal.model.UserProfile;
import com.daemonsets.resumeportal.pdf.PdfExportResult;
import com.daemonsets.resumeportal.repository.UserProfileRepository;
import com.daemonsets.resumeportal.service.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.security.Principal;

@Controller
public class HomeController {

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private ProfileService profileService;

    @GetMapping("/")
    public String home() {
        return "redirect:/app/";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "redirect:/app/register";
    }

    @GetMapping("/edit")
    public String editPage(Principal principal) {
        if (principal == null) {
            return "redirect:/app/login";
        }
        return "redirect:/app/resume";
    }

    @GetMapping({"/login.html", "/register.html", "/resume.html"})
    public String legacyStaticPages() {
        return "redirect:/app/";
    }

    @GetMapping("/public-share.html")
    public String publicSharePage(@RequestParam(required = false) String token) {
        if (token == null || token.isBlank()) {
            return "redirect:/app/public-share";
        }
        return "redirect:/app/public-share?token=" + token;
    }

    @GetMapping({"/app", "/app/", "/app/login", "/app/register", "/app/resume", "/app/public-share"})
    public String reactApp() {
        return "forward:/app/index.html";
    }

    @GetMapping("/view/{userId}")
    public String view(Principal principal, @PathVariable String userId, Model model) {
        assertOwner(principal, userId);

        UserProfile userProfile = userProfileRepository.findByUserName(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));

        model.addAttribute("currentUsersProfile", true);
        model.addAttribute("userId", userId);
        model.addAttribute("userProfile", userProfile);

        return "profile-templates/" + userProfile.getTheme() + "/index";
    }

    @GetMapping("/export/pdf/{userId}")
    public ResponseEntity<byte[]> exportPdf(Principal principal, @PathVariable String userId) {
        assertOwner(principal, userId);

        try {
            PdfExportResult result = profileService.exportPdf(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", result.filename());

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(result.content());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate PDF", e);
        }
    }

    private void assertOwner(Principal principal, String userId) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please login first");
        }
        if (!principal.getName().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot access another user's private resume");
        }
    }
}
