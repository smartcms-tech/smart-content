package com.smartcms.smartcontent.repository;

import com.smartcms.smartcontent.model.ContentHistory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContentHistoryRepository extends MongoRepository<ContentHistory, String> {
    List<ContentHistory> findByContentSnapshotId(String contentId);
    Optional<ContentHistory> findByContentSnapshotIdAndContentSnapshotVersion(String contentId, int version);
}
