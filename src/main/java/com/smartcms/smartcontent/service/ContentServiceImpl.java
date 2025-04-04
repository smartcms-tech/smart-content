package com.smartcms.smartcontent.service;

import com.smartcms.smartcommon.exception.ResourceNotFoundException;
import com.smartcms.smartcommon.exception.ServiceLayerException;
import com.smartcms.smartcommon.model.*;
import com.smartcms.smartcontent.client.SmartMediaClient;
import com.smartcms.smartcontent.dto.ContentRequest;
import com.smartcms.smartcontent.dto.ContentUpdateRequest;
import com.smartcms.smartcontent.dto.ContentVersionDto;
import com.smartcms.smartcontent.exception.InvalidScheduleTimeException;
import com.smartcms.smartcontent.model.*;
import com.smartcms.smartcontent.repository.ContentHistoryRepository;
import com.smartcms.smartcontent.repository.ContentRepository;
import com.smartcms.smartcontent.utility.SlugGenerator;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentServiceImpl implements ContentService {

    private final SlugGenerator slugGenerator;
    private final ContentRepository contentRepository;
    private final ContentHistoryRepository contentHistoryRepository;
    private final SmartMediaClient mediaClient;

    // Status transition validation rules
    private static final Map<ContentStatus, Set<ContentStatus>> ALLOWED_TRANSITIONS = Map.of(
            ContentStatus.DRAFT, Set.of(ContentStatus.UNDER_REVIEW),
            ContentStatus.UNDER_REVIEW, Set.of(ContentStatus.APPROVED, ContentStatus.REJECTED),
            ContentStatus.REJECTED, Set.of(ContentStatus.DRAFT), // Can be re-edited
            ContentStatus.APPROVED, Set.of(ContentStatus.PUBLISHED, ContentStatus.SCHEDULED),
            ContentStatus.SCHEDULED, Set.of(ContentStatus.PUBLISHED), // Auto-publish via scheduler
            ContentStatus.PUBLISHED, Set.of(ContentStatus.ARCHIVED),
            ContentStatus.ARCHIVED, Set.of(ContentStatus.PUBLISHED),
            ContentStatus.DELETED, Set.of() // No further transitions
    );
    public Content createContent(ContentRequest request, String userId, String orgId) {
        validateRequest(request);
        log.debug("Creating content for user {} in org {}", userId, orgId);

        String slug = slugGenerator.generateSlug(request.getTitle());

        if (slug.isEmpty()) {
            slug = slugGenerator.generateSlugWithAI(request.getDescription());
        }

        Content content = Content.builder()
                .title(request.getTitle())
                .slug(slug)  // Using provided SlugGenerator
                .description(request.getDescription())
                .body(request.getBody())
                .tags(request.getTags() != null ? request.getTags() : Collections.emptyList())
                .contentType(request.getContentType())
                .meta(request.getMeta())
                .mediaReferences(request.getMediaReferences() != null ? request.getMediaReferences() : Collections.emptyList())
                .status(ContentStatus.DRAFT)
                .version(1)  // Initial version
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .author(new UserDetails(userId))  // Setting author details
                .lastUpdatedBy(new UserDetails(userId))  // Initially the same as author
                .orgDetails(OrgDetails.builder().orgId(orgId).build())
                .aiInsights(request.getAiInsights())// Setting organization details
                .build();

        return contentRepository.save(content);
    }

    public Content getContentById(String id) {
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("Content ID cannot be blank");
        }
        log.debug("Fetching content by id: {}", id);
        return getExistingContent(id);
    }

    public PaginatedResponse<Content> getOrgContent(String orgId, int page, int size, String sortBy) {

        log.debug("Fetching org content for orgId: {}, page: {}, size: {}, sortBy: {}", orgId, page, size, sortBy);
        Pageable pageable = PageRequest.of(page, size, Sort.by("updatedAt").descending());
        Page<Content> pageResult = contentRepository.findByOrgIdAndStatusNot(orgId, ContentStatus.DELETED, pageable);

        if (pageResult.isEmpty()) {
            throw new ResourceNotFoundException("No content found for org: " + orgId);
        }
        log.debug("Found {} org items for orgId: {}", pageResult.getNumberOfElements(), orgId);
        return buildPaginatedResponse(pageResult);
    }

    public Content updateContent(String contentId, ContentUpdateRequest request, String updatedBy) {
        // Step 1: Fetch existing content
        Content existingContent = getExistingContent(contentId);

        // Step 2: Save history before updating
        UserDetails updatedByUser = new UserDetails(updatedBy);
        String changeSummary = generateSummary(request, existingContent);

        saveContentHistory(existingContent, updatedByUser, changeSummary);

        // Step 3: Update fields only if provided in the request
        Optional.ofNullable(request.getTitle()).ifPresent(existingContent::setTitle);
        Optional.ofNullable(request.getDescription()).ifPresent(existingContent::setDescription);
        Optional.ofNullable(request.getBody()).ifPresent(existingContent::setBody);
        Optional.ofNullable(request.getTags()).ifPresent(existingContent::setTags);
        Optional.ofNullable(request.getMeta()).ifPresent(existingContent::setMeta);
        Optional.ofNullable(request.getMediaReferences()).ifPresent(existingContent::setMediaReferences);
        existingContent.setUpdatedAt(Instant.now());
        existingContent.setVersion(existingContent.getVersion() + 1);
        existingContent.setLastUpdatedBy(updatedByUser);

        // Step 4: Save updated content
        return contentRepository.save(existingContent);
    }

    public PaginatedResponse<Content> getContentByStatus(String orgId, ContentStatus status, int page, int size) {
        log.debug("Fetching content by status for orgId: {}, status: {}, page: {}, size: {}", orgId, status, page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("updatedAt").descending());
        Page<Content> pageResult = contentRepository.findByOrgIdAndStatus(orgId, status, pageable);

        if (pageResult.isEmpty()) {
            throw new ResourceNotFoundException("No content found for org: " + orgId + " with status: " + status);
        }
        log.debug("Found {} items for orgId: {} with status: {}", pageResult.getNumberOfElements(), orgId, status);
        return buildPaginatedResponse(pageResult);
    }

    public Content updateStatus(String contentId, ContentStatus newStatus, String updatedBy) {
        Content content = getExistingContent(contentId);
        validateStatusTransition(content.getStatus(), newStatus);

        content.setStatus(newStatus);
        content.setUpdatedAt(Instant.now());
        content.setLastUpdatedBy(new UserDetails(updatedBy));

        // Set publishedAt when publishing
        if (newStatus.equals(ContentStatus.PUBLISHED)) {
            content.setPublishedAt(Instant.now());
        }
        if (newStatus.equals(ContentStatus.APPROVED) || newStatus.equals(ContentStatus.REJECTED)) {
            content.setReviewedBy(new UserDetails(updatedBy));
        }

        log.info("Updated status of content {} to {} by user {}", contentId, newStatus, updatedBy);
        return contentRepository.save(content);
    }

    public Content schedulePublishing(String contentId, Instant publishTime, String scheduledBy) {
        if (publishTime.isBefore(Instant.now())) {
            throw new InvalidScheduleTimeException("Schedule time must be in the future");
        }

        Content content = getExistingContent(contentId);
        content.setStatus(ContentStatus.SCHEDULED);
        content.setScheduledPublishAt(publishTime);
        content.setUpdatedAt(Instant.now());
        content.setLastUpdatedBy(new UserDetails(scheduledBy));

        log.info("Scheduled content {} for publishing at {} by user {}", contentId, publishTime, scheduledBy);
        return contentRepository.save(content);
    }

    @Scheduled(fixedRate = 300000) // Primary check every 5 mins
    @Transactional
    public void processScheduledContent() {
        Instant now = Instant.now();
        Instant safetyWindow = now.minus(10, ChronoUnit.MINUTES);

        // 1. Process current scheduled items
        List<Content> currentBatch = contentRepository.findByStatusAndScheduledPublishAtBetween(ContentStatus.SCHEDULED, safetyWindow, now);

        // 2. Find any missed items (should be rare)
        List<Content> missedItems = contentRepository.findByStatusAndScheduledPublishAtBefore(ContentStatus.SCHEDULED, safetyWindow);

        if (!currentBatch.isEmpty() || !missedItems.isEmpty()) {
            Stream.concat(currentBatch.stream(), missedItems.stream())
                    .forEach(content -> publishContent(content, content.getScheduledPublishAt()));

            log.info("Published {} current and {} missed scheduled items",
                    currentBatch.size(), missedItems.size());
        }
    }

    private void publishContent(Content content, Instant publishTime) {
        content.setStatus(ContentStatus.PUBLISHED);
        content.setPublishedAt(publishTime);
        content.setUpdatedAt(Instant.now());
        contentRepository.save(content);

        log.warn("Published content {} (original schedule: {})",
                content.getId(), content.getScheduledPublishAt());
    }

    public void moveToBin(String id, String deletedBy) {
        Content content = getExistingContent(id);

        if (content.getStatus() == ContentStatus.DELETED) {
            throw new IllegalStateException("Content is already deleted");
        }
        UserDetails deletedByUser = new UserDetails(deletedBy);

        content.setStatus(ContentStatus.DELETED);
        content.setUpdatedAt(Instant.now());
        content.setDeletedAt(Instant.now());
        content.setLastUpdatedBy(deletedByUser);

        contentRepository.save(content);
        log.info("Soft deleted content with id: {} by user: {}", id, deletedBy);
    }

    public Content restoreContent(String id, String restoredBy) {
        Content content = contentRepository.findByIdAndStatus(id, ContentStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Content is not deleted or not found"));

        if (content.getStatus() != ContentStatus.DELETED) {
            throw new IllegalStateException("Content is not deleted, undo is not applicable");
        }

        content.setStatus(ContentStatus.DRAFT);
        content.setUpdatedAt(Instant.now());
        content.setDeletedAt(null);
        content.setLastUpdatedBy(new UserDetails(restoredBy));

        log.info("Restored content {} from bin by user {}", id, restoredBy);
        return contentRepository.save(content);
    }

    @Scheduled(cron = "0 0 2 * * ?")  // Runs daily at 2 AM
    public void permanentDeleteExpiredContent() {
        Instant expirationTime = Instant.now().minus(15, ChronoUnit.DAYS);
        List<Content> expiredContent = contentRepository.findByStatusAndDeletedAtBefore(ContentStatus.DELETED, expirationTime);

        if (expiredContent.isEmpty()) {
            log.info("No expired bin items found");
            return;
        }
        log.info("Purged {} expired bin items", expiredContent.size());

        for (Content content : expiredContent) {
            deleteAssociatedMedia(content);
        }

        contentRepository.deleteAll(expiredContent);
        log.info("Permanently deleted {} expired content items", expiredContent.size());
    }

    public PaginatedResponse<Content> getBinContent(String orgId, int page, int size) {
        log.debug("Fetching bin content for orgId: {}, page: {}, size: {}", orgId, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("deletedAt").descending());
        Page<Content> pageResult = contentRepository.findByOrgIdAndStatus(orgId, ContentStatus.DELETED, pageable);

        log.debug("Found {} bin items for orgId: {}", pageResult.getNumberOfElements(), orgId);
        return buildPaginatedResponse(pageResult);
    }

    public void deleteContent(String id) {
        Content content = contentRepository.findByIdAndStatus(id, ContentStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Content must be deleted first"));

        try {
            deleteAssociatedMedia(content);
            contentRepository.delete(content);
            log.info("Permanently deleted content {}", id);
        } catch (Exception e) {
            log.error("Failed to delete content {}", id, e);
            throw new ServiceLayerException("Failed to delete content", e);
        }
    }

    @Override
    public List<ContentVersionDto> getContentVersions(String contentId) {
        log.debug("Fetching versions for contentId: {}", contentId);

        List<ContentHistory> historyEntries = contentHistoryRepository.findByContentSnapshotId(contentId);
        if (historyEntries.isEmpty()) {
            throw new ResourceNotFoundException("No versions found for contentId: " + contentId);
        }

        return historyEntries.stream()
                .map(history -> new ContentVersionDto(
                        history.getContentSnapshot().getVersion(),
                        history.getCreatedBy().getName(),
                        history.getCreatedAt(),
                        history.getChangeReason()// Can be replaced with computed summary if needed
                ))
                .collect(Collectors.toList());

    }

    public Content rollbackContent(String contentId, int version, String rolledBackBy, Set<RollbackField> fieldsToRollback) {
        try {
            log.info("Starting rollback for contentId: {}, version: {}, requested by: {}",
                    contentId, version, rolledBackBy);

            validateRollbackInputs(contentId, version, rolledBackBy);

            Content currentContent = getExistingContent(contentId);

            if (currentContent.getVersion() == version) {
                throw new IllegalArgumentException("Cannot rollback to the same version");
            }else if (currentContent.getVersion() < version) {
                throw new IllegalArgumentException("Cannot rollback to a future version");
            }else if (currentContent.getStatus() == ContentStatus.DELETED) {
                throw new ServiceLayerException("Cannot rollback a deleted content");
            }

            // If fieldsToRollback is empty, rollback everything
            if (fieldsToRollback == null || fieldsToRollback.isEmpty()) {
                fieldsToRollback = EnumSet.allOf(RollbackField.class);
            }

            // Step 2: Save the current content in history before rollback
            saveContentHistory(currentContent, new UserDetails(rolledBackBy), "Before rollback to version " + version);

            // Step 3: Fetch the historical version
            ContentHistory historyEntry = contentHistoryRepository.findByContentSnapshotIdAndContentSnapshotVersion(contentId, version)
                    .orElseThrow(() -> new ResourceNotFoundException("Content history not found for version: " + version));

            Content snapshot = historyEntry.getContentSnapshot();
            log.debug("Fetched historical version: {}", version);

            applyRollbackFields(currentContent, snapshot, fieldsToRollback);

            currentContent.setUpdatedAt(Instant.now());
            currentContent.setVersion(currentContent.getVersion() + 1); // Increment version
            currentContent.setLastUpdatedBy(new UserDetails(rolledBackBy));

            Content rolledBackContent = contentRepository.save(currentContent);
            log.info("Successfully rolled back contentId: {} to version: {}", contentId, version);

            return rolledBackContent;

        } catch (Exception e) {
            log.error("Unexpected error during rollback for contentId: {} - {}",
                    contentId, e.getMessage(), e);
            throw new ServiceLayerException("Failed to perform rollback", e);
        }
    }

    public Content updateSlug(String contentId, String newSlug, String updatedBy) {
        Content content = getExistingContent(contentId);

        // Save history
        saveContentHistory(content, new UserDetails(updatedBy), "Slug updated to " + newSlug);

        // Update the slug
        content.setSlug(newSlug);
        content.setUpdatedAt(Instant.now());
        content.setVersion(content.getVersion() + 1); // Increment version
        content.setLastUpdatedBy(new UserDetails(updatedBy));

        // Save the updated content
        return contentRepository.save(content);
    }

    private void deleteAssociatedMedia(Content content) {
        // Extract media IDs from content
        List<String> mediaIds = content.getMediaReferences()
                .stream()
                .map(MediaReference::getMediaId)
                .collect(Collectors.toList());

        try {
            mediaClient.bulkDeleteMedia(mediaIds); // Efficient batch deletion
        } catch (Exception e) {
            log.warn("Failed to delete some media for content {}. Proceeding anyway. Error: {}",
                    content.getId(), e.getMessage());
        }
    }

    private Content getExistingContent(String contentId) {
        return contentRepository.findById(contentId)
                .orElseThrow(() ->  new ResourceNotFoundException("Content not found with id: " + contentId));
    }

    private void validateStatusTransition(ContentStatus current, ContentStatus newStatus) {
        if (!ALLOWED_TRANSITIONS.getOrDefault(current, Set.of()).contains(newStatus)) {
            throw new ServiceLayerException(
                    String.format("Invalid status transition from %s to %s", current, newStatus));
        }
    }

    private void saveContentHistory(Content content, UserDetails createdBy, String changeReason) {
        ContentHistory history = ContentHistory.builder()
                .contentSnapshot(content)
                .createdAt(Instant.now())
                .createdBy(createdBy)
                .changeReason(changeReason)
                .build();
        contentHistoryRepository.save(history);
    }

    private String generateSummary(ContentUpdateRequest request, Content existingContent) {
        List<String> changedFields = new ArrayList<>();

        if (request.getTitle() != null && !request.getTitle().equals(existingContent.getTitle())) {
            changedFields.add("Title");
        }
        if (request.getDescription() != null && !request.getDescription().equals(existingContent.getDescription())) {
            changedFields.add("Description");
        }
        if (request.getBody() != null && !request.getBody().equals(existingContent.getBody())) {
            changedFields.add("Body");
        }
        if (request.getTags() != null && !request.getTags().equals(existingContent.getTags())) {
            changedFields.add("Tags");
        }
        if (request.getMeta() != null && !request.getMeta().equals(existingContent.getMeta())) {
            changedFields.add("Meta");
        }
        if (request.getMediaReferences() != null && !request.getMediaReferences().equals(existingContent.getMediaReferences())) {
            changedFields.add("Media References");
        }
        return changedFields.isEmpty() ? "No changes made" : "Updated " + String.join(", ", changedFields);
    }

    private void validateRequest(ContentRequest request) {
        List<String> errors = new ArrayList<>();

        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            errors.add("Title cannot be empty");
        }
        if (request.getDescription() == null || request.getDescription().trim().isEmpty()) {
            errors.add("Description cannot be empty");
        }
        if (request.getBody() == null || request.getBody().trim().isEmpty()) {
            errors.add("Content body cannot be empty");
        }
        if (request.getContentType() == null) {
            errors.add("Content type is required");
        }

        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(", ", errors));
        }
    }

    private void validateRollbackInputs(String contentId, int version, String rolledBy) {
        if (StringUtils.isBlank(contentId)) {
            throw new IllegalArgumentException("Content ID cannot be blank");
        }
        if (version <= 0) {
            throw new IllegalArgumentException("Version must be positive");
        }
        if (StringUtils.isBlank(rolledBy)) {
            throw new IllegalArgumentException("Rollback user cannot be blank");
        }
    }

    private void applyRollbackFields(Content currentContent, Content snapshot, Set<RollbackField> fieldsToRollback) {
        for (RollbackField field : fieldsToRollback) {
            switch (field) {
                case TITLE -> currentContent.setTitle(snapshot.getTitle());
                case DESCRIPTION -> currentContent.setDescription(snapshot.getDescription());
                case BODY -> currentContent.setBody(snapshot.getBody());
                case TAGS -> currentContent.setTags(snapshot.getTags() != null ? snapshot.getTags() : List.of());
                case MEDIA -> currentContent.setMediaReferences(snapshot.getMediaReferences() != null ? snapshot.getMediaReferences() : List.of());
                case CONTENT_TYPE -> currentContent.setContentType(snapshot.getContentType());
                case META -> currentContent.setMeta(snapshot.getMeta() != null ? snapshot.getMeta() : new MetaData());
                case STATUS -> currentContent.setStatus(snapshot.getStatus());
            }
        }
    }

    private <T> PaginatedResponse<T> buildPaginatedResponse(Page<T> page) {
        return new PaginatedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

}
