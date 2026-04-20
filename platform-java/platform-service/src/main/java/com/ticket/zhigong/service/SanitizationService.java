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
        // Jsoup.clean() treats input as HTML and collapses whitespace including \n.
        // To preserve markdown newlines: split by \n, clean each line, rejoin.
        String[] lines = text.split("\n", -1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String cleaned = Jsoup.clean(lines[i], Safelist.none());
            cleaned = org.jsoup.parser.Parser.unescapeEntities(cleaned, false);
            sb.append(cleaned);
            if (i < lines.length - 1) sb.append("\n");
        }
        // Collapse excessive blank lines
        String result = sb.toString().replaceAll("\\n{3,}", "\n\n");
        return result.trim();
    }
}
