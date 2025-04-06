package com.smartcms.smartcontent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SlugRequest {

    public SlugRequest(String title) {
        this.title = title;
    }

    private String title;
    private String description;
}

