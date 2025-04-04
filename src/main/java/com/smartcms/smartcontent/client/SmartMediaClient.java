package com.smartcms.smartcontent.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Slf4j
@Service
public class SmartMediaClient {

    @Value("${smartmedia.api.url}")
    private String smartMediaBaseUrl;

    private final RestTemplate restTemplate;

    public SmartMediaClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Deletes a media file by ID.
     */
    public void deleteMedia(String mediaId) {
        String url = UriComponentsBuilder.fromHttpUrl(smartMediaBaseUrl)
                .path("/api/media/{mediaId}")
                .buildAndExpand(mediaId)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Void> response = restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    requestEntity,
                    Void.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("Failed to delete media {}. Status: {}", mediaId, response.getStatusCode());
                throw new RuntimeException("Failed to delete media: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error calling SmartMedia API for media {}: {}", mediaId, e.getMessage(), e);
            throw new RuntimeException("Error calling SmartMedia API", e);
        }
    }

    /**
     * Bulk deletes media files by IDs.
     */
    public void bulkDeleteMedia(List<String> mediaIds) {
        String url = smartMediaBaseUrl + "/api/media/bulk-delete";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<List<String>> requestEntity = new HttpEntity<>(mediaIds, headers);

        try {
            ResponseEntity<Void> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    Void.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("Failed to bulk delete media. Status: {}", response.getStatusCode());
                throw new RuntimeException("Bulk media deletion failed: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error calling SmartMedia bulk delete API: {}", e.getMessage(), e);
            throw new RuntimeException("Error calling SmartMedia API", e);
        }
    }
}