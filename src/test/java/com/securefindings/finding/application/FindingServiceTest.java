package com.securefindings.finding.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.securefindings.finding.domain.Finding;
import com.securefindings.finding.domain.FindingSeverity;
import com.securefindings.finding.domain.FindingStatus;

class FindingServiceTest {

    private final FindingService findingService = new FindingService();

    @Test
    void deberiaCrearYListarUnHallazgo() {
        Finding finding = findingService.create(
                "SQL Injection",
                "Entrada de usuario sin validar",
                FindingSeverity.HIGH
        );

        List<Finding> findings = findingService.findAll();

        assertNotNull(finding);
        assertEquals(1, findings.size());
        assertEquals(finding, findings.get(0));
        assertEquals(FindingStatus.OPEN, finding.status());
    }
}