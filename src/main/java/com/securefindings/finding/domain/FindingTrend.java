package com.securefindings.finding.domain;

import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record FindingTrend(
        Instant from,
        Instant to,
        FindingTrendInterval interval,
        List<Point> points) {

    public FindingTrend {
        Objects.requireNonNull(from, "from es obligatorio");
        Objects.requireNonNull(to, "to es obligatorio");
        Objects.requireNonNull(interval, "interval es obligatorio");
        points = List.copyOf(points);
    }

    public record Point(
            Instant bucketStart,
            long total,
            Map<FindingSeverity, Long> bySeverity) {

        public Point {
            Objects.requireNonNull(bucketStart, "bucketStart es obligatorio");
            Objects.requireNonNull(bySeverity, "bySeverity es obligatorio");

            if (total < 0) {
                throw new IllegalArgumentException("total no puede ser negativo");
            }

            EnumMap<FindingSeverity, Long> copy = new EnumMap<>(FindingSeverity.class);
            copy.putAll(bySeverity);
            bySeverity = Collections.unmodifiableMap(copy);
        }
    }
}