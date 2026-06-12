package com.daemonsets.resumeportal;

import com.daemonsets.resumeportal.models.User;
import com.daemonsets.resumeportal.models.UserProfile;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Map;

@Slf4j
@Service
public class AuthService {
    private final PasswordEncoder passwordEncoder;
    private final UserProfileRepository userProfileRepository;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;

    public AuthService(
            PasswordEncoder passwordEncoder,
            UserProfileRepository userProfileRepository,
            UserRepository userRepository,
            AuthenticationManager authenticationManager
    ) {
        this.passwordEncoder = passwordEncoder;
        this.userProfileRepository = userProfileRepository;
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
    }

    public Map<String, Object> login(Map<String, String> credentials, HttpServletRequest request) {
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

            return Map.of(
                    "message", "Login successful",
                    "username", username
            );
        } catch (AuthenticationException exception) {
            log.warn("Login failed for user: {}. Reason: {}", username, exception.getMessage());
            throw exception;
        } catch (RuntimeException exception) {
            log.warn("Login failed for user: {}. Reason: {}", username, exception.getMessage());
            throw new BadCredentialsException("Invalid credentials", exception);
        }
    }

    @Transactional
    public void register(Map<String, String> userData) {
        String userName = userData.getOrDefault("username", "").trim();
        String password = userData.get("password");
        String confirmPassword = userData.get("confirmPassword");

        validateRegistration(userName, password, confirmPassword);

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
    }

    private void validateRegistration(String userName, String password, String confirmPassword) {
        if (userName.isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Passwords do not match");
        }
        if (userRepository.findByUserName(userName).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }
    }

    private void upgradeLegacyPasswordIfNeeded(String username, String rawPassword) {
        userRepository.findByUserName(username)
                .filter(user -> !isBcryptHash(user.getPassword()))
                .ifPresent(user -> {
                    user.setPassword(passwordEncoder.encode(rawPassword));
                    userRepository.save(user);
                });
    }

    private boolean isBcryptHash(String password) {
        return password != null
                && (password.startsWith("$2a$")
                || password.startsWith("$2b$")
                || password.startsWith("$2y$"));
    }
}
