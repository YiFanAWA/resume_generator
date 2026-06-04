package com.daemonsets.resumeportal;

import com.daemonsets.resumeportal.models.User;
import com.daemonsets.resumeportal.models.UserProfile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ResumePortalApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        userProfileRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void registerCreatesEncodedPasswordAndProfile() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"password123\",\"confirmPassword\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Registration successful"));

        User savedUser = userRepository.findByUserName("alice").orElseThrow();
        assertThat(savedUser.getPassword()).isNotEqualTo("password123");
        assertThat(passwordEncoder.matches("password123", savedUser.getPassword())).isTrue();
        assertThat(userProfileRepository.findByUserName("alice")).isPresent();
    }

    @Test
    void profileRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "alice")
    void authenticatedUserCanUpdateOwnProfile() throws Exception {
        createUserAndProfile("alice");

        mockMvc.perform(put("/api/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Alice\",\"lastName\":\"Wang\",\"theme\":2,\"skills\":[\"Java\",\"React\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Profile updated successfully"));

        mockMvc.perform(get("/api/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Alice"))
                .andExpect(jsonPath("$.skills[0]").value("Java"))
                .andExpect(jsonPath("$.isPublic").value(false));
    }

    @Test
    @WithMockUser(username = "alice")
    void shareLinkCanBeGeneratedAndRevoked() throws Exception {
        createUserAndProfile("alice");

        String generateResponse = mockMvc.perform(post("/api/profile/share/generate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isPublic").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(generateResponse);
        String shareToken = json.get("shareToken").asText();

        mockMvc.perform(get("/api/public/" + shareToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Alice"))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.phone").doesNotExist());

        mockMvc.perform(post("/api/profile/share/revoke"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isPublic").value(false));

        mockMvc.perform(get("/api/public/" + shareToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "alice")
    void privatePreviewRejectsOtherUsers() throws Exception {
        createUserAndProfile("alice");
        createUserAndProfile("bob");

        mockMvc.perform(get("/view/bob"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "alice")
    void authenticatedUserCanExportTemplatePdf() throws Exception {
        createUserAndProfile("alice");

        byte[] pdf = mockMvc.perform(get("/api/profile/export/pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        String fileHeader = new String(Arrays.copyOf(pdf, 4), StandardCharsets.US_ASCII);
        assertThat(fileHeader).isEqualTo("%PDF");
    }

    private void createUserAndProfile(String username) {
        User user = new User();
        user.setUserName(username);
        user.setPassword(passwordEncoder.encode("password123"));
        user.setActive(true);
        user.setRoles("USER");
        userRepository.save(user);

        UserProfile profile = new UserProfile();
        profile.setUserName(username);
        profile.setFirstName(capitalize(username));
        profile.setLastName("User");
        profile.setEmail(username + "@example.com");
        profile.setPhone("1234567890");
        profile.setTheme(1);
        profile.setJobs(new ArrayList<>());
        profile.setEducations(new ArrayList<>());
        profile.setSkills(new ArrayList<>());
        userProfileRepository.save(profile);
    }

    private String capitalize(String value) {
        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }
}
