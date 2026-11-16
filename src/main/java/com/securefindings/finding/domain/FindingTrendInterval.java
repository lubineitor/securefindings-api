package com.securefindings.finding.domain;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;

public enum FindingTrendInterval {
    DAY("day"),
    WEEK("week"),
    MONTH("month");

    private final String sqlValue;

    FindingTrendInterval(String sqlValue) {
        this.sqlValue = sqlValue;
    }

    public String sqlValue() {
        return sqlValue;
    }

    public Instant bucketStart(Instant instant) {
        ZonedDateTime utc = instant.atZone(ZoneOffset.UTC);

        return switch (this) {
            case DAY -> utc.toLocalDate()
                    .atStartOfDay(ZoneOffset.UTC)
                    .toInstant();
            case WEEK -> utc
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .toLocalDate()
                    .atStartOfDay(ZoneOffset.UTC)
                    .toInstant();
            case MONTH -> utc
                    .withDayOfMonth(1)
                    .toLocalDate()
                    .atStartOfDay(ZoneOffset.UTC)
                    .toInstant();
        };
    }

    public Instant nextBucket(Instant bucketStart) {
        ZonedDateTime utc = bucketStart.atZone(ZoneOffset.UTC);

        return switch (this) {
            case DAY -> utc.plusDays(1).toInstant();
            case WEEK -> utc.plusWeeks(1).toInstant();
            case MONTH -> utc.plusMonths(1).toInstant();
        };
    }
}