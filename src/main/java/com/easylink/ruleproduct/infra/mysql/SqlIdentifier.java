package com.easylink.ruleproduct.infra.mysql;

import java.util.Arrays;
import java.util.stream.Collectors;

final class SqlIdentifier {

    private SqlIdentifier() {
    }

    static String requireSimple(String value) {
        if (value == null || !value.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("Unsafe SQL identifier: " + value);
        }
        return value;
    }

    static String requireQualified(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("SQL identifier is blank");
        }
        return Arrays.stream(value.split("\\."))
                .map(SqlIdentifier::requireSimple)
                .collect(Collectors.joining("."));
    }
}
