package com.agentforge.runtime;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FeishuBindCodeParser {

    private static final Pattern BIND_PATTERN = Pattern.compile(
        "(?:绑定|bind)\\s*([A-Za-z0-9]{6})",
        Pattern.CASE_INSENSITIVE
    );

    private FeishuBindCodeParser() {
    }

    public static Optional<String> parse(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        String normalized = text.trim()
            .replaceAll("@\\S+\\s*", "")
            .trim();
        Matcher matcher = BIND_PATTERN.matcher(normalized);
        if (matcher.find()) {
            return Optional.of(matcher.group(1).toUpperCase());
        }
        return Optional.empty();
    }
}
