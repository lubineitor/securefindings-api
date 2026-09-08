package com.securefindings.comment.api;

import java.util.List;

import org.springframework.data.domain.Page;

import com.securefindings.comment.domain.FindingComment;

public record FindingCommentPageResponse(
        List<FindingCommentResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {

    public static FindingCommentPageResponse from(
            Page<FindingComment> commentPage) {

        List<FindingCommentResponse> content = commentPage
                .getContent()
                .stream()
                .map(FindingCommentResponse::from)
                .toList();

        return new FindingCommentPageResponse(
                content,
                commentPage.getNumber(),
                commentPage.getSize(),
                commentPage.getTotalElements(),
                commentPage.getTotalPages(),
                commentPage.isFirst(),
                commentPage.isLast());
    }
}