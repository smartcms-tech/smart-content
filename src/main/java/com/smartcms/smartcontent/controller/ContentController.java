package com.smartcms.smartcontent.controller;

import com.smartcms.smartcommon.model.Content;
import com.smartcms.smartcommon.model.ContentStatus;
import com.smartcms.smartcontent.dto.*;

import com.smartcms.smartcontent.model.ContentStatusAudit;
import com.smartcms.smartcontent.model.PaginatedResponse;
import com.smartcms.smartcontent.model.RollbackField;
import com.smartcms.smartcontent.service.ContentServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/content")
@RequiredArgsConstructor
@Tag(name = "Content Management", description = "APIs for managing content lifecycle")
public class ContentController {

    private final ContentServiceImpl contentServiceImpl;

    @Operation(summary = "Create new content", description = "Creates a new content item with the provided details")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Content created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PostMapping("/create")
    public ResponseEntity<Content> createContent(
            @RequestBody @Valid ContentRequest request,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-Org-Id") String orgId){

        Content content = contentServiceImpl.createContent(request, userId, orgId);
        return ResponseEntity.status(HttpStatus.CREATED).body(content);
    }

    // Content Retrieval
    @Operation(summary = "Get content by ID", description = "Retrieves content details including versions and metadata")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Content found"),
            @ApiResponse(responseCode = "404", description = "Content not found")
    })
    @GetMapping("/{contentId}")
    public ResponseEntity<Content> getContentById(@PathVariable String contentId) {
        Content content = contentServiceImpl.getContentById(contentId);
        return ResponseEntity.ok(content);
    }

    @Operation(summary = "List content by organization", description = "Retrieves paginated list of content for an organization")
    @GetMapping("/org")
    public ResponseEntity<PaginatedResponse<Content>> listOrgContent(
            @RequestHeader("X-Org-Id") String orgId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sortBy) {

        PaginatedResponse<Content> response = contentServiceImpl.getOrgContent(orgId, page, size, sortBy);
        return ResponseEntity.ok(response);
    }

    // Content Modification
    @Operation(summary = "Update content", description = "Updates content details (title, description, body, etc.)")
    @PatchMapping("/{contentId}")
    public ResponseEntity<Content> updateContent(
            @PathVariable String contentId,
            @RequestBody @Valid ContentUpdateRequest request,
            @RequestHeader("X-User-Id") String updatedBy) {

        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        Content updatedContent = contentServiceImpl.updateContent(contentId, request, updatedBy);
        return ResponseEntity.ok(updatedContent);
    }

    @Operation(summary = "Get content by status", description = "Retrieves paginated list of content by status")
    @GetMapping("/status")
    public ResponseEntity<PaginatedResponse<Content>> listContentByStatus(
            @RequestHeader("X-Org-Id") String orgId,
            @RequestParam ContentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PaginatedResponse<Content> response = contentServiceImpl.getContentByStatus(orgId, status, page, size);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update content status", description = "Change status between DRAFT, PUBLISHED, or ARCHIVED")
    @PatchMapping("/{contentId}/status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid status transition"),
            @ApiResponse(responseCode = "404", description = "Content not found")
    })
    public ResponseEntity<Content> updateContentStatus(
            @PathVariable String contentId,
            @RequestBody @Valid ContentStatusUpdateRequest request,
            @RequestHeader("X-User-Id") String updatedBy) {

        Content updatedContent = contentServiceImpl.updateStatus(
                contentId,
                request.getNewStatus(),
                updatedBy,
                request.getNote()
        );
        return ResponseEntity.ok(updatedContent);
    }

    @Operation(summary = "Schedule content publishing", description = "Set future publish date/time")
    @PatchMapping("/{contentId}/schedule")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Content scheduled"),
            @ApiResponse(responseCode = "400", description = "Invalid schedule time"),
            @ApiResponse(responseCode = "404", description = "Content not found")
    })
    public ResponseEntity<Content> scheduleContent(
            @PathVariable String contentId,
            @RequestParam @Future Instant publishTime,
            @RequestHeader("X-User-Id") String scheduledBy) {

        Content scheduledContent = contentServiceImpl.schedulePublishing(
                contentId,
                publishTime,
                scheduledBy
        );
        return ResponseEntity.ok(scheduledContent);
    }

    @Operation(summary = "Rollback content version", description = "Restores content to a previous version (full or partial rollback)")
    @PostMapping("/{contentId}/rollback")
    public ResponseEntity<Content> rollbackContent(
            @PathVariable String contentId,
            @RequestParam int version,
            @RequestHeader("X-User-Id") String updatedBy,
            @RequestBody(required = false) Set<RollbackField> fieldsToRollback) {

        if (fieldsToRollback == null) {
            fieldsToRollback = Set.of();
        }
        Content rolledBackContent = contentServiceImpl.rollbackContent(contentId, version, updatedBy, fieldsToRollback);
        return ResponseEntity.ok(rolledBackContent);
    }


    @Operation(summary = "List content versions", description = "Retrieves version history for content")
    @GetMapping("/{contentId}/versions")
    public ResponseEntity<List<ContentVersionDto>> listContentVersions(
            @PathVariable String contentId) {
        List<ContentVersionDto> versions = contentServiceImpl.getContentVersions(contentId);
        return ResponseEntity.ok(versions);
    }

    @Operation(summary = "List bin content", description = "Retrieves paginated list of content in recycle bin")
    @GetMapping("/bin")
    public ResponseEntity<PaginatedResponse<Content>> listBinContent(
            @RequestHeader("X-Org-Id") String orgId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PaginatedResponse<Content> response = contentServiceImpl.getBinContent(orgId, page, size);
        return ResponseEntity.ok(response);
    }

    // Content Recycle Bin
    @Operation(summary = "Move content to bin", description = "Soft-deletes content (moves to recycle bin)")
    @DeleteMapping("/{contentId}")
    public ResponseEntity<Void> moveToBin(
            @PathVariable String contentId,
            @RequestHeader("X-Org-Id") String deletedByUserId) {

        contentServiceImpl.moveToBin(contentId, deletedByUserId);
        return ResponseEntity.noContent().build(); // HTTP 204
    }

    @Operation(summary = "Restore content from bin", description = "Restores content from recycle bin to active state")
    @PostMapping("/bin/{contentId}/restore")
    public ResponseEntity<Content> restoreContent(
            @PathVariable String contentId,
            @RequestHeader("X-Org-Id") String restoredByUserId) {

        Content restoredContent = contentServiceImpl.restoreContent(contentId, restoredByUserId);
        return ResponseEntity.ok(restoredContent);
    }

    @Operation(summary = "Permanently delete content", description = "Hard-deletes content from the system (irreversible)")
    @DeleteMapping("/bin/{contentId}")
    public ResponseEntity<Void> deleteContent(
            @PathVariable String contentId,
            @RequestHeader("X-Org-Id") String deletedByUserId) {
        contentServiceImpl.deleteContent(contentId);
        return ResponseEntity.noContent().build(); // HTTP 204
    }

    @Operation(summary = "Update content slug", description = "Updates the URL-friendly slug for content")
    @PatchMapping("/{contentId}/update-slug")
    public ResponseEntity<Content> updateSlug(
            @PathVariable String contentId,
            @RequestParam String newSlug,
            @RequestHeader("X-User-Id") String updatedBy) {
        Content content = contentServiceImpl.updateSlug(contentId, newSlug, updatedBy);
        return ResponseEntity.ok(content);
    }

    @Operation(summary = "Validate the slug", description = "validates if the slug is available for use")
    @GetMapping("/{contentId}/validate-slug")
    public ResponseEntity<SlugValidationResponse> validateSlug(
            @RequestParam String slug,
            @RequestHeader("X-Org-ID") String orgId,
            @PathVariable String contentId) {

        SlugValidationResponse response = contentServiceImpl.validateSlug(slug, orgId, contentId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Generate a unique slug", description = "Generates a unique slug for the content")
    @PostMapping("/{contentId}/generate-slug")
    public ResponseEntity<String> generateSlug(
            @RequestHeader("X-Org-ID") String orgId,
            @PathVariable String contentId) {
        String slug = contentServiceImpl.generateUniqueSlug(contentId, orgId);
        return ResponseEntity.ok(slug);
    }

    @Operation(summary = "Get content status audit", description = "Retrieves the status change history for a specific content item")
    @GetMapping("/audit/{contentId}")
    public List<ContentStatusAudit> getStatusAudit(@PathVariable String contentId) {
        return contentServiceImpl.getStatusAuditForContent(contentId);
    }

    // Content Relationships
//    @Operation(summary = "Link related content", description = "Creates relationship between content items")
//    @PostMapping("/{contentId}/related")
//    public ResponseEntity<Void> addRelatedContent(
//            @PathVariable String contentId,
//            @RequestBody @Valid RelatedContentRequest request,
//            @RequestHeader("X-User-Id") String linkedBy) {
//        // Implementation
//    }
//
//    @Operation(summary = "List related content", description = "Retrieves content related to specified item")
//    @GetMapping("/{contentId}/related")
//    public ResponseEntity<List<Content>> getRelatedContent(
//            @PathVariable String contentId) {
//        // Implementation
//    }

    // Content Search
//    @Operation(summary = "Search content", description = "Full-text search across content with filters")
//    @GetMapping("/search")
//    public ResponseEntity<PaginatedResponse<Content>> searchContent(
//            @RequestParam String query,
//            @RequestParam(required = false) String contentType,
//            @RequestParam(required = false) String status,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "20") int size) {
//        // Implementation
//    }
}
