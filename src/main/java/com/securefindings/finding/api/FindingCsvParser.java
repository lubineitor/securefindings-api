package com.securefindings.finding.api;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.securefindings.finding.domain.FindingSeverity;

@Component
public class FindingCsvParser {

    public static final int MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024;
    public static final int MAX_ROWS = 1_000;

    private static final List<String> EXPECTED_HEADERS = List.of(
            "title",
            "description",
            "severity",
            "assetId",
            "dueAt");

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    public ParseResult parse(byte[] csvBytes) {
        if (csvBytes == null || csvBytes.length == 0) {
            return invalidFile(1, "file", "El fichero CSV está vacío");
        }

        if (csvBytes.length > MAX_FILE_SIZE_BYTES) {
            return invalidFile(1, "file", "El fichero CSV supera el tamaño máximo de 5 MiB");
        }

        String csv;

        try {
            CharBuffer decoded = StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(csvBytes));

            csv = decoded.toString();
        } catch (CharacterCodingException exception) {
            return invalidFile(1, "file", "El fichero debe estar codificado en UTF-8");
        }

        if (csv.startsWith("\uFEFF")) {
            csv = csv.substring(1);
        }

        List<RawRow> rawRows;

        try {
            rawRows = parseRecords(csv);
        } catch (MalformedCsvException exception) {
            return invalidFile(
                    exception.rowNumber(),
                    "file",
                    exception.getMessage());
        }

        if (rawRows.isEmpty()) {
            return invalidFile(1, "file", "El fichero CSV no contiene una cabecera");
        }

        List<String> headers = rawRows.get(0)
                .fields()
                .stream()
                .map(value -> value == null ? "" : value.trim())
                .toList();

        if (!EXPECTED_HEADERS.equals(headers)) {
            return invalidFile(
                    rawRows.get(0).rowNumber(),
                    "header",
                    "La cabecera debe ser exactamente: title,description,severity,assetId,dueAt");
        }

        List<ParsedRow> parsedRows = new ArrayList<>();
        List<FindingCsvImportResponse.RowError> errors = new ArrayList<>();
        int dataRowCount = 0;

        for (int index = 1; index < rawRows.size(); index++) {
            RawRow rawRow = rawRows.get(index);

            if (rawRow.fields().stream()
                    .allMatch(value -> value == null || value.isBlank())) {
                continue;
            }

            dataRowCount++;

            if (dataRowCount > MAX_ROWS) {
                errors.add(error(
                        rawRow.rowNumber(),
                        "file",
                        "El fichero no puede contener más de 1.000 filas de datos"));
                break;
            }

            if (rawRow.fields().size() != EXPECTED_HEADERS.size()) {
                errors.add(error(
                        rawRow.rowNumber(),
                        "columns",
                        "La fila debe contener exactamente 5 columnas"));
                continue;
            }

            int errorsBeforeRow = errors.size();
            List<String> fields = rawRow.fields();

            String title = fields.get(0).trim();
            String description = fields.get(1);
            String rawSeverity = fields.get(2).trim();
            String rawAssetId = fields.get(3).trim();
            String rawDueAt = fields.get(4).trim();

            if (title.isBlank()) {
                errors.add(error(
                        rawRow.rowNumber(),
                        "title",
                        "El título es obligatorio"));
            } else if (title.length() > 200) {
                errors.add(error(
                        rawRow.rowNumber(),
                        "title",
                        "El título no puede superar los 200 caracteres"));
            }

            if (description.isBlank()) {
                errors.add(error(
                        rawRow.rowNumber(),
                        "description",
                        "La descripción es obligatoria"));
            } else if (description.length() > 5_000) {
                errors.add(error(
                        rawRow.rowNumber(),
                        "description",
                        "La descripción no puede superar los 5.000 caracteres"));
            }

            FindingSeverity severity = null;

            try {
                severity = FindingSeverity.valueOf(
                        rawSeverity.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                String acceptedValues = Arrays.stream(FindingSeverity.values())
                        .map(value -> value == null ? "" : value.name())
                        .reduce((first, second) -> first + ", " + second)
                        .orElse("");

                errors.add(error(
                        rawRow.rowNumber(),
                        "severity",
                        "La severidad debe ser uno de: " + acceptedValues));
            }

            UUID assetId = null;

            if (!rawAssetId.isBlank()) {
                if (!UUID_PATTERN.matcher(rawAssetId).matches()) {
                    errors.add(error(
                            rawRow.rowNumber(),
                            "assetId",
                            "El identificador del activo debe ser un UUID válido"));
                } else {
                    assetId = UUID.fromString(rawAssetId);
                }
            }

            Instant dueAt = null;

            if (!rawDueAt.isBlank()) {
                try {
                    dueAt = Instant.parse(rawDueAt);
                } catch (DateTimeParseException exception) {
                    errors.add(error(
                            rawRow.rowNumber(),
                            "dueAt",
                            "La fecha límite debe tener formato ISO-8601, por ejemplo 2026-12-01T09:00:00Z"));
                }
            }

            if (errors.size() == errorsBeforeRow) {
                parsedRows.add(new ParsedRow(
                        rawRow.rowNumber(),
                        title,
                        description,
                        severity,
                        assetId,
                        dueAt));
            }
        }

        if (dataRowCount == 0 && errors.isEmpty()) {
            errors.add(error(
                    rawRows.get(0).rowNumber() + 1,
                    "file",
                    "El fichero CSV debe contener al menos una fila de datos"));
        }

        return new ParseResult(parsedRows, errors);
    }

