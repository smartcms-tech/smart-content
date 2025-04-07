package com.smartcms.smartcontent.repository;

import com.smartcms.smartcommon.model.Content;
import com.smartcms.smartcommon.model.ContentStatus;
import com.smartcms.smartcommon.model.OrgDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
public class ContentRepositoryTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private ContentRepository contentRepository;

    private Content publishedContent;
    private Content draftContent;
    private Content scheduledContent;
    private Content deletedContent;
    private final String orgId = "test-org-123";

    @BeforeEach
    void setUp() {
        // Clear repository before each test
        contentRepository.deleteAll();

        // Create test data
        Instant now = Instant.now();

        // Create a published content
        publishedContent = new Content();
        publishedContent.setId(UUID.randomUUID().toString());
        publishedContent.setTitle("Published Content");
        publishedContent.setStatus(ContentStatus.PUBLISHED);
        publishedContent.setSlug("published-content");
        OrgDetails publishedOrgDetails = new OrgDetails();
        publishedOrgDetails.setOrgId(orgId);
        publishedContent.setOrgDetails(publishedOrgDetails);
        publishedContent.setCreatedAt(now.minus(5, ChronoUnit.DAYS));
        publishedContent.setUpdatedAt(now.minus(2, ChronoUnit.DAYS));

        // Create a draft content
        draftContent = new Content();
        draftContent.setId(UUID.randomUUID().toString());
        draftContent.setTitle("Draft Content");
        draftContent.setStatus(ContentStatus.DRAFT);
        draftContent.setSlug("draft-content");
        OrgDetails draftOrgDetails = new OrgDetails();
        draftOrgDetails.setOrgId(orgId);
        draftContent.setOrgDetails(draftOrgDetails);
        draftContent.setCreatedAt(now.minus(3, ChronoUnit.DAYS));
        draftContent.setUpdatedAt(now);

        // Create a scheduled content
        scheduledContent = new Content();
        scheduledContent.setId(UUID.randomUUID().toString());
        scheduledContent.setTitle("Scheduled Content");
        scheduledContent.setStatus(ContentStatus.SCHEDULED);
        scheduledContent.setSlug("scheduled-content");
        scheduledContent.setScheduledPublishAt(now.plus(1, ChronoUnit.DAYS));
        OrgDetails scheduledOrgDetails = new OrgDetails();
        scheduledOrgDetails.setOrgId(orgId);
        scheduledContent.setOrgDetails(scheduledOrgDetails);
        scheduledContent.setCreatedAt(now.minus(1, ChronoUnit.DAYS));
        scheduledContent.setUpdatedAt(now);

        // Create a deleted content with expiration time
        deletedContent = new Content();
        deletedContent.setId(UUID.randomUUID().toString());
        deletedContent.setTitle("Deleted Content");
        deletedContent.setStatus(ContentStatus.DELETED);
        deletedContent.setSlug("deleted-content");
        deletedContent.setDeletedAt(now.minus(10, ChronoUnit.DAYS));
        OrgDetails deletedOrgDetails = new OrgDetails();
        deletedOrgDetails.setOrgId(orgId);
        deletedContent.setOrgDetails(deletedOrgDetails);
        deletedContent.setCreatedAt(now.minus(15, ChronoUnit.DAYS));
        deletedContent.setUpdatedAt(now.minus(10, ChronoUnit.DAYS));

        // Save all test content
        contentRepository.saveAll(Arrays.asList(publishedContent, draftContent, scheduledContent, deletedContent));
    }

    @Test
    void findByStatusAndDeletedAtBefore() {
        // Given
        Instant expirationTime = Instant.now();

        // When
        List<Content> expiredContent = contentRepository.findByStatusAndDeletedAtBefore(
                ContentStatus.DELETED, expirationTime);

        // Then
        assertThat(expiredContent).isNotEmpty();
        assertThat(expiredContent).hasSize(1);
        assertThat(expiredContent.get(0).getId()).isEqualTo(deletedContent.getId());
        assertThat(expiredContent.get(0).getDeletedAt()).isBefore(expirationTime);
    }

    @Test
    void findByStatusAndScheduledPublishAtBetween() {
        // Given
        Instant startTime = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant endTime = Instant.now().plus(2, ChronoUnit.DAYS);

        // When
        List<Content> scheduledContents = contentRepository.findByStatusAndScheduledPublishAtBetween(
                ContentStatus.SCHEDULED, startTime, endTime);

        // Then
        assertThat(scheduledContents).isNotEmpty();
        assertThat(scheduledContents).hasSize(1);
        assertThat(scheduledContents.get(0).getId()).isEqualTo(scheduledContent.getId());
        assertThat(scheduledContents.get(0).getScheduledPublishAt()).isAfter(startTime);
        assertThat(scheduledContents.get(0).getScheduledPublishAt()).isBefore(endTime);
    }

    @Test
    void findByStatusAndScheduledPublishAtBefore() {
        // Given
        Instant pastScheduleTime = Instant.now().minus(1, ChronoUnit.DAYS);
        Content pastScheduledContent = new Content();
        pastScheduledContent.setId(UUID.randomUUID().toString());
        pastScheduledContent.setTitle("Past Scheduled Content");
        pastScheduledContent.setStatus(ContentStatus.SCHEDULED);
        pastScheduledContent.setScheduledPublishAt(pastScheduleTime);
        OrgDetails orgDetails = new OrgDetails();
        orgDetails.setOrgId(orgId);
        pastScheduledContent.setOrgDetails(orgDetails);
        contentRepository.save(pastScheduledContent);

        Instant cutoffTime = Instant.now();

        // When
        List<Content> readyToPublishContent = contentRepository.findByStatusAndScheduledPublishAtBefore(
                ContentStatus.SCHEDULED, cutoffTime);

        // Then
        assertThat(readyToPublishContent).isNotEmpty();
        assertThat(readyToPublishContent).hasSize(1);
        assertThat(readyToPublishContent.get(0).getId()).isEqualTo(pastScheduledContent.getId());
        assertThat(readyToPublishContent.get(0).getScheduledPublishAt()).isBefore(cutoffTime);
    }

    @Test
    void findByIdAndStatus() {
        // Given
        String contentId = publishedContent.getId();
        ContentStatus status = ContentStatus.PUBLISHED;

        // When
        Optional<Content> foundContent = contentRepository.findByIdAndStatus(contentId, status);

        // Then
        assertThat(foundContent).isPresent();
        assertThat(foundContent.get().getId()).isEqualTo(contentId);
        assertThat(foundContent.get().getStatus()).isEqualTo(status);

        // Test for content with wrong status
        Optional<Content> notFoundContent = contentRepository.findByIdAndStatus(contentId, ContentStatus.DRAFT);
        assertThat(notFoundContent).isEmpty();
    }

    @Test
    void findByOrgIdAndStatus() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Content> publishedPages = contentRepository.findByOrgIdAndStatus(orgId, ContentStatus.PUBLISHED, pageable);
        Page<Content> draftPages = contentRepository.findByOrgIdAndStatus(orgId, ContentStatus.DRAFT, pageable);

        // Then
        assertThat(publishedPages.getTotalElements()).isEqualTo(1);
        assertThat(publishedPages.getContent().get(0).getId()).isEqualTo(publishedContent.getId());

        assertThat(draftPages.getTotalElements()).isEqualTo(1);
        assertThat(draftPages.getContent().get(0).getId()).isEqualTo(draftContent.getId());
    }

    @Test
    void findByOrgIdAndStatusNot() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Content> nonDeletedContent = contentRepository.findByOrgIdAndStatusNot(
                orgId, ContentStatus.DELETED, pageable);

        // Then
        assertThat(nonDeletedContent.getTotalElements()).isEqualTo(3);
        assertThat(nonDeletedContent.getContent()).extracting("status")
                .doesNotContain(ContentStatus.DELETED);
    }

    @Test
    void notExistsBySlugAndOrgDetails_OrgIdAndStatus() {
        // Given
        String newSlug = "new-unique-slug";
        String existingSlug = publishedContent.getSlug();

        // When & Then
        assertThat(contentRepository.existsBySlugAndOrgDetails_OrgIdAndStatus(
                newSlug, orgId, ContentStatus.PUBLISHED)).isFalse();

        assertThat(contentRepository.existsBySlugAndOrgDetails_OrgIdAndStatus(
                existingSlug, orgId, ContentStatus.PUBLISHED)).isTrue();
    }

    @Test
    void existsBySlugAndOrgDetails_OrgIdAndStatusAndIdNot() {
        // Given
        String existingSlug = publishedContent.getSlug();
        String anotherContentId = draftContent.getId();
        String sameContentId = publishedContent.getId();

        // When & Then
        // Should return true when the slug exists for another content with the same status and org
        assertThat(contentRepository.existsBySlugAndOrgDetails_OrgIdAndStatusAndIdNot(
                existingSlug, orgId, ContentStatus.PUBLISHED, anotherContentId)).isTrue();

        // Should return false when checking against the same content's ID
        assertThat(contentRepository.existsBySlugAndOrgDetails_OrgIdAndStatusAndIdNot(
                existingSlug, orgId, ContentStatus.PUBLISHED, sameContentId)).isFalse();

        // Create a second published content with the same slug but different org
        Content sameSlugDifferentOrg = new Content();
        sameSlugDifferentOrg.setId(UUID.randomUUID().toString());
        sameSlugDifferentOrg.setTitle("Same Slug Different Org");
        sameSlugDifferentOrg.setStatus(ContentStatus.PUBLISHED);
        sameSlugDifferentOrg.setSlug(existingSlug);
        OrgDetails differentOrgDetails = new OrgDetails();
        differentOrgDetails.setOrgId("different-org-id");
        sameSlugDifferentOrg.setOrgDetails(differentOrgDetails);
        contentRepository.save(sameSlugDifferentOrg);

        // Should return false when checking for a different org
        assertThat(contentRepository.existsBySlugAndOrgDetails_OrgIdAndStatusAndIdNot(
                existingSlug, "different-org-id", ContentStatus.PUBLISHED, anotherContentId)).isTrue();
    }
}