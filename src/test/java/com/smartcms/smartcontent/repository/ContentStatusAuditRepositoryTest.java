package com.smartcms.smartcontent.repository;

import com.smartcms.smartcommon.model.ContentStatus;
import com.smartcms.smartcommon.model.UserDetails;
import com.smartcms.smartcontent.model.ContentStatusAudit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class ContentStatusAuditRepositoryTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private ContentStatusAuditRepository repository;

    private static final String CONTENT_ID = "content-123";
    private static final String USER_ID = "user-123";

    @BeforeEach
    void setUp() {
        repository.deleteAll(); // 🧹 Clear old data before each test
    }

    @Test
    @DisplayName("Should return audits by contentId")
    void testFindByContentId() {
        // given
        ContentStatusAudit audit1 = ContentStatusAudit.builder()
                .contentId(CONTENT_ID)
                .oldStatus(ContentStatus.DRAFT)
                .newStatus(ContentStatus.APPROVED)
                .changedBy(new UserDetails(USER_ID))
                .changedAt(Instant.now())
                .note("Approved by reviewer")
                .build();

        ContentStatusAudit audit2 = ContentStatusAudit.builder()
                .contentId(CONTENT_ID)
                .oldStatus(ContentStatus.APPROVED)
                .newStatus(ContentStatus.PUBLISHED)
                .changedBy(new UserDetails(USER_ID))
                .changedAt(Instant.now())
                .note("Published by admin")
                .build();

        repository.saveAll(List.of(audit1, audit2));

        // when
        List<ContentStatusAudit> result = repository.findByContentId(CONTENT_ID);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getContentId()).isEqualTo(CONTENT_ID);
        assertThat(result.get(1).getNewStatus()).isEqualTo(ContentStatus.PUBLISHED);
    }
}
