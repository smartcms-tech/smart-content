package com.smartcms.smartcontent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcms.smartcommon.model.Content;
import com.smartcms.smartcommon.model.ContentStatus;
import com.smartcms.smartcommon.model.ContentType;
import com.smartcms.smartcommon.model.UserDetails;
import com.smartcms.smartcontent.dto.*;
import com.smartcms.smartcontent.model.ContentStatusAudit;
import com.smartcms.smartcontent.model.PaginatedResponse;
import com.smartcms.smartcontent.model.RollbackField;
import com.smartcms.smartcontent.service.ContentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ContentControllerTest {

    @Mock
    private ContentServiceImpl contentService;

    @InjectMocks
    private ContentController contentController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private Content testContent;
    private ContentRequest contentRequest;
    private ContentUpdateRequest updateRequest;
    private final String USER_ID = "user123";
    private final String ORG_ID = "org456";
    private final String CONTENT_ID = "content789";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(contentController).build();
        objectMapper = new ObjectMapper();

        // Set up test content
        testContent = Content.builder()
                .id(CONTENT_ID)
                .title("Test Title")
                .slug("test-title")
                .description("Test Description")
                .body("Test Body Content")
                .contentType(ContentType.ARTICLE)
                .status(ContentStatus.DRAFT)
                .version(1)
                .build();

        // Set up content request
        contentRequest = ContentRequest.builder()
                .title("Test Title")
                .description("Test Description")
                .body("Test Body Content")
                .contentType(ContentType.ARTICLE)
                .tags(List.of("tag1", "tag2"))
                .build();

        // Set up update request
        updateRequest = ContentUpdateRequest.builder()
                .title("Updated Title")
                .description("Updated Description")
                .body("Updated Body Content")
                .tags(List.of("updatedTag1", "updatedTag2"))
                .build();
    }

    @Test
    @DisplayName("Should create content successfully")
    void testCreateContent() throws Exception {
        when(contentService.createContent(any(ContentRequest.class), eq(USER_ID), eq(ORG_ID)))
                .thenReturn(testContent);

        mockMvc.perform(post("/api/v1/content/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", USER_ID)
                        .header("X-Org-Id", ORG_ID)
                        .content(objectMapper.writeValueAsString(contentRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(CONTENT_ID)))
                .andExpect(jsonPath("$.title", is("Test Title")));

        verify(contentService).createContent(any(ContentRequest.class), eq(USER_ID), eq(ORG_ID));
    }

    @Test
    @DisplayName("Should get content by ID")
    void testGetContentById() throws Exception {
        when(contentService.getContentById(CONTENT_ID)).thenReturn(testContent);

        mockMvc.perform(get("/api/v1/content/{contentId}", CONTENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(CONTENT_ID)))
                .andExpect(jsonPath("$.title", is("Test Title")));

        verify(contentService).getContentById(CONTENT_ID);
    }

    @Test
    @DisplayName("Should list organization content")
    void testListOrgContent() throws Exception {
        List<Content> contentList = List.of(testContent);
        PaginatedResponse<Content> paginatedResponse = new PaginatedResponse<>(
                contentList, 1, 0, 20, 1, true
        );

        when(contentService.getOrgContent(eq(ORG_ID), anyInt(), anyInt(), anyString()))
                .thenReturn(paginatedResponse);

        mockMvc.perform(get("/api/v1/content/org")
                        .header("X-Org-Id", ORG_ID)
                        .param("page", "0")
                        .param("size", "20")
                        .param("sortBy", "title"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(CONTENT_ID)));

        verify(contentService).getOrgContent(eq(ORG_ID), eq(0), eq(20), eq("title"));
    }

    @Test
    @DisplayName("Should update content")
    void testUpdateContent() throws Exception {
        Content updatedContent = Content.builder()
                .id(CONTENT_ID)
                .title("Updated Title")
                .description("Updated Description")
                .body("Updated Body Content")
                .version(2)
                .build();

        when(contentService.updateContent(eq(CONTENT_ID), any(ContentUpdateRequest.class), eq(USER_ID)))
                .thenReturn(updatedContent);

        mockMvc.perform(patch("/api/v1/content/{contentId}", CONTENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", USER_ID)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Updated Title")))
                .andExpect(jsonPath("$.version", is(2)));

        verify(contentService).updateContent(eq(CONTENT_ID), any(ContentUpdateRequest.class), eq(USER_ID));
    }

    @Test
    @DisplayName("Should list content by status")
    void testListContentByStatus() throws Exception {
        List<Content> contentList = List.of(testContent);
        PaginatedResponse<Content> paginatedResponse = new PaginatedResponse<>(
                contentList, 1, 0, 20, 1, true
        );

        when(contentService.getContentByStatus(eq(ORG_ID), eq(ContentStatus.DRAFT), anyInt(), anyInt()))
                .thenReturn(paginatedResponse);

        mockMvc.perform(get("/api/v1/content/status")
                        .header("X-Org-Id", ORG_ID)
                        .param("status", "DRAFT")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].status", is("DRAFT")));

        verify(contentService).getContentByStatus(eq(ORG_ID), eq(ContentStatus.DRAFT), eq(0), eq(20));
    }

    @Test
    @DisplayName("Should update content status")
    void testUpdateContentStatus() throws Exception {
        ContentStatusUpdateRequest statusUpdateRequest = new ContentStatusUpdateRequest(
                ContentStatus.PUBLISHED, "Ready for publication"
        );

        Content publishedContent = Content.builder()
                .id(CONTENT_ID)
                .title("Test Title")
                .status(ContentStatus.PUBLISHED)
                .build();

        when(contentService.updateStatus(
                eq(CONTENT_ID), eq(ContentStatus.PUBLISHED), eq(USER_ID), eq("Ready for publication")))
                .thenReturn(publishedContent);

        mockMvc.perform(patch("/api/v1/content/{contentId}/status", CONTENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", USER_ID)
                        .content(objectMapper.writeValueAsString(statusUpdateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PUBLISHED")));

        verify(contentService).updateStatus(
                eq(CONTENT_ID), eq(ContentStatus.PUBLISHED), eq(USER_ID), eq("Ready for publication"));
    }

    @Test
    @DisplayName("Should schedule content publishing")
    void testScheduleContent() throws Exception {
        Instant publishTime = Instant.now().plus(1, ChronoUnit.DAYS);

        Content scheduledContent = Content.builder()
                .id(CONTENT_ID)
                .title("Test Title")
                .status(ContentStatus.SCHEDULED)
                .scheduledPublishAt(publishTime)
                .build();

        when(contentService.schedulePublishing(eq(CONTENT_ID), any(Instant.class), eq(USER_ID)))
                .thenReturn(scheduledContent);

        mockMvc.perform(patch("/api/v1/content/{contentId}/schedule", CONTENT_ID)
                        .header("X-User-Id", USER_ID)
                        .param("publishTime", publishTime.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SCHEDULED")));

        verify(contentService).schedulePublishing(eq(CONTENT_ID), any(Instant.class), eq(USER_ID));
    }

    @Test
    @DisplayName("Should rollback content to previous version")
    void testRollbackContent() throws Exception {
        Set<RollbackField> fieldsToRollback = Set.of(RollbackField.TITLE, RollbackField.BODY);

        Content rolledBackContent = Content.builder()
                .id(CONTENT_ID)
                .title("Previous Title")
                .version(3)
                .build();

        when(contentService.rollbackContent(eq(CONTENT_ID), eq(1), eq(USER_ID), anySet()))
                .thenReturn(rolledBackContent);

        mockMvc.perform(post("/api/v1/content/{contentId}/rollback", CONTENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", USER_ID)
                        .param("version", "1")
                        .content(objectMapper.writeValueAsString(fieldsToRollback)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Previous Title")))
                .andExpect(jsonPath("$.version", is(3)));

        verify(contentService).rollbackContent(eq(CONTENT_ID), eq(1), eq(USER_ID), anySet());
    }

    @Test
    @DisplayName("Should list content versions")
    void testListContentVersions() throws Exception {
        List<ContentVersionDto> versions = List.of(
                new ContentVersionDto(1, USER_ID, Instant.now().minus(2, ChronoUnit.DAYS), "Initial version"),
                new ContentVersionDto(2, USER_ID, Instant.now().minus(1, ChronoUnit.DAYS), "Updated content")
        );

        when(contentService.getContentVersions(CONTENT_ID)).thenReturn(versions);

        mockMvc.perform(get("/api/v1/content/{contentId}/versions", CONTENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].version", is(1)))
                .andExpect(jsonPath("$[1].version", is(2)));

        verify(contentService).getContentVersions(CONTENT_ID);
    }

    @Test
    @DisplayName("Should list bin content")
    void testListBinContent() throws Exception {
        Content deletedContent = Content.builder()
                .id(CONTENT_ID)
                .title("Deleted Content")
                .status(ContentStatus.DELETED)
                .build();

        List<Content> contentList = List.of(deletedContent);
        PaginatedResponse<Content> paginatedResponse = new PaginatedResponse<>(
                contentList, 1, 0, 20, 1, true
        );

        when(contentService.getBinContent(eq(ORG_ID), anyInt(), anyInt()))
                .thenReturn(paginatedResponse);

        mockMvc.perform(get("/api/v1/content/bin")
                        .header("X-Org-Id", ORG_ID)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].status", is("DELETED")));

        verify(contentService).getBinContent(eq(ORG_ID), eq(0), eq(20));
    }

    @Test
    @DisplayName("Should move content to bin")
    void testMoveToBin() throws Exception {
        doNothing().when(contentService).moveToBin(CONTENT_ID, USER_ID);

        mockMvc.perform(delete("/api/v1/content/{contentId}", CONTENT_ID)
                        .header("X-Org-Id", USER_ID))
                .andExpect(status().isNoContent());

        verify(contentService).moveToBin(CONTENT_ID, USER_ID);
    }

    @Test
    @DisplayName("Should restore content from bin")
    void testRestoreContent() throws Exception {
        Content restoredContent = Content.builder()
                .id(CONTENT_ID)
                .title("Restored Content")
                .status(ContentStatus.DRAFT)
                .build();

        when(contentService.restoreContent(CONTENT_ID, USER_ID)).thenReturn(restoredContent);

        mockMvc.perform(post("/api/v1/content/bin/{contentId}/restore", CONTENT_ID)
                        .header("X-Org-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("DRAFT")));

        verify(contentService).restoreContent(CONTENT_ID, USER_ID);
    }

    @Test
    @DisplayName("Should permanently delete content")
    void testDeleteContent() throws Exception {
        doNothing().when(contentService).deleteContent(CONTENT_ID);

        mockMvc.perform(delete("/api/v1/content/bin/{contentId}", CONTENT_ID)
                        .header("X-Org-Id", USER_ID))
                .andExpect(status().isNoContent());

        verify(contentService).deleteContent(CONTENT_ID);
    }

    @Test
    @DisplayName("Should update content slug")
    void testUpdateSlug() throws Exception {
        Content contentWithNewSlug = Content.builder()
                .id(CONTENT_ID)
                .title("Test Title")
                .slug("new-slug")
                .build();

        when(contentService.updateSlug(eq(CONTENT_ID), eq("new-slug"), eq(USER_ID)))
                .thenReturn(contentWithNewSlug);

        mockMvc.perform(patch("/api/v1/content/{contentId}/update-slug", CONTENT_ID)
                        .header("X-User-Id", USER_ID)
                        .param("newSlug", "new-slug"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug", is("new-slug")));

        verify(contentService).updateSlug(CONTENT_ID, "new-slug", USER_ID);
    }

    @Test
    @DisplayName("Should validate slug")
    void testValidateSlug() throws Exception {
        SlugValidationResponse validationResponse = new SlugValidationResponse(true, List.of());

        when(contentService.validateSlug(eq("test-slug"), eq(ORG_ID), eq(CONTENT_ID)))
                .thenReturn(validationResponse);

        mockMvc.perform(get("/api/v1/content/{contentId}/validate-slug", CONTENT_ID)
                        .header("X-Org-ID", ORG_ID)
                        .param("slug", "test-slug"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available", is(true)))
                .andExpect(jsonPath("$.suggestions", hasSize(0)));

        verify(contentService).validateSlug("test-slug", ORG_ID, CONTENT_ID);
    }

    @Test
    @DisplayName("Should generate unique slug")
    void testGenerateSlug() throws Exception {
        when(contentService.generateUniqueSlug(eq(CONTENT_ID), eq(ORG_ID)))
                .thenReturn("generated-unique-slug");

        mockMvc.perform(post("/api/v1/content/{contentId}/generate-slug", CONTENT_ID)
                        .header("X-Org-ID", ORG_ID))
                .andExpect(status().isOk())
                .andExpect(content().string("generated-unique-slug"));

        verify(contentService).generateUniqueSlug(CONTENT_ID, ORG_ID);
    }

    @Test
    @DisplayName("Should get content status audit")
    void testGetContentStatusAudit() throws Exception {
        List<ContentStatusAudit> auditList = List.of(
                new ContentStatusAudit("audit-id-1", CONTENT_ID, ContentStatus.DRAFT, ContentStatus.APPROVED, new UserDetails(USER_ID), Instant.now(), "Content approved"),
                new ContentStatusAudit("audit-id-2", CONTENT_ID, ContentStatus.APPROVED, ContentStatus.PUBLISHED, new UserDetails(USER_ID), Instant.now(), "Content Published")
        );

        when(contentService.getStatusAuditForContent(CONTENT_ID)).thenReturn(auditList);

        mockMvc.perform(get("/api/v1/content/audit/{contentId}", CONTENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].contentId", is(CONTENT_ID)))
                .andExpect(jsonPath("$[0].oldStatus", is("DRAFT")));

        verify(contentService).getStatusAuditForContent(CONTENT_ID);
    }
}