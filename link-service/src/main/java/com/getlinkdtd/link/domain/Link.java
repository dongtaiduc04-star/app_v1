package com.getlinkdtd.link.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "links")
public class Link {
    @Id
    @UuidGenerator
    @Column(name = "id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;

    @Column(name = "title", nullable = false, length = 120)
    private String title;

    @Column(name = "destination_url", nullable = false, length = 2048)
    private String destinationUrl;

    @Column(name = "icon", length = 100)
    private String icon;

    @Column(name = "position", nullable = false, columnDefinition = "SMALLINT UNSIGNED")
    private int position;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "click_count", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private long clickCount;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Link() {
    }

    public Link(Profile profile, String title, String destinationUrl, int position) {
        this.profile = profile;
        this.title = title;
        this.destinationUrl = destinationUrl;
        this.position = position;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void update(String title, String destinationUrl, String icon, boolean enabled) {
        this.title = title;
        this.destinationUrl = destinationUrl;
        this.icon = icon;
        this.enabled = enabled;
        this.updatedAt = Instant.now();
    }

    public void moveTo(int position) {
        this.position = position;
        this.updatedAt = Instant.now();
    }

    public void recordClick() {
        this.clickCount++;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public Profile getProfile() { return profile; }
    public String getTitle() { return title; }
    public String getDestinationUrl() { return destinationUrl; }
    public String getIcon() { return icon; }
    public int getPosition() { return position; }
    public boolean isEnabled() { return enabled; }
    public long getClickCount() { return clickCount; }
    public long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
