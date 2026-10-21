package edu.example.library;

import java.util.Locale;

final class Identifiers {
    private Identifiers() {}

    static String normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Identifier must not be blank");
        }
        return value.strip().toUpperCase(Locale.ROOT);
    }
}
