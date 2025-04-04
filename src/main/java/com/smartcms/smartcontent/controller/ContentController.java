package com.smartcms.smartcontent.controller;

import com.smartcms.smartcommon.model.Content;
import com.smartcms.smartcommon.model.ContentStatus;
import com.smartcms.smartcontent.dto.ContentRequest;
import com.smartcms.smartcontent.dto.ContentUpdateRequest;

import com.smartcms.smartcontent.dto.ContentVersionDto;
import com.smartcms.smartcontent.model.PaginatedResponse;
import com.smartcms.smartcontent.model.RollbackField;
import com.smartcms.smartcontent.service.ContentServiceImpl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
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
@Api(tags = "Content Management", description = "Endpoints for managing content lifecycle")
public class ContentController {

    private final ContentServiceImpl contentServiceImpl;

    @ApiOperation(value = "Create new content", notes = "Creates new content item with the provided details")
    @ApiResponses({
            @ApiResponse(code = 201, message = "Content created successfully", response = Content.class),
            @ApiResponse(code = 400, message = "Invalid input data"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Insufficient permissions")
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
    @ApiOperation(value = "Get content by ID", notes = "Retrieves content details including versions and metadata")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Content found", response = Content.class),
            @ApiResponse(code = 404, message = "Content not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Content> getContentById(@PathVariable String id) {
        Content content = contentServiceImpl.getContentById(id);
        return ResponseEntity.ok(content);
    }

    @ApiOperation(value = "List content by organization", notes = "Retrieves paginated list of content for an organization")
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
    @ApiOperation(value = "Update content", notes = "Updates content details (title, description, body, etc.)")
    @PatchMapping("/{id}")
    public ResponseEntity<Content> updateContent(
            @PathVariable String id,
            @RequestBody @Valid ContentUpdateRequest request,
            @RequestHeader("X-User-Id") String updatedBy) {

        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        Content updatedContent = contentServiceImpl.updateContent(id, request, updatedBy);
        return ResponseEntity.ok(updatedContent);
    }

    @ApiOperation(value = "Update content slug", notes = "Updates the URL-friendly slug for content")
    @PatchMapping("/{id}/slug")
    public ResponseEntity<Content> updateSlug(
            @PathVariable String id,
            @RequestParam String newSlug,
            @RequestHeader("X-User-Id") String updatedBy) {
        Content content = contentServiceImpl.updateSlug(id, newSlug, updatedBy);
        return ResponseEntity.ok(content);
    }

    @ApiOperation(value = "Get content by status", notes = "Retrieves paginated list of content by status")
    @GetMapping("/status")
    public ResponseEntity<PaginatedResponse<Content>> listContentByStatus(
            @RequestHeader("X-Org-Id") String orgId,
            @RequestParam ContentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PaginatedResponse<Content> response = contentServiceImpl.getContentByStatus(orgId, status, page, size);
        return ResponseEntity.ok(response);
    }

    @ApiOperation(value = "Update content status", notes = "Change status between DRAFT, PUBLISHED, or ARCHIVED")
    @PatchMapping("/{contentId}/status")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Status updated successfully"),
            @ApiResponse(code = 400, message = "Invalid status transition"),
            @ApiResponse(code = 404, message = "Content not found")
    })
    public ResponseEntity<Content> updateContentStatus(
            @PathVariable String contentId,
            @RequestParam @NotNull ContentStatus newStatus,
            @RequestHeader("X-User-Id") String updatedBy) {

        Content updatedContent = contentServiceImpl.updateStatus(contentId, newStatus, updatedBy);
        return ResponseEntity.ok(updatedContent);
    }

    @ApiOperation(value = "Schedule content publishing", notes = "Set future publish date/time")
    @PatchMapping("/{contentId}/schedule")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Content scheduled"),
            @ApiResponse(code = 400, message = "Invalid schedule time"),
            @ApiResponse(code = 404, message = "Content not found")
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

    @ApiOperation(value = "Rollback content version", notes = "Restores content to a previous version (full or partial rollback)")
    @PostMapping("/{id}/rollback")
    public ResponseEntity<Content> rollbackContent(
            @PathVariable String id,
            @RequestParam int version,
            @RequestHeader("X-User-Id") String updatedBy,
            @RequestBody(required = false) Set<RollbackField> fieldsToRollback) {

        if (fieldsToRollback == null) {
            fieldsToRollback = Set.of();
        }
        Content rolledBackContent = contentServiceImpl.rollbackContent(id, version, updatedBy, fieldsToRollback);
        return ResponseEntity.ok(rolledBackContent);
    }


    @ApiOperation(value = "List content versions", notes = "Retrieves version history for content")
    @GetMapping("/{id}/versions")
    public ResponseEntity<List<ContentVersionDto>> listContentVersions(
            @PathVariable String id) {
        List<ContentVersionDto> versions = contentServiceImpl.getContentVersions(id);
        return ResponseEntity.ok(versions);
    }

    @ApiOperation(value = "List bin content", notes = "Retrieves paginated list of content in recycle bin")
    @GetMapping("/bin")
    public ResponseEntity<PaginatedResponse<Content>> listBinContent(
            @RequestHeader("X-Org-Id") String orgId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PaginatedResponse<Content> response = contentServiceImpl.getBinContent(orgId, page, size);
        return ResponseEntity.ok(response);
    }

    // Content Recycle Bin
    @ApiOperation(value = "Move content to bin", notes = "Soft-deletes content (moves to recycle bin)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> moveToBin(
            @PathVariable String id,
            @RequestHeader("X-Org-Id") String deletedByUserId) {

        contentServiceImpl.moveToBin(id, deletedByUserId);
        return ResponseEntity.noContent().build(); // HTTP 204
    }

    @ApiOperation(value = "Restore content from bin", notes = "Restores content from recycle bin to active state")
    @PostMapping("/bin/{id}/restore")
    public ResponseEntity<Content> restoreContent(
            @PathVariable String id,
            @RequestHeader("X-Org-Id") String restoredByUserId) {

        Content restoredContent = contentServiceImpl.restoreContent(id, restoredByUserId);
        return ResponseEntity.ok(restoredContent);
    }

    @ApiOperation(value = "Permanently delete content", notes = "Hard-deletes content from the system (irreversible)")
    @DeleteMapping("/bin/{id}")
    public ResponseEntity<Void> deleteContent(
            @PathVariable String id,
            @RequestHeader("X-Org-Id") String deletedByUserId) {
        contentServiceImpl.deleteContent(id);
        return ResponseEntity.noContent().build(); // HTTP 204
    }

    // Content Relationships
//    @ApiOperation(value = "Link related content", notes = "Creates relationship between content items")
//    @PostMapping("/{id}/related")
//    public ResponseEntity<Void> addRelatedContent(
//            @PathVariable String id,
//            @RequestBody @Valid RelatedContentRequest request,
//            @RequestHeader("X-User-Id") String linkedBy) {
//        // Implementation
//    }
//
//    @ApiOperation(value = "List related content", notes = "Retrieves content related to specified item")
//    @GetMapping("/{id}/related")
//    public ResponseEntity<List<Content>> getRelatedContent(
//            @PathVariable String id) {
//        // Implementation
//    }

    // Content Search
//    @ApiOperation(value = "Search content", notes = "Full-text search across content with filters")
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
