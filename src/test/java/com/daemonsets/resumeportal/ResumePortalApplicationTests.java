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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
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

    @MockBean
    private PublicResumeCacheService publicResumeCacheService;

    @BeforeEach
    void cleanDatabase() {
        reset(publicResumeCacheService);
        when(publicResumeCacheService.get(anyString())).thenReturn(Optional.empty());
        userProfileRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void registerCreatesEncodedPasswordAndProfile() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
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
    void logoutReturnsJsonAndClearsSessionCookies() throws Exception {
        mockMvc.perform(post("/api/auth/logout").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Logout successful"))
                .andExpect(cookie().maxAge("JSESSIONID", 0))
                .andExpect(cookie().maxAge("XSRF-TOKEN", 0));
    }

    @Test
    @WithMockUser(username = "alice")
    void authenticatedUserCanUpdateOwnProfile() throws Exception {
        createUserAndProfile("alice");

        mockMvc.perform(put("/api/profile")
                        .with(csrf())
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

        String generateResponse = mockMvc.perform(post("/api/profile/share/generate").with(csrf()))
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
        verify(publicResumeCacheService).put(eq(shareToken), anyMap());

        clearInvocations(publicResumeCacheService);
        mockMvc.perform(post("/api/profile/share/revoke").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isPublic").value(false));
        verify(publicResumeCacheService, atLeastOnce()).evict(shareToken);

        mockMvc.perform(get("/api/public/" + shareToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void publicResumeCanBeServedFromCache() throws Exception {
        UserProfile profile = createUserAndProfile("cached");
        profile.setPublic(true);
        profile.setShareToken("cached-token");
        userProfileRepository.save(profile);

        Map<String, Object> cachedResume = new LinkedHashMap<>();
        cachedResume.put("firstName", "Cached");
        cachedResume.put("lastName", "Resume");
        cachedResume.put("designation", "Engineer");
        cachedResume.put("summary", "Loaded from cache");
        cachedResume.put("jobs", List.of());
        cachedResume.put("educations", List.of());
        cachedResume.put("skills", List.of("Redis"));
        cachedResume.put("theme", 1);
        when(publicResumeCacheService.get("cached-token")).thenReturn(Optional.of(cachedResume));

        mockMvc.perform(get("/api/public/cached-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Cached"))
                .andExpect(jsonPath("$.skills[0]").value("Redis"));

        verify(publicResumeCacheService, never()).put(anyString(), anyMap());
        assertThat(userProfileRepository.findByShareToken("cached-token").orElseThrow().getShareViewCount()).isEqualTo(1);
    }

    @Test
    @WithMockUser(username = "alice")
    void updatingPublicProfileEvictsCachedPublicResume() throws Exception {
        createUserAndProfile("alice");
        String generateResponse = mockMvc.perform(post("/api/profile/share/generate").with(csrf()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String shareToken = objectMapper.readTree(generateResponse).get("shareToken").asText();

        clearInvocations(publicResumeCacheService);
        mockMvc.perform(put("/api/profile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Alice\",\"lastName\":\"Updated\",\"theme\":2,\"skills\":[\"Java\",\"Redis\"]}"))
                .andExpect(status().isOk());

        verify(publicResumeCacheService, atLeastOnce()).evict(shareToken);
    }

    @Test
    @WithMockUser(username = "alice")
    void passwordProtectedShareRequiresCorrectPassword() throws Exception {
        createUserAndProfile("alice");
        String generateResponse = mockMvc.perform(post("/api/profile/share/generate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sharePasswordProtected").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String shareToken = objectMapper.readTree(generateResponse).get("shareToken").asText();

        mockMvc.perform(get("/api/public/" + shareToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.requiresPassword").value(true));

        mockMvc.perform(post("/api/public/" + shareToken + "/access")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.requiresPassword").value(true));

        mockMvc.perform(post("/api/public/" + shareToken + "/access")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Alice"))
                .andExpect(jsonPath("$.email").doesNotExist());

        UserProfile profile = userProfileRepository.findByShareToken(shareToken).orElseThrow();
        assertThat(profile.getShareViewCount()).isEqualTo(1);
        assertThat(profile.getShareLastViewedAt()).isNotNull();
    }

    @Test
    @WithMockUser(username = "alice")
    void shareViewLimitIsEnforced() throws Exception {
        createUserAndProfile("alice");
        String generateResponse = mockMvc.perform(post("/api/profile/share/generate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"maxViews\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shareMaxViews").value(1))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String shareToken = objectMapper.readTree(generateResponse).get("shareToken").asText();

        mockMvc.perform(get("/api/public/" + shareToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/public/" + shareToken))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.error").value("Share link view limit has been reached"));
    }

    @Test
    @WithMockUser(username = "alice")
    void expiredShareLinkIsRejected() throws Exception {
        createUserAndProfile("alice");
        String expiredAt = LocalDateTime.now().minusMinutes(1).withNano(0).toString();
        String generateResponse = mockMvc.perform(post("/api/profile/share/generate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expiresAt\":\"" + expiredAt + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shareExpired").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String shareToken = objectMapper.readTree(generateResponse).get("shareToken").asText();

        mockMvc.perform(get("/api/public/" + shareToken))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.error").value("Share link has expired"));
    }

    @Test
    @WithMockUser(username = "alice")
    void shareSettingsCanBeUpdatedAndPasswordCleared() throws Exception {
        createUserAndProfile("alice");
        String generateResponse = mockMvc.perform(post("/api/profile/share/generate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"secret123\",\"maxViews\":5}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String shareToken = objectMapper.readTree(generateResponse).get("shareToken").asText();

        mockMvc.perform(put("/api/profile/share/settings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clearPassword\":true,\"maxViews\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sharePasswordProtected").value(false))
                .andExpect(jsonPath("$.shareMaxViews").doesNotExist());

        mockMvc.perform(get("/api/public/" + shareToken))
                .andExpect(status().isOk());
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

        for (int theme = 1; theme <= 3; theme++) {
            UserProfile profile = userProfileRepository.findByUserName("alice").orElseThrow();
            profile.setTheme(theme);
            userProfileRepository.save(profile);

            byte[] pdf = mockMvc.perform(get("/api/profile/export/pdf"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                    .andReturn()
                    .getResponse()
                    .getContentAsByteArray();

            String fileHeader = new String(Arrays.copyOf(pdf, 4), StandardCharsets.US_ASCII);
            assertThat(fileHeader).isEqualTo("%PDF");
        }
    }

    private UserProfile createUserAndProfile(String username) {
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
        return userProfileRepository.save(profile);
    }

    private String capitalize(String value) {
        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }
}
