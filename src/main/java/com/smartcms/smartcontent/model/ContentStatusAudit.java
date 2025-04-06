package com.smartcms.smartcontent.model;

import com.smartcms.smartcommon.model.ContentStatus;
import com.smartcms.smartcommon.model.UserDetails;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "content_status_audit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentStatusAudit {

    @Id
    private String id;

    private String contentId;

    private ContentStatus oldStatus;
    private ContentStatus newStatus;

    private UserDetails changedBy;
    private Instant changedAt;

    private String note;
}

