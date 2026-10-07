package com.getlinkdtd.link.repository;

import com.getlinkdtd.link.domain.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {
    Optional<Profile> findByUserId(UUID userId);
    Optional<Profile> findByUsernameIgnoreCase(String username);
    boolean existsByUsernameIgnoreCase(String username);
}
