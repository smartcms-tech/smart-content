package com.smartcms.smartcontent.service;

import com.smartcms.smartcommon.model.Content;
import com.smartcms.smartcommon.model.ContentStatus;
import com.smartcms.smartcontent.dto.*;
import com.smartcms.smartcontent.model.*;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public interface ContentService {

    // Content CRUD Operations
    Content createContent(ContentRequest request, String userId, String orgId);
    Content getContentById(String id);
    PaginatedResponse<Content> getOrgContent(String orgId, int page, int size, String sortBy);
    Content updateContent(String contentId, ContentUpdateRequest request, String updatedBy);
    PaginatedResponse<Content> getContentByStatus(String orgId, ContentStatus status, int page, int size);

    // Content Status Management
    Content updateStatus(String contentId, ContentStatus newStatus, String updatedBy, String note);
    Content schedulePublishing(String contentId, Instant publishTime, String scheduledBy);
    void processScheduledContent();

    // Content Lifecycle Operations
    void moveToBin(String id, String deletedBy);
    Content restoreContent(String id, String restoredBy);
    void permanentDeleteExpiredContent();
    PaginatedResponse<Content> getBinContent(String orgId, int page, int size);
    void deleteContent(String id);

    // Version Control
    List<ContentVersionDto> getContentVersions(String contentId);
    Content rollbackContent(String contentId, int version, String rolledBackBy, Set<RollbackField> fieldsToRollback);

    // Update Slug
    Content updateSlug(String contentId, String newSlug, String updatedBy);
    SlugValidationResponse validateSlug(String slug, String orgId, String contentId);
    String generateUniqueSlug(String contentId, String orgId);

    // Content Status Audit
    List<ContentStatusAudit> getStatusAuditForContent(String contentId);
}
