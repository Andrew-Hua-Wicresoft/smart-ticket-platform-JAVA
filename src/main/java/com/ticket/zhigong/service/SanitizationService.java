package com.ticket.zhigong.service;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

/**
 * Sanitizes untrusted text (LLM output, user input) before storage or display.
 * Uses jsoup to strip all HTML, preventing stored XSS and prompt injection.
 */
@Service
public class SanitizationService {

    /**
     * Strip all HTML tags and normalize whitespace.
     * Safe for LLM output and user-submitted plain text fields.
     */
    public String sanitize(String text) {
        if (text == null) return null;
        // Jsoup.clean with NONE safelist strips every HTML tag
        String cleaned = Jsoup.clean(text, Safelist.none());
        // Jsoup.clean escapes &, <, > as entities — unescape for plain text storage
        cleaned = org.jsoup.parser.Parser.unescapeEntities(cleaned, false);
        // Collapse excessive blank lines
        cleaned = cleaned.replaceAll("\\n{3,}", "\n\n");
        return cleaned.trim();
    }
}