    private List<RawRow> parseRecords(String csv)
            throws MalformedCsvException {

        List<RawRow> rows = new ArrayList<>();
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();

        boolean insideQuotes = false;
        boolean quoteClosed = false;
        int lineNumber = 1;
        int rowStartLine = 1;

        for (int index = 0; index < csv.length(); index++) {
            char character = csv.charAt(index);

            if (insideQuotes) {
                if (character == '"') {
                    if (index + 1 < csv.length()
                            && csv.charAt(index + 1) == '"') {
                        field.append('"');
                        index++;
                    } else {
                        insideQuotes = false;
                        quoteClosed = true;
                    }
                } else if (isLineBreak(character)) {
                    field.append('\n');

                    if (character == '\r'
                            && index + 1 < csv.length()
                            && csv.charAt(index + 1) == '\n') {
                        index++;
                    }

                    lineNumber++;
                } else {
                    field.append(character);
                }

                continue;
            }

            if (quoteClosed) {
                if (character == ',') {
                    fields.add(field.toString());
                    field.setLength(0);
                    quoteClosed = false;
                    continue;
                }

                if (isLineBreak(character)) {
                    fields.add(field.toString());
                    rows.add(new RawRow(rowStartLine, List.copyOf(fields)));

                    fields.clear();
                    field.setLength(0);
                    quoteClosed = false;

                    if (character == '\r'
                            && index + 1 < csv.length()
                            && csv.charAt(index + 1) == '\n') {
                        index++;
                    }

                    lineNumber++;
                    rowStartLine = lineNumber;
                    continue;
                }

                if (character == ' ' || character == '\t') {
                    continue;
                }

                throw new MalformedCsvException(
                        lineNumber,
                        "Hay contenido no válido después de un campo entre comillas");
            }

            if (character == '"') {
                if (field.length() > 0) {
                    throw new MalformedCsvException(
                            lineNumber,
                            "Las comillas dentro de un campo deben estar escapadas");
                }

                insideQuotes = true;
                continue;
            }

            if (character == ',') {
                fields.add(field.toString());
                field.setLength(0);
                continue;
            }

            if (isLineBreak(character)) {
                fields.add(field.toString());
                rows.add(new RawRow(rowStartLine, List.copyOf(fields)));

                fields.clear();
                field.setLength(0);

                if (character == '\r'
                        && index + 1 < csv.length()
                        && csv.charAt(index + 1) == '\n') {
                    index++;
                }

                lineNumber++;
                rowStartLine = lineNumber;
                continue;
            }

            field.append(character);
        }

        if (insideQuotes) {
            throw new MalformedCsvException(
                    rowStartLine,
                    "Hay un campo entre comillas que no se ha cerrado");
        }

        if (quoteClosed || field.length() > 0 || !fields.isEmpty()) {
            fields.add(field.toString());
            rows.add(new RawRow(rowStartLine, List.copyOf(fields)));
        }

        return rows;
    }

    private boolean isLineBreak(char character) {
        return character == '\r' || character == '\n';
    }

    private ParseResult invalidFile(
            int rowNumber,
            String field,
            String message) {

        return new ParseResult(
                List.of(),
                List.of(error(rowNumber, field, message)));
    }

    private FindingCsvImportResponse.RowError error(
            int rowNumber,
            String field,
            String message) {

        return new FindingCsvImportResponse.RowError(
                rowNumber,
                field,
                message);
    }

    public record ParsedRow(
            int rowNumber,
            String title,
            String description,
            FindingSeverity severity,
            UUID assetId,
            Instant dueAt) {
    }

    public record ParseResult(
            List<ParsedRow> rows,
            List<FindingCsvImportResponse.RowError> errors) {

        public ParseResult {
            rows = List.copyOf(rows);
            errors = List.copyOf(errors);
        }
    }

    private record RawRow(
            int rowNumber,
            List<String> fields) {
    }

    private static final class MalformedCsvException extends Exception {

        private final int rowNumber;

        private MalformedCsvException(
                int rowNumber,
                String message) {
            super(message);
            this.rowNumber = rowNumber;
        }

        private int rowNumber() {
            return rowNumber;
        }
    }
}