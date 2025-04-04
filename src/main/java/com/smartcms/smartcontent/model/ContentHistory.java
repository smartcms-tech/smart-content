package com.smartcms.smartcontent.model;

import com.smartcms.smartcommon.model.Content;
import com.smartcms.smartcommon.model.UserDetails;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "content_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentHistory {

    public ContentHistory(Content contentSnapshot, UserDetails createdBy, String changeReason) {
        this.contentSnapshot = contentSnapshot;
        this.createdBy = createdBy;
        this.changeReason = changeReason;
    }

    @Id
    private String id;
    private Content contentSnapshot;
    private Instant createdAt;
    private UserDetails createdBy;
    private String changeReason;
}
