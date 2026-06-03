package com.daemonsets.resumeportal;

import com.daemonsets.resumeportal.models.UserProfile;
import com.lowagie.text.DocumentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.security.Principal;

@Controller
public class HomeController {

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private PdfExportService pdfExportService;

    @GetMapping("/")
    public String home() {
        return "redirect:/login.html";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "redirect:/register.html";
    }

    @GetMapping("/edit")
    public String editPage(Principal principal) {
        if (principal == null) {
            return "redirect:/login.html";
        }
        return "redirect:/resume.html";
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

        UserProfile userProfile = userProfileRepository.findByUserName(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));

        try {
            byte[] pdfBytes = pdfExportService.generatePdf(userProfile);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", userId + "_resume.pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);

        } catch (DocumentException | IOException e) {
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