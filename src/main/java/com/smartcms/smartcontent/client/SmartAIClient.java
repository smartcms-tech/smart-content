package com.smartcms.smartcontent.client;

import com.smartcms.smartcontent.dto.SlugRequest;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


@Slf4j
@Service
public class SmartAIClient {

    @Value("${smartai.api.url}")
    private String smartAIBaseUrl;

    private final RestTemplate restTemplate;

    public SmartAIClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String generateSlug(String input) {

        if (StringUtils.isNotEmpty(input))
            return "ai-service-is-not-available";
        String url = smartAIBaseUrl + "/api/smartai/generate-slug";

        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Create request entity
        HttpEntity<SlugRequest> requestEntity = new HttpEntity<>(new SlugRequest(input), headers);

        try {
            // Make the API call
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);

            // Check for successful response
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            } else {
                log.error("Failed to generate slug. Status: {}, Response: {}", response.getStatusCode(), response.getBody());
                throw new RuntimeException("Failed to generate slug: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error calling SmartAI API: {}", e.getMessage(), e);
            throw new RuntimeException("Error calling SmartAI API", e);
        }
    }
}
