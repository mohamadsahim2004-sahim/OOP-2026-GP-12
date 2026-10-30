package com.facultyams.service;

import com.facultyams.exception.AuthorizationException;
import com.facultyams.exception.ValidationException;
import com.facultyams.model.Admin;
import com.facultyams.model.AttendanceFilter;
import com.facultyams.model.TechnicalOfficer;
import com.facultyams.security.EvidenceStorage;
import com.facultyams.security.Session;
import com.facultyams.service.AttendanceService;
import com.facultyams.service.TechnicalMedicalService;
import com.facultyams.util.CsvWriter;
import com.facultyams.util.PdfWriter;
import com.facultyams.util.ReportTable;
import com.facultyams.util.SqlFilter;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Automated checks for the Technical Officer module that need NO database: attendance rules, role permissions,
 * input validation, document validation and storage, CSV / PDF export and the SQL filter builder.
 * Run:  java -cp target/classes com.facultyams.TechnicalOfficerChecks [pdf-output-file]
 * Anything that reads or writes MySQL must be checked manually (see README).
 */
public class TechnicalOfficerChecks {

    private static int passed;
    private static int failed;

    public static void main(String[] args) throws Exception {
        rules();
        permissions();
        validation();
        evidence();
        exports(args.length > 0 ? Path.of(args[0]) : Files.createTempFile("report", ".pdf"));
        sqlFilter();
        System.out.println("\nTechnical Officer checks: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void rules() {
        AttendanceService s = new AttendanceService();
        check("percentage = (attended + approved medical) / 15", Math.abs(s.calculatePercentage(10, 2, 15) - 80.0) < 1e-9);
        check("80% is eligible, 79.9% is not", s.isEligible(80.0) && !s.isEligible(79.9));
        check("all 15 attended = 100%", Math.abs(s.calculatePercentage(15, 0, 15) - 100.0) < 1e-9);
        check("zero sessions is rejected", throwsIllegal(() -> s.calculatePercentage(1, 0, 0)));
        check("statuses are present, absent, late", AttendanceService.STATUSES.equals(List.of("present", "absent", "late")));
    }

    private static void permissions() {
        Session.logout();
        AttendanceService a = new AttendanceService();
        TechnicalMedicalService m = new TechnicalMedicalService();
        check("logged-out user cannot load courses", denied(a::getCourses));
        check("logged-out user cannot approve", denied(() -> { m.approve(1); return null; }));
        Session.login(new Admin(1, "admin", "Admin", "a@x.lk"));
        check("administrator cannot mark attendance", denied(() -> a.saveSession(1, 1, "B1", java.time.LocalDate.now(), java.util.Map.of(1, "present"))));
        check("administrator cannot read medical records", denied(() -> m.listPending()));
        check("administrator cannot open documents", denied(() -> m.getEvidencePath(1)));
        check("administrator cannot correct attendance", denied(() -> { a.correctStatus(1, "late"); return null; }));
        Session.logout();
    }

    private static void validation() {
        Session.login(new TechnicalOfficer(7, "to", "Tech Officer", "to@x.lk"));
        AttendanceService a = new AttendanceService();
        TechnicalMedicalService m = new TechnicalMedicalService();
        check("session 0 is rejected", invalid(() -> a.loadSheet(1, 0, "B1")));
        check("session 16 is rejected", invalid(() -> a.loadSheet(1, 16, "B1")));
        check("blank batch is rejected", invalid(() -> a.loadSheet(1, 1, " ")));
        check("future date is rejected", invalid(() -> a.saveSession(1, 1, "B1", java.time.LocalDate.now().plusDays(1), java.util.Map.of(1, "present"))));
        check("no students is rejected", invalid(() -> a.saveSession(1, 1, "B1", java.time.LocalDate.now(), java.util.Map.of())));
        check("unknown status is rejected", invalid(() -> { a.correctStatus(1, "holiday"); return null; }));
        check("from after to is rejected", invalid(() -> a.getHistory(new AttendanceFilter(null, null, null, null, null,
                java.time.LocalDate.of(2026, 2, 1), java.time.LocalDate.of(2026, 1, 1), null))));
        check("short medical reason is rejected", invalid(() -> m.submit(List.of(1), "abc", null)));
        check("medical without a selected session is rejected", invalid(() -> m.submit(List.<Integer>of(), "Fever and rest advised", null)));
        check("reject needs a reason", invalid(() -> { m.reject(1, "  "); return null; }));
        check("over-long reason is rejected", invalid(() -> { m.reject(1, "x".repeat(300)); return null; }));
        Session.logout();
    }

    private static void evidence() throws Exception {
        Path root = Files.createTempDirectory("evidence-test");
        EvidenceStorage st = new EvidenceStorage(root);
        Path pdf = write(root, "cert.pdf", "%PDF-1.4 test".getBytes(StandardCharsets.ISO_8859_1));
        Path png = write(root, "scan.png", new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1});
        Path jpg = write(root, "photo.JPG", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0});
        check("PDF accepted", noThrow(() -> st.validate(pdf)));
        check("PNG accepted", noThrow(() -> st.validate(png)));
        check("JPG (upper-case extension) accepted", noThrow(() -> st.validate(jpg)));
        check("exe rejected", invalid(() -> st.validate(write(root, "run.exe", new byte[] {1, 2, 3}))));
        check("text renamed to .pdf rejected", invalid(() -> st.validate(write(root, "fake.pdf", "hello".getBytes()))));
        check("empty file rejected", invalid(() -> st.validate(write(root, "empty.pdf", new byte[0]))));
        byte[] big = new byte[(int) EvidenceStorage.MAX_BYTES + 1];
        System.arraycopy("%PDF".getBytes(), 0, big, 0, 4);
        check("file over 5 MB rejected", invalid(() -> st.validate(write(root, "big.pdf", big))));
        check("missing file rejected", invalid(() -> st.validate(root.resolve("nope.pdf"))));
        EvidenceStorage.Stored stored = st.store(pdf);
        check("stored under a generated name", stored.storedName().matches("[0-9a-f]{32}\\.pdf") && stored.originalName().equals("cert.pdf"));
        check("stored file can be resolved", st.resolve(stored.storedName()) != null);
        check("path traversal cannot be resolved", st.resolve("../cert.pdf") == null && st.resolve("cert.pdf") == null);
        st.delete(stored.storedName());
        check("deleted file no longer resolves", st.resolve(stored.storedName()) == null);
    }

    private static void exports(Path pdfOut) throws Exception {
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("TG/1", "Amal, \"A\" Perera", "=SUM(A1)", "85.0%"));
        for (int i = 0; i < 90; i++) {
            rows.add(List.of("TG/" + (i + 2), "Student (" + i + ") \\ test \u0DC3", "x", "70.0%"));
        }
        ReportTable t = new ReportTable("Attendance Report", List.of("Filters: none"), List.of("Reg", "Name", "Note", "Attendance %"), rows);
        String csv = CsvWriter.toCsv(t);
        check("CSV starts with BOM and title", csv.startsWith("\uFEFFAttendance Report"));
        check("CSV quotes commas and quotes", csv.contains("\"Amal, \"\"A\"\" Perera\""));
        check("CSV defuses formulas", csv.contains(",'=SUM(A1),"));
        check("CSV has header + data rows", csv.split("\r\n").length == 1 + 1 + 1 + 1 + rows.size());
        byte[] pdf = PdfWriter.toPdf(t);
        Files.write(pdfOut, pdf);
        String head = new String(pdf, 0, 8, StandardCharsets.ISO_8859_1);
        check("PDF has header and EOF marker", head.startsWith("%PDF-1.4") && new String(pdf, StandardCharsets.ISO_8859_1).trim().endsWith("%%EOF"));
        check("PDF spans several pages", new String(pdf, StandardCharsets.ISO_8859_1).contains("/Count 3") || new String(pdf, StandardCharsets.ISO_8859_1).contains("/Count 2"));
        System.out.println("        (PDF written to " + pdfOut + ")");
    }

    private static void sqlFilter() {
        SqlFilter f = new SqlFilter().and("a = 1").andIf(null, "b = ?").andIf("  ", "c = ?").andIf("x", "d = ?")
                .containsAny("50%_", "u.name", "s.reg");
        check("filter skips empty criteria", f.where().equals(" WHERE a = 1 AND d = ? AND (u.name LIKE ? OR s.reg LIKE ?)"));
        check("LIKE wildcards are escaped", SqlFilter.escapeLike("50%_\\").equals("50\\%\\_\\\\"));
    }

    // ------------------------------------------------------------------ helpers

    private static Path write(Path dir, String name, byte[] content) throws Exception {
        Path p = dir.resolve(name);
        Files.write(p, content);
        return p;
    }

    private static boolean denied(Callable<?> c) {
        try {
            c.call();
            return false;
        } catch (AuthorizationException e) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean invalid(Callable<?> c) {
        try {
            c.call();
            return false;
        } catch (ValidationException e) {
            return true;
        } catch (Exception e) {
            System.out.println("        unexpected: " + e);
            return false;
        }
    }

    private static boolean throwsIllegal(Runnable r) {
        try {
            r.run();
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private static boolean noThrow(Callable<?> c) {
        try {
            c.call();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS  " : "FAIL  ") + name);
        if (ok) {
            passed++;
        } else {
            failed++;
        }
    }
}