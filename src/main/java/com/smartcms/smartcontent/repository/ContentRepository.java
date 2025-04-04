package com.smartcms.smartcontent.repository;


import com.smartcms.smartcommon.model.Content;
import com.smartcms.smartcommon.model.ContentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContentRepository extends MongoRepository<Content, String> {

    List<Content> findByTagsContaining(String tag);

    List<Content> findByAuthor(String userId);

    List<Content> findByStatusAndDeletedAtBefore(ContentStatus contentStatus, Instant expirationTime);

    @Query("{ 'status': ?0, 'scheduledPublishAt': { $gte: ?1, $lte: ?2 } }")
    List<Content> findByStatusAndScheduledPublishAtBetween(ContentStatus status, Instant startTime, Instant endTime);

    @Query("{ 'status': ?0, 'scheduledPublishAt': { $lt: ?1 } }")
    List<Content> findByStatusAndScheduledPublishAtBefore(ContentStatus status, Instant cutoffTime);

    @Query("{ '_id': ?0, 'status': ?1 }")
    Optional<Content> findByIdAndStatus(String id, ContentStatus contentStatus);

    @Query("{ 'orgDetails.orgId': ?0, 'status': ?1 }")
    Page<Content> findByOrgIdAndStatus(String orgId, ContentStatus status, Pageable pageable);

    @Query("{ 'orgDetails.orgId': ?0, 'status': { $ne: ?1 } }")
    Page<Content> findByOrgIdAndStatusNot(String orgId, ContentStatus status, Pageable pageable);
}