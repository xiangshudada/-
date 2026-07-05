package com.easylink.ruleproduct.core.operator;

import com.easylink.ruleproduct.core.model.Fact;
import com.easylink.ruleproduct.core.model.ThresholdProfile;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

final class OperatorSupport {

    private OperatorSupport() {
    }

    static Optional<BigDecimal> numberMetric(List<Fact> facts, String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        for (Fact fact : facts) {
            Optional<BigDecimal> value = asNumber(fact.metrics().get(key));
            if (value.isPresent()) {
                return value;
            }
        }
        return Optional.empty();
    }

    static Optional<BigDecimal> numberField(List<Fact> facts, String key) {
        Optional<BigDecimal> metric = numberMetric(facts, key);
        if (metric.isPresent()) {
            return metric;
        }
        for (Map<String, Object> sample : samples(facts)) {
            Optional<BigDecimal> value = asNumber(sample.get(key));
            if (value.isPresent()) {
                return value;
            }
        }
        return Optional.empty();
    }

    static Optional<BigDecimal> asNumber(Object value) {
        if (value instanceof Number number) {
            return Optional.of(new BigDecimal(number.toString()));
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Optional.of(new BigDecimal(text));
            } catch (NumberFormatException ignored) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    static List<Map<String, Object>> samples(List<Fact> facts) {
        List<Map<String, Object>> samples = new ArrayList<>();
        for (Fact fact : facts) {
            samples.addAll(fact.samples());
        }
        return samples;
    }

    static BigDecimal divide(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return numerator.divide(denominator, 8, RoundingMode.HALF_UP);
    }

    static BigDecimal thresholdMin(ThresholdProfile profile, List<Fact> facts) {
        BigDecimal configured = profile.min();
        BigDecimal byClientType = numberByClientType(profile.raw().get("minByClientType"), facts, true);
        return byClientType == null ? configured : byClientType;
    }

    static BigDecimal thresholdMax(ThresholdProfile profile, List<Fact> facts) {
        BigDecimal configured = profile.max();
        BigDecimal byClientType = numberByClientType(profile.raw().get("maxByClientType"), facts, false);
        return byClientType == null ? configured : byClientType;
    }

    static Optional<String> clientType(List<Fact> facts) {
        for (Fact fact : facts) {
            Object value = fact.metrics().get("clientType");
            if (value instanceof String text && !text.isBlank()) {
                return Optional.of(text.trim().toUpperCase(Locale.ROOT));
            }
        }
        for (Map<String, Object> sample : samples(facts)) {
            Object value = sample.get("clientType");
            if (value instanceof String text && !text.isBlank()) {
                return Optional.of(text.trim().toUpperCase(Locale.ROOT));
            }
        }
        return Optional.empty();
    }

    static boolean booleanValue(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        if (value instanceof String text) {
            String normalized = text.trim().toLowerCase(Locale.ROOT);
            return "true".equals(normalized) || "y".equals(normalized) || "yes".equals(normalized)
                    || "1".equals(normalized) || "是".equals(normalized);
        }
        return false;
    }

    static Optional<LocalDate> dateField(Map<String, Object> sample, String key) {
        Object value = sample.get(key);
        if (value instanceof LocalDate date) {
            return Optional.of(date);
        }
        if (value instanceof java.sql.Date date) {
            return Optional.of(date.toLocalDate());
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Optional.of(LocalDate.parse(text));
            } catch (Exception ignored) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    static Optional<Long> daysBetween(Map<String, Object> sample, String startField, String endField) {
        Optional<LocalDate> start = dateField(sample, startField);
        Optional<LocalDate> end = dateField(sample, endField);
        if (start.isEmpty() || end.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Math.abs(ChronoUnit.DAYS.between(start.get(), end.get())));
    }

    private static BigDecimal numberByClientType(Object thresholds, List<Fact> facts, boolean useLargestFallback) {
        if (!(thresholds instanceof Map<?, ?> map)) {
            return null;
        }
        Optional<String> clientType = clientType(facts);
        if (clientType.isPresent()) {
            Object value = map.get(clientType.get());
            return asNumber(value).orElse(null);
        }
        BigDecimal fallback = null;
        for (Object value : map.values()) {
            BigDecimal number = asNumber(value).orElse(null);
            if (number == null) {
                continue;
            }
            if (fallback == null) {
                fallback = number;
            } else if (useLargestFallback && number.compareTo(fallback) > 0) {
                fallback = number;
            } else if (!useLargestFallback && number.compareTo(fallback) < 0) {
                fallback = number;
            }
        }
        return fallback;
    }
}
