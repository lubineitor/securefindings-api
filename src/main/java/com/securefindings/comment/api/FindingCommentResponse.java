package com.securefindings.comment.api;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.securefindings.comment.domain.FindingComment;

public record FindingCommentResponse(
        @JsonProperty("id") UUID id,

        @JsonProperty("findingId") UUID findingId,

        @JsonProperty("author") String author,

        @JsonProperty("content") String content,

        @JsonProperty("createdAt") Instant createdAt) {

    public static FindingCommentResponse from(
            FindingComment comment) {

        return new FindingCommentResponse(
                comment.id(),
                comment.findingId(),
                comment.author(),
                comment.content(),
                comment.createdAt());
    }
}