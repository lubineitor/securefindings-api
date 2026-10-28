package com.securefindings.finding.api;

import java.util.List;

import org.springframework.stereotype.Component;

import com.securefindings.finding.domain.Finding;

@Component
public class FindingCsvExporter {

    private static final List<String> HEADERS = List.of(
            "id",
            "title",
            "description",
            "severity",
            "status",
            "createdAt",
            "updatedAt",
            "assetId");

    public String toCsv(List<Finding> findings) {
        StringBuilder csv = new StringBuilder();
        appendRow(csv, HEADERS);

        for (Finding finding : findings) {
            appendRow(csv, List.of(
                    finding.id().toString(),
                    finding.title(),
                    finding.description(),
                    finding.severity().name(),
                    finding.status().name(),
                    finding.createdAt().toString(),
                    finding.updatedAt().toString(),
                    finding.assetId() == null
                            ? ""
                            : finding.assetId().toString()));
        }

        return csv.toString();
    }

    private void appendRow(StringBuilder csv, List<String> values) {
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                csv.append(',');
            }

            csv.append('"')
                    .append(escapeCsvValue(values.get(index)))
                    .append('"');
        }

        csv.append("\r\n");
    }

    private String escapeCsvValue(String value) {
        String safeValue = neutralizeSpreadsheetFormula(value);
        return safeValue.replace("\"", "\"\"");
    }

    private String neutralizeSpreadsheetFormula(String value) {
        if (value.isEmpty()) {
            return value;
        }

        if (value.charAt(0) == '\t'
                || value.charAt(0) == '\r'
                || value.charAt(0) == '\n') {
            return "'" + value;
        }

        int firstNonWhitespace = 0;
        while (firstNonWhitespace < value.length()
                && isLeadingWhitespace(value.charAt(firstNonWhitespace))) {
            firstNonWhitespace++;
        }

        if (firstNonWhitespace < value.length()
                && "=+-@".indexOf(value.charAt(firstNonWhitespace)) >= 0) {
            return "'" + value;
        }

        return value;
    }

    private boolean isLeadingWhitespace(char character) {
        return Character.isWhitespace(character)
                || Character.isSpaceChar(character)
                || Character.isISOControl(character)
                || Character.getType(character) == Character.FORMAT
                || character == '\uFEFF';
    }
}