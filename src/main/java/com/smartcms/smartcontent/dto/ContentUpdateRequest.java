package com.smartcms.smartcontent.dto;


import com.smartcms.smartcommon.model.MediaReference;
import com.smartcms.smartcommon.model.MetaData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContentUpdateRequest {
    private String title;
    private String description;
    private String body;
    private List<String> tags;
    private MetaData meta;
    private List<MediaReference> mediaReferences;
}

