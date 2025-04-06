package com.smartcms.smartcontent.service;

import com.smartcms.smartcommon.exception.ResourceNotFoundException;
import com.smartcms.smartcommon.exception.ServiceLayerException;
import com.smartcms.smartcommon.model.*;
import com.smartcms.smartcontent.dto.ContentRequest;
import com.smartcms.smartcontent.dto.ContentUpdateRequest;

import com.smartcms.smartcontent.dto.ContentVersionDto;
import com.smartcms.smartcontent.dto.SlugValidationResponse;
import com.smartcms.smartcontent.model.*;
import com.smartcms.smartcontent.repository.ContentHistoryRepository;
import com.smartcms.smartcontent.repository.ContentRepository;
import com.smartcms.smartcontent.repository.ContentStatusAuditRepository;
import com.smartcms.smartcontent.utility.SlugGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContentServiceImplTest {

    @Mock
    private ContentRepository contentRepository;

    @Mock
    private ContentHistoryRepository contentHistoryRepository;

    @Mock
    private ContentStatusAuditRepository contentStatusAuditRepository;

    @Mock
    private SlugGenerator slugGenerator;

    @InjectMocks
    private ContentServiceImpl contentServiceImpl;

    private ContentRequest contentRequest;
    private ContentUpdateRequest contentUpdateRequest;
    private Content content;
    private final String USER_ID = "user123";
    private final String ORG_ID = "org456";
    private final String CONTENT_ID = "content789";

    @BeforeEach
    void setUp() {
        Instant now = Instant.now();

        // Setup valid media references
        List<MediaReference> mediaReferences = new ArrayList<>();
        MediaReference media1 = new MediaReference();
        media1.setMediaId("media123");
        media1.setUrl("https://example.com/image1.jpg");
        media1.setType(MediaType.IMAGE);
        media1.setAIGenerated(false);
        media1.setPrimary(true);

        MediaReference media2 = new MediaReference();
        media2.setMediaId("media456");
        media2.setUrl("https://example.com/video1.mp4");
        media2.setType(MediaType.VIDEO);
        media2.setAIGenerated(true);
        media2.setPrimary(false);

        mediaReferences.add(media1);
        mediaReferences.add(media2);

        // Setup AI Insights
        AIInsights aiInsights = new AIInsights();
        aiInsights.setPrompt("Create a blog post about Java Spring Boot");
        aiInsights.setWordCount(750);
        aiInsights.setTone("Professional");
        aiInsights.setTargetAudience("Software Developers");
        aiInsights.setGeneratedBy("GPT-4");
        aiInsights.setGeneratedAt(now);

        contentRequest = ContentRequest.builder()
                .title("Test Title")
                .description("Test Description")
                .body("Test Body Content")
                .tags(List.of("tag1", "tag2"))
                .contentType(ContentType.ARTICLE)
                .build();

        MetaData meta = MetaData.builder()
                .metaTitle("Test Meta Title")
                .metaDescription("Test Meta Description")
                .metaKeywords(List.of("keyword1", "keyword2"))
                .canonicalUrl("https://example.com/test")
                .build();

        contentRequest.setMediaReferences(mediaReferences);
        contentRequest.setAiInsights(aiInsights);
        contentRequest.setMeta(meta);


        contentUpdateRequest = ContentUpdateRequest.builder()
                .title("Updated Title")
                .description("Updated Description")
                .body("Updated Body")
                .tags(List.of("updatedTag1", "updatedTag2"))
                .meta(meta)
                .mediaReferences(mediaReferences)
                .build();

        content = Content.builder()
                .id(CONTENT_ID)
                .title("Test Title")
                .slug("test-title")
                .description("Test Description")
                .body("Test Body")
                .tags(List.of("tag1", "tag2"))
                .contentType(ContentType.ARTICLE)
                .meta(meta)
                .mediaReferences(mediaReferences)
                .status(ContentStatus.DRAFT)
                .version(1)
                .createdAt(now)
                .updatedAt(now)
                .author(new UserDetails(USER_ID))
                .lastUpdatedBy(new UserDetails(USER_ID))
                .orgDetails(new OrgDetails(ORG_ID))
                .aiInsights(aiInsights)
                .build();
    }

    @Test
    @DisplayName("Should successfully create content")
    void testCreateContent_Success() {
        when(slugGenerator.generateSlug(anyString())).thenReturn("test-title");
        when(contentRepository.save(any(Content.class))).thenReturn(content);

        Content createdContent = contentServiceImpl.createContent(contentRequest, USER_ID, ORG_ID);

        assertNotNull(createdContent);
        assertEquals(contentRequest.getTitle(), createdContent.getTitle());
        assertEquals("test-title", createdContent.getSlug());
        assertEquals(ContentStatus.DRAFT, createdContent.getStatus());
        assertEquals(1, createdContent.getVersion());

        verify(contentRepository, times(1)).save(any(Content.class));
        verify(slugGenerator, times(1)).generateSlug(anyString());
    }

    @Test
    @DisplayName("Should throw exception when title and body is empty")
    void testCreateContent_ValidationFailure() {
        contentRequest.setTitle(null);
        contentRequest.setBody(null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> contentServiceImpl.createContent(contentRequest, USER_ID, ORG_ID));

        assertTrue(exception.getMessage().contains("Title cannot be empty"));
        assertTrue(exception.getMessage().contains("Content body cannot be empty"));

        verify(contentRepository, never()).save(any(Content.class));
    }

    @Test
    @DisplayName("Should get content by ID successfully")
    void testGetContentById_Success() {
        // Mock repository
        when(contentRepository.findById(CONTENT_ID)).thenReturn(Optional.of(content));
        Content foundContent = contentServiceImpl.getContentById(CONTENT_ID);

        assertNotNull(foundContent);
        assertEquals(CONTENT_ID, foundContent.getId());

        verify(contentRepository, times(1)).findById(CONTENT_ID);
    }

    @Test
    @DisplayName("Should throw exception when content not found")
    void testGetContentById_NotFound() {
        when(contentRepository.findById(CONTENT_ID)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> contentServiceImpl.getContentById(CONTENT_ID));

        assertEquals("Content not found with id: " + CONTENT_ID, exception.getMessage());
        verify(contentRepository, times(1)).findById(CONTENT_ID);
    }

    @Test
    @DisplayName("Should get content by organization")
    void testGetOrgContent_Success() {
        Page<Content> page = new PageImpl<>(List.of(content));
        when(contentRepository.findByOrgIdAndStatusNot(eq(ORG_ID), eq(ContentStatus.DELETED), any(Pageable.class)))
                .thenReturn(page);

        PaginatedResponse<Content> response = contentServiceImpl.getOrgContent(ORG_ID, 0, 20, null);

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        assertEquals(1, response.getTotalPages());
        verify(contentRepository, times(1)).findByOrgIdAndStatusNot(eq(ORG_ID), eq(ContentStatus.DELETED), any(Pageable.class));
    }

    @Test
    @DisplayName("Should get empty list when no content found for organization")
    void testGetOrgContent_Empty() {
        Page<Content> page = new PageImpl<>(Collections.emptyList());
        when(contentRepository.findByOrgIdAndStatusNot(eq(ORG_ID), eq(ContentStatus.DELETED), any(Pageable.class)))
                .thenReturn(page);

        PaginatedResponse<Content> response = contentServiceImpl.getOrgContent(ORG_ID, 0, 20, null);

        assertNotNull(response);
        assertEquals(0, response.getTotalElements());
        assertEquals(1, response.getTotalPages());
        verify(contentRepository, times(1)).findByOrgIdAndStatusNot(eq(ORG_ID), eq(ContentStatus.DELETED), any(Pageable.class));
    }


    @Test
    @DisplayName("Should update content successfully")
    void testUpdateContent_Success() {
        when(contentRepository.findById(CONTENT_ID)).thenReturn(Optional.of(content));
        when(contentRepository.save(any(Content.class))).thenReturn(content);

        Content updatedContent = contentServiceImpl.updateContent(CONTENT_ID, contentUpdateRequest, USER_ID);

        assertNotNull(updatedContent);
        assertEquals("Updated Title", updatedContent.getTitle());
        assertEquals("Updated Description", updatedContent.getDescription());
        assertEquals(2, updatedContent.getVersion()); // Version should increment

        ArgumentCaptor<Content> contentCaptor = ArgumentCaptor.forClass(Content.class);
        verify(contentRepository, times(1)).save(contentCaptor.capture());
        Content savedContent = contentCaptor.getValue();
        assertEquals("Updated Title", savedContent.getTitle());

        verify(contentHistoryRepository, times(1)).save(any(ContentHistory.class));
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent content")
    void testUpdateContent_NotFound() {
        when(contentRepository.findById(CONTENT_ID)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> contentServiceImpl.updateContent(CONTENT_ID, contentUpdateRequest, USER_ID));

        assertEquals("Content not found with id: " + CONTENT_ID, exception.getMessage());
        verify(contentRepository, times(1)).findById(CONTENT_ID);
    }

    @Test
    @DisplayName("Should return content by status")
    void testGetContentByStatus_Success() {
        Page<Content> page = new PageImpl<>(List.of(content));
        when(contentRepository.findByOrgIdAndStatus(eq(ORG_ID), eq(ContentStatus.DRAFT), any(Pageable.class)))
                .thenReturn(page);

        PaginatedResponse<Content> result = contentServiceImpl.getContentByStatus(ORG_ID, ContentStatus.DRAFT, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getTotalPages());
        verify(contentRepository, times(1)).findByOrgIdAndStatus(eq(ORG_ID), eq(ContentStatus.DRAFT), any(Pageable.class));
    }




    @Test
    @DisplayName("Should update content status successfully")
    void testUpdateContentStatus_Success() {
        when(contentRepository.findById(CONTENT_ID)).thenReturn(Optional.of(content));
        when(contentRepository.save(any(Content.class))).thenReturn(content);

        Content updatedContent = contentServiceImpl.updateStatus(CONTENT_ID, ContentStatus.UNDER_REVIEW, USER_ID, "Submitted for editorial check");

        assertNotNull(updatedContent);
        assertEquals(ContentStatus.UNDER_REVIEW, updatedContent.getStatus());

        verify(contentRepository, times(1)).save(any(Content.class));
        verify(contentStatusAuditRepository, times(1)).save(any(ContentStatusAudit.class));
    }

    @Test
    @DisplayName("Should throw exception when updating status for non-existent content")
    void testUpdateContentStatus_NotFound() {
        when(contentRepository.findById(CONTENT_ID)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> contentServiceImpl.updateStatus(CONTENT_ID, ContentStatus.PUBLISHED, USER_ID, "Publishing the content"));

        assertEquals("Content not found with id: " + CONTENT_ID, exception.getMessage());
        verify(contentRepository, times(1)).findById(CONTENT_ID);
        verify(contentRepository, never()).save(any(Content.class));
        verify(contentStatusAuditRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when updating status to invalid transition")
    void testUpdateContentStatus_InvalidTransition() {
        when(contentRepository.findById(CONTENT_ID)).thenReturn(Optional.of(content));

        ServiceLayerException exception = assertThrows(ServiceLayerException.class,
                () -> contentServiceImpl.updateStatus(CONTENT_ID, ContentStatus.ARCHIVED, USER_ID, "Trying to archive directly from draft"));

        assertEquals("Invalid status transition from DRAFT to ARCHIVED", exception.getMessage());

        verify(contentRepository, never()).save(any(Content.class));
        verify(contentStatusAuditRepository, never()).save(any(ContentStatusAudit.class));
    }

    @Test
    @DisplayName("Should schedule content publishing")
    void testSchedulePublishing_Success() {
        content.setStatus(ContentStatus.APPROVED);
        when(contentRepository.findById(CONTENT_ID)).thenReturn(Optional.of(content));
        when(contentRepository.save(any(Content.class))).thenReturn(content);

        Instant publishTime = Instant.now().plus(Duration.ofHours(2));
        Content scheduledContent = contentServiceImpl.schedulePublishing(CONTENT_ID, publishTime, USER_ID);

        assertNotNull(scheduledContent);
        assertEquals(publishTime, scheduledContent.getScheduledPublishAt());
        assertEquals(ContentStatus.SCHEDULED, scheduledContent.getStatus());
        verify(contentRepository).save(any(Content.class));
    }

    @Test
    @DisplayName("Should publish both current and missed scheduled content")
    void testProcessScheduledContent_PublishesContent() {
        Instant now = Instant.now();
        Content current = new Content();
        current.setId("content1");
        current.setStatus(ContentStatus.SCHEDULED);
        current.setScheduledPublishAt(now.minus(5, ChronoUnit.MINUTES));

        Content missed = new Content();
        missed.setId("content2");
        missed.setStatus(ContentStatus.SCHEDULED);
        missed.setScheduledPublishAt(now.minus(20, ChronoUnit.MINUTES));

        when(contentRepository.findByStatusAndScheduledPublishAtBetween(eq(ContentStatus.SCHEDULED), any(), any()))
                .thenReturn(List.of(current));
        when(contentRepository.findByStatusAndScheduledPublishAtBefore(eq(ContentStatus.SCHEDULED), any()))
                .thenReturn(List.of(missed));


        contentServiceImpl.processScheduledContent();

        verify(contentRepository, times(1)).save(argThat(content ->
                content.getId().equals("content1") && content.getStatus() == ContentStatus.PUBLISHED));

        verify(contentRepository, times(1)).save(argThat(content ->
                content.getId().equals("content2") && content.getStatus() == ContentStatus.PUBLISHED));

        verify(contentStatusAuditRepository, times(2)).save(any(ContentStatusAudit.class));
    }

    @Test
    @DisplayName("Should not publish anything when no scheduled content found")
    void testProcessScheduledContent_NoContent() {
        when(contentRepository.findByStatusAndScheduledPublishAtBetween(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        when(contentRepository.findByStatusAndScheduledPublishAtBefore(any(), any()))
                .thenReturn(Collections.emptyList());

        contentServiceImpl.processScheduledContent();

        verify(contentRepository, never()).save(any());
        verify(contentStatusAuditRepository, never()).save(any());
    }


    @Test
    @DisplayName("Should move content to bin")
    void testMoveToBin_Success() {
        // Mock repository
        when(contentRepository.findById(CONTENT_ID)).thenReturn(Optional.of(content));

        // Call the method
        contentServiceImpl.moveToBin(CONTENT_ID, USER_ID);

        // Verify interactions
        verify(contentRepository, times(1)).findById(CONTENT_ID);
        verify(contentRepository, times(1)).save(content);
        assertEquals(ContentStatus.DELETED, content.getStatus());
    }

    @Test
    @DisplayName("Should throw exception when moving already deleted content to bin")
    void testDeleteContent_AlreadyDeleted() {
        content.setStatus(ContentStatus.DELETED);
        when(contentRepository.findById(CONTENT_ID)).thenReturn(Optional.of(content));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> contentServiceImpl.moveToBin(CONTENT_ID, USER_ID));

        verify(contentRepository, times(1)).findById(CONTENT_ID);
        assertEquals("Content is already deleted", exception.getMessage());
        verify(contentRepository, never()).save(any(Content.class));
    }

    @Test
    @DisplayName("Should restore content from bin successfully")
    void testRestoreContent_Success() {
        content.setStatus(ContentStatus.DELETED);
        // Mock repository
        when(contentRepository.findByIdAndStatus(CONTENT_ID, ContentStatus.DELETED)).thenReturn(Optional.of(content));
        when(contentRepository.save(any(Content.class))).thenReturn(content);

        // Call the method
        contentServiceImpl.restoreContent(CONTENT_ID, USER_ID);

        // Verify interactions
        verify(contentRepository, times(1)).findByIdAndStatus(CONTENT_ID, ContentStatus.DELETED);
        verify(contentRepository, times(1)).save(content);

    }

    // Todo: Need improve the test case when deleteAssociatedMedia is ready
    @Test
    @DisplayName("Should permanently delete expired DELETED content older than 15 days")
    void testPermanentDeleteExpiredContent_SuccessfulPurge() {
        // Arrange
        Instant now = Instant.now();
        Instant deletedAt = now.minus(16, ChronoUnit.DAYS); // older than 15 days
        content.setStatus(ContentStatus.DELETED);
        content.setDeletedAt(deletedAt);
        List<Content> expiredContentList = List.of(content);

        when(contentRepository.findByStatusAndDeletedAtBefore(eq(ContentStatus.DELETED), any()))
                .thenReturn(expiredContentList);

        contentServiceImpl.permanentDeleteExpiredContent();

        verify(contentRepository, times(1)).findByStatusAndDeletedAtBefore(eq(ContentStatus.DELETED), any());
        verify(contentRepository, times(1)).deleteAll(expiredContentList);
    }

    @Test
    @DisplayName("Should return bin content")
    void testGetBinContent_Success() {
        content.setStatus(ContentStatus.DELETED);
        Page<Content> page = new PageImpl<>(List.of(content));
        when(contentRepository.findByOrgIdAndStatus(eq(ORG_ID), eq(ContentStatus.DELETED), any(Pageable.class)))
                .thenReturn(page);

        PaginatedResponse<Content> result = contentServiceImpl.getBinContent(ORG_ID, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(ContentStatus.DELETED, result.getContent().get(0).getStatus());
    }

    @Test
    @DisplayName("Should permanently delete content when status is DELETED")
    void testDeleteContent_WhenStatusIsDeleted_ShouldDeleteSuccessfully() {
        content.setStatus(ContentStatus.DELETED);

        when(contentRepository.findById(content.getId())).thenReturn(Optional.of(content));
        contentServiceImpl.deleteContent(content.getId());

        verify(contentRepository).delete(content);
        verify(contentRepository).findById(content.getId());
    }


    @Test
    @DisplayName("Should throw IllegalStateException if content status is not DELETED")
    void testDeleteContent_WhenStatusNotDeleted_ShouldThrow() {
        when(contentRepository.findById(content.getId())).thenReturn(Optional.of(content));

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> contentServiceImpl.deleteContent(content.getId()));

        assertEquals("Content is not deleted, cannot be permanently deleted", exception.getMessage());
        verify(contentRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should return list of content versions for given contentId")
    void testGetContentVersions_Success() {
        content.setVersion(2);
        ContentHistory history1 = new ContentHistory();
        history1.setContentSnapshot(content);
        history1.setCreatedAt(Instant.parse("2024-03-01T10:00:00Z"));
        history1.setChangeReason("First update");
        history1.setCreatedBy(new UserDetails("user1"));

        when(contentHistoryRepository.findByContentSnapshotId(content.getId()))
                .thenReturn(List.of(history1));

        List<ContentVersionDto> result = contentServiceImpl.getContentVersions(content.getId());

        // Assert
        assertEquals(1, result.size());
        assertEquals(2, result.getFirst().getVersion());
        verify(contentHistoryRepository, times(1)).findByContentSnapshotId(content.getId());
    }

    @Test
    @DisplayName("Should throw exception when no content versions found")
    void testGetContentVersions_ThrowsExceptionWhenEmpty() {
        // Arrange
        String contentId = "nonexistent";
        when(contentHistoryRepository.findByContentSnapshotId(contentId)).thenReturn(Collections.emptyList());

        // Act + Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> contentServiceImpl.getContentVersions(contentId));

        assertEquals("No versions found for contentId: nonexistent", exception.getMessage());
        verify(contentHistoryRepository).findByContentSnapshotId(contentId);
    }


    @Test
    @DisplayName("Should rollback content to a previous version")
    void testRollbackContent_Success() {
        content.setVersion(2);

        ContentHistory contentHistory = new ContentHistory(content, new UserDetails(USER_ID), "Test");
        when(contentRepository.findById(CONTENT_ID)).thenReturn(Optional.of(content));
        when(contentHistoryRepository.findByContentSnapshotIdAndContentSnapshotVersion(CONTENT_ID, 1))
                .thenReturn(Optional.of(contentHistory));
        when(contentRepository.save(any(Content.class))).thenReturn(content);

        Content rolledBackContent = contentServiceImpl.rollbackContent(
                CONTENT_ID, 1, USER_ID, EnumSet.of(RollbackField.TITLE));

        assertNotNull(rolledBackContent);
        assertEquals(3, rolledBackContent.getVersion()); // 2 → rollback → +1 → 3

        verify(contentHistoryRepository, times(1)).findByContentSnapshotIdAndContentSnapshotVersion(CONTENT_ID, 1);
        verify(contentRepository, times(1)).save(any(Content.class));
    }

    @Test
    @DisplayName("Should throw exception when rolling back non-existent version")
    void testRollbackContent_VersionNotFound() {
        content.setVersion(2); // ensure rollback is allowed

        when(contentRepository.findById(CONTENT_ID)).thenReturn(Optional.of(content));
        when(contentHistoryRepository.findByContentSnapshotIdAndContentSnapshotVersion(CONTENT_ID, 1))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> contentServiceImpl.rollbackContent(CONTENT_ID, 1, USER_ID, EnumSet.of(RollbackField.TITLE)));

        assertEquals("Content history not found for version: 1", exception.getMessage());
    }


    @Test
    @DisplayName("Should update content slug successfully")
    void testUpdateSlug_Success() {
        when(contentRepository.findById(CONTENT_ID)).thenReturn(Optional.of(content));
        when(contentRepository.save(any(Content.class))).thenReturn(content);

        Content updatedContent = contentServiceImpl.updateSlug(CONTENT_ID, "new-slug", USER_ID);

        assertNotNull(updatedContent);
        assertEquals("new-slug", updatedContent.getSlug());
        assertEquals(2, updatedContent.getVersion());

        verify(contentRepository, times(1)).findById(CONTENT_ID);
        verify(contentRepository, times(1)).save(any(Content.class));
    }

    @Test
    @DisplayName("Should throw exception when updating slug for non-existent content")
    void testUpdateSlug_NotFound() {
        when(contentRepository.findById(CONTENT_ID)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> contentServiceImpl.updateSlug(CONTENT_ID, "new-slug", USER_ID));

        assertEquals("Content not found with id: " + CONTENT_ID, exception.getMessage());
    }

    @Test
    @DisplayName("Should generate base slug when available")
    void testGenerateUniqueSlug_BaseSlugAvailable() {
        content.setTitle("Best SEO Practices");
        content.setDescription("Learn the best SEO practices for 2025");
        String baseSlug = "best-seo-practices";

        when(contentRepository.findById(content.getId())).thenReturn(Optional.of(content));
        when(slugGenerator.generateUniqueSlug(content.getTitle(), content.getDescription(), ORG_ID))
                .thenReturn(baseSlug);

        String result = contentServiceImpl.generateUniqueSlug(content.getId(), ORG_ID);

        assertEquals(baseSlug, result);
    }


    @Test
    @DisplayName("Should return true when slug is available")
    void testValidateSlug_WhenAvailable() {
        String slug = "available-slug";

        when(slugGenerator.isSlugAvailable(slug, ORG_ID, content.getId())).thenReturn(true);

        SlugValidationResponse response = contentServiceImpl.validateSlug(slug, ORG_ID, content.getId());

        assertTrue(response.isAvailable());
        assertTrue(response.getSuggestions().isEmpty());
    }

    @DisplayName("Should return false with suggestions when slug is taken")
    @Test
    void testValidateSlug_WhenNotAvailable() {
        String slug = "taken-slug";
        List<String> suggestions = List.of("taken-slug-1", "taken-slug-2");

        when(slugGenerator.isSlugAvailable(slug, ORG_ID, content.getId())).thenReturn(false);
        when(slugGenerator.generateSlugSuggestions(slug, ORG_ID)).thenReturn(suggestions);

        SlugValidationResponse response = contentServiceImpl.validateSlug(slug, ORG_ID, content.getId());

        assertFalse(response.isAvailable());
        assertEquals(suggestions, response.getSuggestions());
    }

    @Test
    @DisplayName("Should return list of content status audits for given contentId")
    void testGetStatusAuditForContent_Success() {
        ContentStatusAudit audit = new ContentStatusAudit();
        audit.setContentId(CONTENT_ID);
        audit.setOldStatus(ContentStatus.DRAFT);
        audit.setNewStatus(ContentStatus.APPROVED);
        audit.setChangedBy(new UserDetails(USER_ID));
        audit.setChangedAt(Instant.now());

        when(contentStatusAuditRepository.findByContentId(CONTENT_ID)).thenReturn(List.of(audit));

        List<ContentStatusAudit> result = contentServiceImpl.getStatusAuditForContent(CONTENT_ID);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(CONTENT_ID, result.get(0).getContentId());
    }

}
