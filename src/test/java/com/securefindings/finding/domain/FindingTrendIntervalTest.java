package com.securefindings.finding.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class FindingTrendIntervalTest {

    @Test
    void deberiaCalcularInicioYPeriodoSiguienteEnUtc() {
        Instant wednesday = Instant.parse("2026-10-21T14:30:00Z");

        assertEquals(
                Instant.parse("2026-10-21T00:00:00Z"),
                FindingTrendInterval.DAY.bucketStart(wednesday));
        assertEquals(
                Instant.parse("2026-10-22T00:00:00Z"),
                FindingTrendInterval.DAY.nextBucket(
                        FindingTrendInterval.DAY.bucketStart(wednesday)));

        assertEquals(
                Instant.parse("2026-10-19T00:00:00Z"),
                FindingTrendInterval.WEEK.bucketStart(wednesday));
        assertEquals(
                Instant.parse("2026-10-26T00:00:00Z"),
                FindingTrendInterval.WEEK.nextBucket(
                        FindingTrendInterval.WEEK.bucketStart(wednesday)));

        assertEquals(
                Instant.parse("2026-10-01T00:00:00Z"),
                FindingTrendInterval.MONTH.bucketStart(wednesday));
        assertEquals(
                Instant.parse("2026-11-01T00:00:00Z"),
                FindingTrendInterval.MONTH.nextBucket(
                        FindingTrendInterval.MONTH.bucketStart(wednesday)));
    }
}