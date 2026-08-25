package it.fantacalcio.battitore.service;

import it.fantacalcio.battitore.model.Player;
import org.apache.poi.ss.usermodel.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class FantacalcioExcelReader {

    private static final Set<String> REQUIRED_HEADERS = Set.of("Id", "R", "Nome", "Squadra");
    private static final int MAX_HEADER_SCAN_ROWS = 40;

    public List<Player> read(File file) throws IOException {
        if (file == null || !file.isFile()) {
            throw new IllegalArgumentException("File Excel non valido.");
        }

        try (FileInputStream input = new FileInputStream(file);
             Workbook workbook = WorkbookFactory.create(input)) {

            SheetLocation location = findPlayerSheet(workbook)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Non trovo le colonne Id, R, Nome e Squadra nel file selezionato."
                    ));

            return readPlayers(location.sheet(), location.headerRowIndex());
        }
    }

    private Optional<SheetLocation> findPlayerSheet(Workbook workbook) {
        for (Sheet sheet : workbook) {
            int lastRow = Math.min(sheet.getLastRowNum(), MAX_HEADER_SCAN_ROWS);
            for (int rowIndex = 0; rowIndex <= lastRow; rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }

                Map<String, Integer> columns = buildColumnMap(row);
                if (columns.keySet().containsAll(REQUIRED_HEADERS)) {
                    return Optional.of(new SheetLocation(sheet, rowIndex));
                }
            }
        }
        return Optional.empty();
    }

    private List<Player> readPlayers(Sheet sheet, int headerRowIndex) {
        Row header = sheet.getRow(headerRowIndex);
        Map<String, Integer> columns = buildColumnMap(header);
        DataFormatter formatter = new DataFormatter(Locale.ITALY);

        List<Player> players = new ArrayList<>();

        for (int rowIndex = headerRowIndex + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }

            String name = text(row, columns.get("Nome"), formatter);
            if (name.isBlank()) {
                continue;
            }

            String role = text(row, columns.get("R"), formatter).toUpperCase(Locale.ROOT);
            String team = text(row, columns.get("Squadra"), formatter);

            // Evita righe di riepilogo o testo estraneo eventualmente presenti in fondo al foglio.
            if (role.isBlank() || team.isBlank()) {
                continue;
            }

            int id = integer(row, columns.get("Id"), formatter);
            String mantraRole = text(row, columns.get("RM"), formatter);
            double currentQuotation = decimal(row, columns.get("Qt.A"), formatter);
            double initialQuotation = decimal(row, columns.get("Qt.I"), formatter);
            double fvm = decimal(row, columns.get("FVM"), formatter);

            players.add(new Player(
                    id,
                    role,
                    mantraRole,
                    name,
                    team,
                    currentQuotation,
                    initialQuotation,
                    fvm
            ));
        }

        if (players.isEmpty()) {
            throw new IllegalArgumentException("Il listone è stato riconosciuto, ma non contiene giocatori leggibili.");
        }

        return players;
    }

    private Map<String, Integer> buildColumnMap(Row row) {
        Map<String, Integer> result = new HashMap<>();
        DataFormatter formatter = new DataFormatter(Locale.ITALY);

        for (Cell cell : row) {
            String header = formatter.formatCellValue(cell).trim();
            if (!header.isBlank()) {
                result.put(header, cell.getColumnIndex());
            }
        }
        return result;
    }

    private String text(Row row, Integer column, DataFormatter formatter) {
        if (column == null) {
            return "";
        }
        Cell cell = row.getCell(column, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }

    private int integer(Row row, Integer column, DataFormatter formatter) {
        String raw = text(row, column, formatter);
        if (raw.isBlank()) {
            return 0;
        }
        try {
            return (int) Math.round(Double.parseDouble(normalizeNumber(raw)));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private double decimal(Row row, Integer column, DataFormatter formatter) {
        String raw = text(row, column, formatter);
        if (raw.isBlank()) {
            return 0;
        }
        try {
            return Double.parseDouble(normalizeNumber(raw));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private String normalizeNumber(String raw) {
        String cleaned = raw.trim().replace(" ", "");

        // Formato italiano: 1.234,5 -> 1234.5
        if (cleaned.contains(",")) {
            cleaned = cleaned.replace(".", "").replace(',', '.');
        }
        return cleaned;
    }

    private record SheetLocation(Sheet sheet, int headerRowIndex) {
    }
}
