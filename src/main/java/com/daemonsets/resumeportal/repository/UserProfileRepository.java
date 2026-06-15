package com.daemonsets.resumeportal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.daemonsets.resumeportal.model.UserProfile;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Integer> {
    Optional<UserProfile> findByUserName(String userName);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select profile from UserProfile profile where profile.userName = :userName")
    Optional<UserProfile> findByUserNameForUpdate(@Param("userName") String userName);

    Optional<UserProfile> findByShareToken(String shareToken);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select profile from UserProfile profile where profile.shareToken = :shareToken")
    Optional<UserProfile> findByShareTokenForUpdate(@Param("shareToken") String shareToken);

}
