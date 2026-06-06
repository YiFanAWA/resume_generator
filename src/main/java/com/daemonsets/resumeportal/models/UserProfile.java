package com.daemonsets.resumeportal.models;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import javax.persistence.*;
import java.time.LocalDateTime;
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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    @Column(name = "share_password_hash", length = 100)
    private String sharePasswordHash;

    @Column(name = "share_expires_at")
    private LocalDateTime shareExpiresAt;

    @Column(name = "share_max_views")
    private Integer shareMaxViews;

    @Column(name = "share_view_count")
    private long shareViewCount = 0;

    @Column(name = "share_last_viewed_at")
    private LocalDateTime shareLastViewedAt;

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
        this.sharePasswordHash = null;
        this.shareExpiresAt = null;
        this.shareMaxViews = null;
        this.shareViewCount = 0;
        this.shareLastViewedAt = null;
    }

    public boolean hasSharePassword() {
        return this.sharePasswordHash != null && !this.sharePasswordHash.isBlank();
    }

    public boolean isShareExpired() {
        return this.shareExpiresAt != null && !this.shareExpiresAt.isAfter(LocalDateTime.now());
    }

    public boolean isShareViewLimitReached() {
        return this.shareMaxViews != null && this.shareMaxViews > 0 && this.shareViewCount >= this.shareMaxViews;
    }

    public void recordShareView() {
        this.shareViewCount++;
        this.shareLastViewedAt = LocalDateTime.now();
    }
}
