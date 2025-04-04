package com.smartcms.smartcontent.dto;

import com.smartcms.smartcommon.model.AIInsights;
import com.smartcms.smartcommon.model.ContentType;
import com.smartcms.smartcommon.model.MediaReference;
import com.smartcms.smartcommon.model.MetaData;
import lombok.*;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContentRequest {
    private String title;
    private String description;
    private String body;
    private List<String> tags;
    private ContentType contentType;
    private MetaData meta;
    private List<MediaReference> mediaReferences;
    private AIInsights aiInsights;
}
