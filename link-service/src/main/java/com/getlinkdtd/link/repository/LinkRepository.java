package com.getlinkdtd.link.repository;

import com.getlinkdtd.link.domain.Link;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LinkRepository extends JpaRepository<Link, UUID> {
    List<Link> findByProfileIdOrderByPositionAsc(UUID profileId);
    List<Link> findByProfileIdAndEnabledTrueOrderByPositionAsc(UUID profileId);
    Optional<Link> findByIdAndProfileUserId(UUID id, UUID userId);
    Optional<Link> findByIdAndEnabledTrue(UUID id);
    long countByProfileId(UUID profileId);
}
