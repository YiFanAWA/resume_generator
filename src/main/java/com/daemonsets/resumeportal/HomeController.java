package com.daemonsets.resumeportal;

import com.daemonsets.resumeportal.models.User;
import com.lowagie.text.DocumentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.daemonsets.resumeportal.models.Education;
import com.daemonsets.resumeportal.models.Job;
import com.daemonsets.resumeportal.models.UserProfile;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Controller
public class HomeController {

    @Autowired
    UserProfileRepository userProfileRepository;

    @Autowired
    UserRepository userRepository;
    @Autowired
    PdfExportService pdfExportService;
    @GetMapping("/")
    public String home() {
        return "index";
    }
    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@RequestParam String userName,
                               @RequestParam String password,
                               @RequestParam String confirmPassword,
                               Model model) {

        if (userName == null || userName.trim().isEmpty()) {
            model.addAttribute("error", "Username cannot be empty");
            return "register";
        }

        if (password == null || password.length() < 4) {
            model.addAttribute("error", "Password must be at least 4 characters long");
            return "register";
        }

        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match");
            return "register";
        }

        Optional<User> existingUser = userRepository.findByUserName(userName);
        if (existingUser.isPresent()) {
            model.addAttribute("error", "Username already exists");
            return "register";
        }

        User newUser = new User();
        newUser.setUserName(userName);
        newUser.setPassword(password);
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

        model.addAttribute("success", "Registration successful! Please login.");
        return "register";
    }
    @GetMapping("/edit")
    public String edit(Model model, Principal principal, @RequestParam(required = false) String add) {
        String userId = principal.getName();
        model.addAttribute("userId", userId);
        Optional<UserProfile> userProfileOptional = userProfileRepository.findByUserName(userId);
        userProfileOptional.orElseThrow(() -> new RuntimeException("Not found: " + userId));
        UserProfile userProfile = userProfileOptional.get();
        if ("job".equals(add)) {
            userProfile.getJobs().add(new Job());
        } else if ("education".equals(add)) {
            userProfile.getEducations().add(new Education());
        } else if ("skill".equals(add)) {
            userProfile.getSkills().add("");
        }

        model.addAttribute("userProfile", userProfile);
        return "profile-edit";
    }

    @GetMapping("/delete")
    public String delete(Model model, Principal principal, @RequestParam String type, @RequestParam int index) {
        String userId = principal.getName();
        Optional<UserProfile> userProfileOptional = userProfileRepository.findByUserName(userId);
        userProfileOptional.orElseThrow(() -> new RuntimeException("Not found: " + userId));
        UserProfile userProfile = userProfileOptional.get();
        if ("job".equals(type)) {
            userProfile.getJobs().remove(index);
        } else if ("education".equals(type)) {
            userProfile.getEducations().remove(index);
        } else if ("skill".equals(type)) {
            userProfile.getSkills().remove(index);
        }
        userProfileRepository.save(userProfile);
        return "redirect:/edit";
    }

    @PostMapping("/edit")
    public String postEdit(Model model, Principal principal, @ModelAttribute UserProfile userProfile) {
        String userName = principal.getName();
        Optional<UserProfile> userProfileOptional = userProfileRepository.findByUserName(userName);
        userProfileOptional.orElseThrow(() -> new RuntimeException("Not found: " + userName));
        UserProfile savedUserProfile = userProfileOptional.get();
        userProfile.setId(savedUserProfile.getId());
        userProfile.setUserName(userName);
        userProfileRepository.save(userProfile);
        return "redirect:/view/" + userName;
    }

    @GetMapping("/view/{userId}")
    public String view(Principal principal, @PathVariable String userId, Model model) {
        if (principal != null && principal.getName() != "") {
            boolean currentUsersProfile = principal.getName().equals(userId);
            model.addAttribute("currentUsersProfile", currentUsersProfile);
        }
        String userName = principal.getName();
        Optional<UserProfile> userProfileOptional = userProfileRepository.findByUserName(userId);
        userProfileOptional.orElseThrow(() -> new RuntimeException("Not found: " + userId));

        model.addAttribute("userId", userId);
        UserProfile userProfile = userProfileOptional.get();
        model.addAttribute("userProfile", userProfile);
        System.out.println(userProfile.getJobs());

        return "profile-templates/" + userProfile.getTheme() + "/index";
    }

    @GetMapping("/export/pdf/{userId}")
    public ResponseEntity<byte[]> exportPdf(@PathVariable String userId) {
        Optional<UserProfile> userProfileOptional = userProfileRepository.findByUserName(userId);
        userProfileOptional.orElseThrow(() -> new RuntimeException("Not found: " + userId));

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

        } catch (DocumentException | IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}
