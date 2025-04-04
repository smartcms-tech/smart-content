package com.smartcms.smartcontent.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ContentVersionDto {
    private int version;
    private String updatedBy;
    private Instant updatedAt;
    private String changeSummary;
}
