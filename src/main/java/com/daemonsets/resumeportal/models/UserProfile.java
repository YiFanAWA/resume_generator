package com.daemonsets.resumeportal.models;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table
public class UserProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @Column(nullable = false, unique = true, length = 100)
    private String userName;

    private int theme;

    private String summary;

    private String firstName;

    private String lastName;

    private String email;

    private String phone;

    private String designation;

    @Column(name = "is_public")
    private boolean isPublic = false;

    @Column(name = "share_token", unique = true, length = 64)
    private String shareToken;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "user_profile_id")
    @Builder.Default
    List<Job> jobs = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "user_profile_id")
    @Builder.Default
    List<Education> educations = new ArrayList<>();

    @ElementCollection(targetClass=String.class)
    @Builder.Default
    List<String> skills = new ArrayList<>();

    public void generateShareToken() {
        if (this.shareToken == null || this.shareToken.isEmpty()) {
            this.shareToken = UUID.randomUUID().toString();
        }
    }

    public void revokeShareToken() {
        this.shareToken = null;
        this.isPublic = false;
    }
}
