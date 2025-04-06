package com.smartcms.smartcontent.repository;

import com.smartcms.smartcommon.model.Content;
import com.smartcms.smartcommon.model.ContentStatus;
import com.smartcms.smartcommon.model.OrgDetails;
import com.smartcms.smartcontent.model.ContentHistory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
@DataMongoTest
@ActiveProfiles("test")
class ContentHistoryRepositoryTest {

    @Autowired
    private ContentHistoryRepository contentHistoryRepository;

    private String contentId;
    private ContentHistory version1;
    private ContentHistory version2;

    @BeforeEach
    void setUp() {
        contentHistoryRepository.deleteAll();

        contentId = UUID.randomUUID().toString();

        Content snapshot1 = new Content();
        snapshot1.setId(contentId);
        snapshot1.setTitle("First Version");
        snapshot1.setVersion(1);
        snapshot1.setStatus(ContentStatus.DRAFT);
        snapshot1.setOrgDetails(new OrgDetails("org-123"));
        snapshot1.setCreatedAt(Instant.now());
        snapshot1.setUpdatedAt(Instant.now());

        version1 = new ContentHistory();
        version1.setId(UUID.randomUUID().toString());
        version1.setContentSnapshot(snapshot1);

        Content snapshot2 = new Content();
        snapshot2.setId(contentId);
        snapshot2.setTitle("Second Version");
        snapshot2.setVersion(2);
        snapshot2.setStatus(ContentStatus.PUBLISHED);
        snapshot2.setOrgDetails(new OrgDetails("org-123"));
        snapshot2.setCreatedAt(Instant.now());
        snapshot2.setUpdatedAt(Instant.now());

        version2 = new ContentHistory();
        version2.setId(UUID.randomUUID().toString());
        version2.setContentSnapshot(snapshot2);

        contentHistoryRepository.saveAll(List.of(version1, version2));
    }

    @Test
    void findByContentSnapshotId_shouldReturnAllVersions() {
        List<ContentHistory> histories = contentHistoryRepository.findByContentSnapshotId(contentId);
        assertThat(histories).hasSize(2);
        assertThat(histories).extracting(h -> h.getContentSnapshot().getVersion())
                .containsExactlyInAnyOrder(1, 2);
    }

    @Test
    void findByContentSnapshotIdAndContentSnapshotVersion_shouldReturnSpecificVersion() {
        Optional<ContentHistory> result = contentHistoryRepository.findByContentSnapshotIdAndContentSnapshotVersion(contentId, 1);
        assertThat(result).isPresent();
        assertThat(result.get().getContentSnapshot().getTitle()).isEqualTo("First Version");
    }

    @Test
    void findByContentSnapshotIdAndContentSnapshotVersion_shouldReturnEmptyIfNotFound() {
        Optional<ContentHistory> result = contentHistoryRepository.findByContentSnapshotIdAndContentSnapshotVersion(contentId, 99);
        assertThat(result).isNotPresent();
    }
}
