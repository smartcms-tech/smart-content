package com.smartcms.smartcontent.dto;

import com.smartcms.smartcommon.model.ContentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContentStatusUpdateRequest {

    @NotNull
    private ContentStatus newStatus;
    private String note;
}
