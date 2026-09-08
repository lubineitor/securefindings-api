package com.securefindings.comment.api;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.securefindings.comment.application.FindingCommentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Validated
@RestController
@RequestMapping("/api/v1/findings/{findingId}/comments")
@Tag(name = "Comentarios", description = "Comentarios asociados a los hallazgos")
@SecurityRequirement(name = "bearerAuth")
public class FindingCommentController {

    private final FindingCommentService commentService;

    public FindingCommentController(
            FindingCommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear un comentario", description = "Añade un comentario al hallazgo indicado")
    public FindingCommentResponse create(
            @PathVariable("findingId") UUID findingId,
            @Valid @RequestBody CreateFindingCommentRequest request) {

        return FindingCommentResponse.from(
                commentService.create(
                        findingId,
                        request.content()));
    }

    @GetMapping
    @Operation(summary = "Consultar comentarios", description = "Devuelve los comentarios paginados y ordenados "
            + "cronológicamente")
    public FindingCommentPageResponse findPage(
            @PathVariable("findingId") UUID findingId,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) int size) {

        return FindingCommentPageResponse.from(
                commentService.findPageByFindingId(
                        findingId,
                        page,
                        size));
    }
}