package com.securefindings.comment.persistence;

import java.time.Instant;
import java.util.UUID;

import com.securefindings.comment.domain.FindingComment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "finding_comments")
public class FindingCommentEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "finding_id", nullable = false, updatable = false)
    private UUID findingId;

    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

    @Column(name = "author", nullable = false, length = 255, updatable = false)
    private String author;

    @Column(name = "content", nullable = false, length = 5000, updatable = false)
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected FindingCommentEntity() {
    }

    public FindingCommentEntity(FindingComment comment) {
        this.id = comment.id();
        this.findingId = comment.findingId();
        this.organizationId = comment.organizationId();
        this.author = comment.author();
        this.content = comment.content();
        this.createdAt = comment.createdAt();
    }

    public FindingComment toDomain() {
        return new FindingComment(
                id,
                findingId,
                organizationId,
                author,
                content,
                createdAt);
    }

    public UUID getId() {
        return id;
    }

    public UUID getFindingId() {
        return findingId;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public String getAuthor() {
        return author;
    }

    public String getContent() {
        return content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}