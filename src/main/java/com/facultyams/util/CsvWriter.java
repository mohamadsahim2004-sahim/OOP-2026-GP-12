package com.facultyams.util;

import java.util.List;

/** Turns a {@link ReportTable} into CSV text that opens correctly in Excel (UTF-8 with byte-order mark). */
public final class CsvWriter {

    private CsvWriter() {
    }

    public static String toCsv(ReportTable table) {
        StringBuilder sb = new StringBuilder("\uFEFF");
        line(sb, List.of(table.title()));
        for (String info : table.info()) {
            line(sb, List.of(info));
        }
        sb.append("\r\n");
        line(sb, table.headers());
        for (List<String> row : table.rows()) {
            line(sb, row);
        }
        return sb.toString();
    }

    private static void line(StringBuilder sb, List<String> cells) {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(cell(cells.get(i)));
        }
        sb.append("\r\n");
    }

    /** Quotes the cell when needed and defuses spreadsheet formulas (cells starting with = + - @). */
    static String cell(String value) {
        String v = value == null ? "" : value;
        if (!v.isEmpty() && "=+-@\t\r".indexOf(v.charAt(0)) >= 0) {
            v = "'" + v;
        }
        if (v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r")) {
            v = "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }
}