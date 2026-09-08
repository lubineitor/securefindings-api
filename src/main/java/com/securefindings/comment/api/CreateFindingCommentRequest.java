package com.securefindings.comment.api;

import com.securefindings.comment.domain.FindingComment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateFindingCommentRequest(
        @NotBlank(message = "El contenido es obligatorio") @Size(max = FindingComment.MAX_CONTENT_LENGTH, message = "El contenido no puede superar los 5000 caracteres") String content) {
}