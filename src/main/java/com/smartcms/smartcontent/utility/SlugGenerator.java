package com.smartcms.smartcontent.utility;

import com.smartcms.smartcontent.client.SmartAIClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class SlugGenerator {

    @Autowired
    private SmartAIClient smartAIClient;


    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final Pattern EDGES_DASHES = Pattern.compile("(^-|-$)");

    public String generateSlug(String input) {
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("Input cannot be null or empty");
        }

        // Normalize to remove accents and diacritics
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        normalized = NON_LATIN.matcher(normalized).replaceAll("");

        // Replace whitespace with hyphens
        String slug = WHITESPACE.matcher(normalized).replaceAll("-");

        // Remove leading/trailing hyphens
        slug = EDGES_DASHES.matcher(slug).replaceAll("");

        // Convert to lowercase
        return slug.toLowerCase(Locale.ENGLISH);
    }

    public String generateSlugWithAI(String input) {
        // Call SmartAI API to generate a slug
        String aiGeneratedSlug = smartAIClient.generateSlug(input);

        // Fallback: If AI-generated slug is empty, use a default slug
        if (aiGeneratedSlug == null || aiGeneratedSlug.isEmpty()) {
            aiGeneratedSlug = "untitled-" + System.currentTimeMillis();
        }

        return aiGeneratedSlug;
    }
}
