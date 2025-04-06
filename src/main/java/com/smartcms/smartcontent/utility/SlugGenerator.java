package com.smartcms.smartcontent.utility;

import com.smartcms.smartcommon.model.ContentStatus;
import com.smartcms.smartcontent.client.SmartAIClient;
import com.smartcms.smartcontent.repository.ContentRepository;
import io.micrometer.common.util.StringUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class SlugGenerator {

    @Autowired
    private SmartAIClient smartAIClient;

    @Autowired
    private ContentRepository contentRepository;

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");
    private static final Pattern LEADING_TRAILING_HYPHENS = Pattern.compile("^-|-$");
    private static final Pattern MULTIPLE_HYPHENS = Pattern.compile("-+");

    private static final int MAX_SLUG_WORDS = 10;

    /**
     * Generate a basic slug from a title
     * @param input The title or text to convert to a slug
     * @return A basic slug (not guaranteed to be unique)
     */
    public String generateSlug(String input) {
        if (StringUtils.isBlank(input)) {
            throw new IllegalArgumentException("Input cannot be null or empty");
        }

        // 1. Normalize and remove accents
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "");

        // 2. Convert all whitespace and underscores to single hyphens
        String slug = WHITESPACE.matcher(normalized).replaceAll("-")
                .replace("_", "-");

        // 3. Remove all remaining non-alphanumeric chars except hyphens
        slug = NONLATIN.matcher(slug).replaceAll("");

        // 4. Convert to lowercase
        slug = slug.toLowerCase(Locale.ENGLISH);

        // 5. Remove leading/trailing hyphens and collapse multiple hyphens
        slug = MULTIPLE_HYPHENS.matcher(slug).replaceAll("-");
        slug = LEADING_TRAILING_HYPHENS.matcher(slug).replaceAll("");

        String[] words = slug.split("-");
        if (words.length > MAX_SLUG_WORDS) {
            StringBuilder shortenedSlug = new StringBuilder();
            for (int i = 0; i < MAX_SLUG_WORDS; i++) {
                if (i > 0) {
                    shortenedSlug.append("-");
                }
                shortenedSlug.append(words[i]);
            }
            slug = shortenedSlug.toString();
        }

        return slug;
    }

    /**
     * Generate a slug using AI when title doesn't provide a good slug
     * @param input The description or content to use for slug generation
     * @return An AI-generated slug
     */
    public String generateSlugWithAI(String input) {
        // Call SmartAI API to generate a slug
        String aiGeneratedSlug = smartAIClient.generateSlug(input);

        // Fallback: If AI-generated slug is empty, use a default slug
        if (aiGeneratedSlug == null || aiGeneratedSlug.isEmpty()) {
            aiGeneratedSlug = "untitled-" + System.currentTimeMillis();
        }

        return aiGeneratedSlug;
    }

    /**
     * Generate a unique slug for published content
     * @param title The content title
     * @param description The content description (fallback)
     * @param orgId The organization ID
     * @return A unique, SEO-friendly slug for published content
     */
    public String generateUniqueSlug(String title, String description, String orgId) {
        String baseSlug = generateSlug(title);

        if (baseSlug.isEmpty() && description != null && !description.isEmpty()) {
            baseSlug = generateSlugWithAI(description);
        }

        // Check uniqueness only against published content
        if (!contentRepository.existsBySlugAndOrgDetails_OrgIdAndStatus(baseSlug, orgId, ContentStatus.PUBLISHED)) {
            return baseSlug;
        }

        // Try sequential numbering first (SEO-friendly)
        for (int counter = 1; counter <= 5; counter++) {
            String sequentialSlug = baseSlug + "-" + counter;
            if (!contentRepository.existsBySlugAndOrgDetails_OrgIdAndStatus(sequentialSlug, orgId, ContentStatus.PUBLISHED)) {
                return sequentialSlug;
            }
        }

        // As final fallback, use a random suffix
        return baseSlug + "-" + RandomStringUtils.randomAlphanumeric(4).toLowerCase();
    }

    /**
     * Check if a slug is available for published content
     * @param slug The slug to check
     * @param orgId The organization ID
     * @param currentContentId Optional current content ID to exclude from check
     * @return true if the slug is available, false if already in use
     */
    public boolean isSlugAvailable(String slug, String orgId, String currentContentId) {
        if (currentContentId.isBlank())
            throw new IllegalArgumentException("Current content ID cannot be null or empty");
        return !contentRepository.existsBySlugAndOrgDetails_OrgIdAndStatusAndIdNot(
                    slug, orgId, ContentStatus.PUBLISHED, currentContentId);
    }

    /**
     * Generate slug suggestions for the UI
     * @param baseSlug The base slug
     * @param orgId The organization ID
     * @return A list of available slug suggestions
     */
    public List<String> generateSlugSuggestions(String baseSlug, String orgId) {
        List<String> suggestions = new ArrayList<>();

        // Add sequential variations
        for (int i = 1; i <= 3; i++) {
            String suggestion = baseSlug + "-" + i;
            if (!contentRepository.existsBySlugAndOrgDetails_OrgIdAndStatus(suggestion, orgId, ContentStatus.PUBLISHED)) {
                suggestions.add(suggestion);
            }
        }

        // Add a timestamp-based suggestion as fallback
        if (suggestions.isEmpty()) {
            suggestions.add(baseSlug + "-" + RandomStringUtils.randomAlphanumeric(4).toLowerCase());
        }

        return suggestions;
    }

}
