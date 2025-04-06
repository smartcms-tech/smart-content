package com.smartcms.smartcontent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class SlugValidationResponse {
    private boolean available;
    private List<String> suggestions;
}
