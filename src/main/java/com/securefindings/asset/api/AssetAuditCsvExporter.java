package com.securefindings.asset.api;

import java.util.List;

import org.springframework.stereotype.Component;

import com.securefindings.asset.persistence.AssetAuditEntity;

@Component
public class AssetAuditCsvExporter {

    private static final List<String> HEADERS = List.of(
            "id",
            "assetId",
            "action",
            "actor",
            "occurredAt",
            "requestId");

    public String toCsv(List<AssetAuditEntity> events) {
        StringBuilder csv = new StringBuilder();
        appendRow(csv, HEADERS);

        for (AssetAuditEntity event : events) {
            appendRow(csv, List.of(
                    event.getId().toString(),
                    event.getAssetId().toString(),
                    event.getAction().name(),
                    event.getActor(),
                    event.getOccurredAt().toString(),
                    event.getRequestId() == null ? "" : event.getRequestId()));
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
        return neutralizeSpreadsheetFormula(value).replace("\"", "\"\"");
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