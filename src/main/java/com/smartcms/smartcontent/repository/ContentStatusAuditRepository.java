package com.smartcms.smartcontent.repository;

import com.smartcms.smartcontent.model.ContentStatusAudit;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ContentStatusAuditRepository extends MongoRepository<ContentStatusAudit, String> {
    List<ContentStatusAudit> findByContentId(String contentId);
}
