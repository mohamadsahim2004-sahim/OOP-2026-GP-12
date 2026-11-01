package com.facultyams.service.timetable;

import com.facultyams.model.timetable.ClassSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Minimal dependency-free PDF exporter for compact timetable schedules. */
public final class PdfScheduleExporter implements Exportable {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a");

    @Override public void export(List<ClassSession> sessions, Path destination) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("UNDERGRADUATE TIMETABLE");
        lines.add("Semester 5 | 2023/2024 | Department of CSE");
        lines.add("");
        String day = "";
        for (ClassSession session : sessions) {
            String currentDay = session.getDay().toString();
            if (!day.equals(currentDay)) {
                day = currentDay;
                lines.add(day);
            }
            lines.add(session.getStart().format(TIME) + " - " + session.getEnd().format(TIME) + "    "
                    + session.getCourse().getCode() + "  " + session.getDisplayName() + "  ["
                    + session.getType().getLabel() + "]");
            lines.add("       " + session.getVenue().getName() + " | " + session.getLecturer().getName());
            if (session instanceof com.facultyams.model.timetable.PersonalEvent event && !event.getNote().isBlank()) {
                lines.add("       Note: " + event.getNote());
            }
        }
        writePdf(lines, destination);
    }

    private static void writePdf(List<String> lines, Path destination) throws IOException {
        List<List<String>> pages = new ArrayList<>();
        for (int start = 0; start < lines.size(); start += 48) {
            List<String> page = new ArrayList<>();
            if (start > 0) page.add("UNDERGRADUATE TIMETABLE (continued)");
            page.addAll(lines.subList(start, Math.min(start + 48, lines.size())));
            pages.add(page);
        }
        int fontObject = 3 + pages.size() * 2;
        List<String> objects = new ArrayList<>();
        objects.add("<< /Type /Catalog /Pages 2 0 R >>");
        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < pages.size(); i++) kids.append(3 + i * 2).append(" 0 R ");
        objects.add("<< /Type /Pages /Kids [" + kids + "] /Count " + pages.size() + " >>");
        for (int i = 0; i < pages.size(); i++) {
            int pageObject = 3 + i * 2;
            int contentObject = pageObject + 1;
            StringBuilder stream = new StringBuilder("BT\n/F1 10 Tf\n50 790 Td\n14 TL\n");
            List<String> page = pages.get(i);
            for (int lineIndex = 0; lineIndex < page.size(); lineIndex++) {
                if (lineIndex == 0 && i == 0) stream.append("/F1 16 Tf\n");
                else if (lineIndex == 1 && i == 0) stream.append("/F1 10 Tf\n");
                stream.append("(").append(pdfEscape(ascii(page.get(lineIndex)))).append(") Tj\nT*\n");
            }
            stream.append("ET");
            String content = stream.toString();
            objects.add("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 842] /Resources << /Font << /F1 "
                    + fontObject + " 0 R >> >> /Contents " + contentObject + " 0 R >>");
            objects.add("<< /Length " + content.getBytes(StandardCharsets.US_ASCII).length
                    + " >>\nstream\n" + content + "\nendstream");
        }
        objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>");

        StringBuilder pdf = new StringBuilder("%PDF-1.4\n");
        List<Integer> offsets = new ArrayList<>();
        for (int i = 0; i < objects.size(); i++) {
            offsets.add(pdf.toString().getBytes(StandardCharsets.US_ASCII).length);
            pdf.append(i + 1).append(" 0 obj\n").append(objects.get(i)).append("\nendobj\n");
        }
        int xref = pdf.toString().getBytes(StandardCharsets.US_ASCII).length;
        pdf.append("xref\n0 ").append(objects.size() + 1).append("\n0000000000 65535 f \n");
        for (int offset : offsets) pdf.append(String.format("%010d 00000 n \n", offset));
        pdf.append("trailer\n<< /Size ").append(objects.size() + 1).append(" /Root 1 0 R >>\nstartxref\n")
                .append(xref).append("\n%%EOF");
        Path parent = destination.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        Files.write(destination, pdf.toString().getBytes(StandardCharsets.US_ASCII));
    }

    private static String ascii(String value) {
        return value.replace('\u2022', '|').replace('\u2013', '-').replace('\u2014', '-')
                .replaceAll("[^\\x20-\\x7E]", "?");
    }
    private static String pdfEscape(String value) {
        return value.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }
}
