package com.facultyams.util;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal dependency-free PDF writer: A4 landscape pages with a title, information lines and a table
 * (header repeated on every page, "Page x of y" footer). Text uses the built-in Helvetica font, so
 * characters outside Latin-1 are shown as '?'.
 */
public final class PdfWriter {

    private static final int PAGE_W = 842;
    private static final int PAGE_H = 595;
    private static final int MARGIN = 36;
    private static final int ROW_H = 13;
    private static final double FONT = 8;
    private static final double CHAR_W = FONT * 0.55; // conservative average glyph width

    private PdfWriter() {
    }

    public static byte[] toPdf(ReportTable t) {
        int cols = t.headers().size();
        int[] x = new int[cols + 1];
        int[] capacity = new int[cols];
        computeColumns(t, x, capacity);

        List<String> pages = new ArrayList<>();
        int rowIndex = 0;
        int total = t.rows().size();
        do {
            StringBuilder c = new StringBuilder();
            int y = PAGE_H - MARGIN;
            text(c, "F2", 14, MARGIN, y - 12, t.title());
            y -= 22;
            if (pages.isEmpty()) {
                for (String info : t.info()) {
                    text(c, "F1", 9, MARGIN, y - 9, info);
                    y -= 12;
                }
            }
            y -= 6;
            // header row
            c.append("0.04 0.16 0.40 rg ").append(MARGIN).append(' ').append(y - ROW_H + 3).append(' ')
                    .append(PAGE_W - 2 * MARGIN).append(' ').append(ROW_H).append(" re f 1 g\n");
            for (int i = 0; i < cols; i++) {
                text(c, "F2", FONT, x[i] + 2, y - ROW_H + 6, fit(t.headers().get(i), capacity[i]));
            }
            c.append("0 g\n");
            y -= ROW_H;
            int bottom = MARGIN + 16;
            while (rowIndex < total && y - ROW_H >= bottom) {
                List<String> row = t.rows().get(rowIndex++);
                for (int i = 0; i < cols; i++) {
                    text(c, "F1", FONT, x[i] + 2, y - ROW_H + 6, fit(i < row.size() ? row.get(i) : "", capacity[i]));
                }
                c.append("0.85 G 0.5 w ").append(MARGIN).append(' ').append(y - ROW_H + 3).append(" m ")
                        .append(PAGE_W - MARGIN).append(' ').append(y - ROW_H + 3).append(" l S 0 G\n");
                y -= ROW_H;
            }
            if (total == 0) {
                text(c, "F1", 9, MARGIN, y - 12, "No records match the selected filters.");
            }
            pages.add(c.toString());
        } while (rowIndex < total);

        String stamp = "Generated " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        for (int p = 0; p < pages.size(); p++) {
            StringBuilder c = new StringBuilder(pages.get(p));
            text(c, "F1", 8, MARGIN, MARGIN - 4, stamp);
            text(c, "F1", 8, PAGE_W - MARGIN - 60, MARGIN - 4, "Page " + (p + 1) + " of " + pages.size());
            pages.set(p, c.toString());
        }
        return assemble(pages);
    }

    private static void computeColumns(ReportTable t, int[] x, int[] capacity) {
        int cols = t.headers().size();
        double[] weight = new double[cols];
        double sum = 0;
        for (int i = 0; i < cols; i++) {
            int max = t.headers().get(i).length();
            for (List<String> row : t.rows()) {
                if (i < row.size()) {
                    max = Math.max(max, row.get(i).length());
                }
            }
            weight[i] = Math.max(4, Math.min(max, 34)) + 1;
            sum += weight[i];
        }
        int usable = PAGE_W - 2 * MARGIN;
        x[0] = MARGIN;
        for (int i = 0; i < cols; i++) {
            int w = (int) Math.round(usable * weight[i] / sum);
            x[i + 1] = i == cols - 1 ? PAGE_W - MARGIN : x[i] + w;
            capacity[i] = Math.max(2, (int) ((x[i + 1] - x[i] - 4) / CHAR_W));
        }
    }

    private static String fit(String s, int capacity) {
        String v = s == null ? "" : s.replace('\n', ' ').replace('\r', ' ');
        return v.length() <= capacity ? v : v.substring(0, Math.max(1, capacity - 2)) + "..";
    }

    private static void text(StringBuilder c, String font, double size, int x, int y, String value) {
        c.append("BT /").append(font).append(' ').append(size).append(" Tf ").append(x).append(' ').append(y)
                .append(" Td (").append(escape(value)).append(") Tj ET\n");
    }

    /** Escapes PDF string syntax and replaces characters Latin-1 cannot show. */
    static String escape(String s) {
        StringBuilder sb = new StringBuilder();
        for (char ch : s.toCharArray()) {
            if (ch == '(' || ch == ')' || ch == '\\') {
                sb.append('\\').append(ch);
            } else if (ch < 32 || ch > 255) {
                sb.append('?');
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    private static byte[] assemble(List<String> pages) {
        int n = pages.size();
        // object numbers: 1 catalog, 2 page tree, 3 Helvetica, 4 Helvetica-Bold, then (page, content) pairs
        List<byte[]> objects = new ArrayList<>();
        objects.add(bytes("<< /Type /Catalog /Pages 2 0 R >>"));
        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < n; i++) {
            kids.append(5 + 2 * i).append(" 0 R ");
        }
        objects.add(bytes("<< /Type /Pages /Kids [" + kids + "] /Count " + n + " >>"));
        objects.add(bytes("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>"));
        objects.add(bytes("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>"));
        for (int i = 0; i < n; i++) {
            objects.add(bytes("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + PAGE_W + " " + PAGE_H + "] "
                    + "/Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents " + (6 + 2 * i) + " 0 R >>"));
            byte[] stream = bytes(pages.get(i));
            objects.add(concat(bytes("<< /Length " + stream.length + " >>\nstream\n"), stream, bytes("\nendstream")));
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        write(out, "%PDF-1.4\n");
        int[] offsets = new int[objects.size()];
        for (int i = 0; i < objects.size(); i++) {
            offsets[i] = out.size();
            write(out, (i + 1) + " 0 obj\n");
            out.writeBytes(objects.get(i));
            write(out, "\nendobj\n");
        }
        int xref = out.size();
        write(out, "xref\n0 " + (objects.size() + 1) + "\n0000000000 65535 f \n");
        for (int offset : offsets) {
            write(out, String.format("%010d 00000 n \n", offset));
        }
        write(out, "trailer\n<< /Size " + (objects.size() + 1) + " /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF\n");
        return out.toByteArray();
    }

    private static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.ISO_8859_1);
    }

    private static void write(ByteArrayOutputStream out, String s) {
        out.writeBytes(bytes(s));
    }

    private static byte[] concat(byte[]... parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] p : parts) {
            out.writeBytes(p);
        }
        return out.toByteArray();
    }
}