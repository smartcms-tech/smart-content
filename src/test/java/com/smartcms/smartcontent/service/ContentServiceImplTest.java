package com.smartcms.smartcontent.service;

import com.smartcms.smartcommon.exception.ResourceNotFoundException;
import com.smartcms.smartcommon.model.*;
import com.smartcms.smartcontent.dto.ContentRequest;
import com.smartcms.smartcontent.dto.ContentUpdateRequest;

import com.smartcms.smartcontent.model.*;
import com.smartcms.smartcontent.repository.ContentHistoryRepository;
import com.smartcms.smartcontent.repository.ContentRepository;
import com.smartcms.smartcontent.utility.SlugGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
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
        verify(contentRepository, times(1)).findByOrgIdAndStatusNot(ORG_ID, ContentStatus.DELETED, Pageable.unpaged());
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
        assertEquals(0, response.getTotalPages());
        verify(contentRepository, times(1)).findByOrgIdAndStatusNot(ORG_ID, ContentStatus.DELETED, Pageable.unpaged());
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
    @DisplayName("Should update content status successfully")
    void testUpdateContentStatus_Success() {
        when(contentRepository.findById(CONTENT_ID)).thenReturn(Optional.of(content));
        when(contentRepository.save(any(Content.class))).thenReturn(content);

        Content updatedContent = contentServiceImpl.updateStatus(CONTENT_ID, ContentStatus.PUBLISHED, USER_ID);

        assertNotNull(updatedContent);
        assertEquals(ContentStatus.PUBLISHED, updatedContent.getStatus());
        assertEquals(2, updatedContent.getVersion());

        verify(contentRepository, times(1)).save(any(Content.class));
    }

    @Test
    @DisplayName("Should throw exception when updating status for non-existent content")
    void testUpdateContentStatus_NotFound() {
        when(contentRepository.findById(CONTENT_ID)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> contentServiceImpl.updateStatus(CONTENT_ID, ContentStatus.PUBLISHED, USER_ID));

        assertEquals("Content not found with id: " + CONTENT_ID, exception.getMessage());
        verify(contentRepository, times(1)).findById(CONTENT_ID);
    }

    @Test
    @DisplayName("Should throw exception when updating status to invalid transition")
    void testUpdateContentStatus_InvalidTransition() {
        when(contentRepository.findById(CONTENT_ID)).thenReturn(Optional.of(content));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> contentServiceImpl.updateStatus(CONTENT_ID, ContentStatus.ARCHIVED, USER_ID));

        assertEquals("Invalid status transition from DRAFT to ARCHIVED", exception.getMessage());
        verify(contentRepository, never()).save(any(Content.class));
    }


    @Test
    void testDeleteContent_Success() {
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
    void testRestoreContent_Success() {
        content.setStatus(ContentStatus.DELETED);
        // Mock repository
        when(contentRepository.findById(CONTENT_ID)).thenReturn(Optional.of(content));

        // Call the method
        contentServiceImpl.restoreContent(CONTENT_ID, USER_ID);

        // Verify interactions
        verify(contentRepository, times(1)).findById(CONTENT_ID);
        verify(contentRepository, times(1)).save(content);

    }


    @Test
    @DisplayName("Should rollback content to a previous version")
    void testRollbackContent_Success() {
        ContentHistory contentHistory = new ContentHistory(content, new UserDetails(USER_ID), "Test");
        when(contentRepository.findById(CONTENT_ID)).thenReturn(Optional.of(content));
        when(contentHistoryRepository.findByContentSnapshotIdAndContentSnapshotVersion(CONTENT_ID, 1))
                .thenReturn(Optional.of(contentHistory));
        when(contentRepository.save(any(Content.class))).thenReturn(content);

        Content rolledBackContent = contentServiceImpl.rollbackContent(CONTENT_ID, 1, USER_ID, EnumSet.of(RollbackField.TITLE));

        assertNotNull(rolledBackContent);
        assertEquals(2, rolledBackContent.getVersion());

        verify(contentHistoryRepository, times(1)).findByContentSnapshotIdAndContentSnapshotVersion(CONTENT_ID, 1);
        verify(contentRepository, times(1)).save(any(Content.class));
    }

    @Test
    @DisplayName("Should throw exception when rolling back non-existent version")
    void testRollbackContent_VersionNotFound() {
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
}
