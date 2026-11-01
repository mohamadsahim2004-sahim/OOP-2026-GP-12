package com.facultyams.service;

import com.facultyams.dao.StudentMedicalDAO;
import com.facultyams.dao.StudentMedicalDAO.MedicalRow;
import com.facultyams.dao.StudentMedicalDAO.SessionCheck;
import com.facultyams.exception.AuthorizationException;
import com.facultyams.exception.DatabaseException;
import com.facultyams.model.MedicalPolicy;
import com.facultyams.model.MedicalRequest;
import com.facultyams.model.MedicalStatus;
import com.facultyams.security.StudentAccess;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Medical requests backed by the {@code medical} table (one row per absent session, rows of one request share
 * a {@code request_ref}). Privacy: every read or write is scoped to the logged-in student, the student id is
 * taken from the session, and certificate files live under {@code data/medical/<studentId>/<ref>/}.
 */
public class DbStudentMedicalService implements StudentMedicalService {
    public static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "jpg", "jpeg", "png");

    private final StudentMedicalDAO dao = new StudentMedicalDAO();
    private final Path rootDir;
    private final StudentAttendanceService attendanceService;
    private final List<MedicalListener> listeners = new CopyOnWriteArrayList<>();

    public DbStudentMedicalService(StudentAttendanceService attendanceService) {
        this(Paths.get("data", "medical"), attendanceService);
    }

    public DbStudentMedicalService(Path rootDir, StudentAttendanceService attendanceService) {
        this.rootDir = rootDir;
        this.attendanceService = attendanceService;
    }

    @Override
    public synchronized MedicalRequest submit(MedicalRequest draft, List<File> files) throws IOException {
        int me = StudentAccess.currentStudentId();
        if (draft == null) throw new IllegalArgumentException("The request is empty.");
        String reason = clean(draft.reason());
        if (reason.isEmpty()) throw new IllegalArgumentException("Please enter the reason for your absence.");
        if (reason.length() > 255) throw new IllegalArgumentException("The reason is too long (maximum 255 characters).");
        String doctor = clean(draft.doctor());
        String certNo = clean(draft.certificateNo());
        String notes = clean(draft.notes());
        if (doctor.length() > 100) throw new IllegalArgumentException("The doctor/hospital text is too long (maximum 100).");
        if (certNo.length() > 50) throw new IllegalArgumentException("The certificate number is too long (maximum 50).");
        if (notes.length() > 500) throw new IllegalArgumentException("The notes are too long (maximum 500 characters).");

        List<Integer> ids = parseSessionIds(draft.sessionIds());
        validateSessions(me, ids);
        List<File> toCopy = validateFiles(files);

        String ref = "MED-" + LocalDate.now().getYear() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
        Path dir = rootDir.resolve(String.valueOf(me)).resolve(ref);
        List<String> names = new ArrayList<>();
        try {
            if (!toCopy.isEmpty()) Files.createDirectories(dir);
            for (File f : toCopy) {
                String safe = f.getName().replaceAll("[^A-Za-z0-9._ -]", "_");
                Files.copy(f.toPath(), dir.resolve(safe), StandardCopyOption.REPLACE_EXISTING);
                names.add(safe);
            }
            dao.insertRequest(me, ref, ids, LocalDate.now(), reason, doctor, certNo, notes, String.join("|", names));
        } catch (DatabaseException e) {
            deleteQuietly(dir);
            throw new StudentServiceException("Your request could not be saved. Please try again.", e);
        } catch (IOException e) {
            deleteQuietly(dir);
            throw e;
        }
        notifyListeners();
        return list(String.valueOf(me)).stream().filter(r -> ref.equals(r.id())).findFirst()
                .orElseThrow(() -> new StudentServiceException("The request was saved but could not be read back."));
    }

    @Override
    public synchronized List<MedicalRequest> list(String studentId) {
        int me = StudentAccess.requireOwn(studentId);
        List<MedicalRow> rows;
        try {
            rows = dao.findAll(me);
        } catch (DatabaseException e) {
            throw new StudentServiceException("Your medical records could not be loaded. Please try again.", e);
        }
        Map<String, List<MedicalRow>> grouped = new LinkedHashMap<>();
        for (MedicalRow r : rows) {
            String key = r.ref() == null || r.ref().isBlank() ? "MED-" + r.medicalId() : r.ref();
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(r);
        }
        List<MedicalRequest> result = new ArrayList<>();
        for (Map.Entry<String, List<MedicalRow>> e : grouped.entrySet()) {
            result.add(toRequest(me, e.getKey(), e.getValue()));
        }
        return result;
    }

    @Override
    public synchronized void withdraw(String requestId) {
        int me = StudentAccess.currentStudentId();
        if (requestId == null || requestId.isBlank()) throw new IllegalArgumentException("Select a request first.");
        try {
            if (dao.deletePending(me, requestId.trim()) == 0) {
                throw new IllegalArgumentException("Only your own pending requests can be withdrawn.");
            }
        } catch (DatabaseException e) {
            throw new StudentServiceException("The request could not be withdrawn. Please try again.", e);
        }
        deleteQuietly(rootDir.resolve(String.valueOf(me)).resolve(requestId.trim()));
        notifyListeners();
    }

    @Override
    public void addMedicalListener(MedicalListener listener) {
        if (listener != null && !listeners.contains(listener)) listeners.add(listener);
    }

    private void notifyListeners() {
        for (MedicalListener l : listeners) {
            try {
                l.onMedicalRequestsChanged();
            } catch (RuntimeException ignored) {
                // keep notifying the others
            }
        }
        if (attendanceService != null) attendanceService.notifyChanged();
    }

    private void validateSessions(int me, List<Integer> ids) {
        List<SessionCheck> checks;
        try {
            checks = dao.checkSessions(me, ids);
        } catch (DatabaseException e) {
            throw new StudentServiceException("Your sessions could not be checked. Please try again.", e);
        }
        if (checks.size() != ids.size()) {
            throw new AuthorizationException("You can only submit a medical request for your own sessions.");
        }
        LocalDate today = LocalDate.now();
        LocalDate min = null;
        LocalDate max = null;
        for (SessionCheck c : checks) {
            if (!"absent".equalsIgnoreCase(c.status())) {
                throw new IllegalArgumentException("Only absent sessions can be excused (" + c.date() + " is not absent).");
            }
            if (c.existingMedical() > 0) {
                throw new IllegalArgumentException("A medical request already exists for the session on " + c.date() + ".");
            }
            if (c.date().isAfter(today) || c.date().isBefore(today.minusDays(MedicalPolicy.WINDOW_DAYS))) {
                throw new IllegalArgumentException("Medical requests are accepted only for absences in the last "
                        + MedicalPolicy.WINDOW_DAYS + " days (" + c.date() + " is outside that window).");
            }
            if (min == null || c.date().isBefore(min)) min = c.date();
            if (max == null || c.date().isAfter(max)) max = c.date();
        }
        if (min != null && java.time.temporal.ChronoUnit.DAYS.between(min, max) + 1 > MedicalPolicy.MAX_RANGE_DAYS) {
            throw new IllegalArgumentException("A request can cover at most " + MedicalPolicy.MAX_RANGE_DAYS + " consecutive days.");
        }
    }

    private static List<Integer> parseSessionIds(List<String> raw) {
        if (raw == null || raw.isEmpty()) throw new IllegalArgumentException("Select at least one absent session.");
        Set<Integer> ids = new TreeSet<>();
        for (String s : raw) {
            try {
                ids.add(Integer.parseInt(s.trim()));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid session selected.");
            }
        }
        if (ids.size() > 30) throw new IllegalArgumentException("Too many sessions selected.");
        return new ArrayList<>(ids);
    }

    private static List<File> validateFiles(List<File> files) {
        List<File> ok = new ArrayList<>();
        if (files == null) return ok;
        if (files.size() > MedicalPolicy.MAX_FILES) {
            throw new IllegalArgumentException("You can attach at most " + MedicalPolicy.MAX_FILES + " files.");
        }
        for (File f : files) {
            String name = f.getName().toLowerCase(Locale.ROOT);
            String ext = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1) : "";
            if (!ALLOWED_EXTENSIONS.contains(ext)) {
                throw new IllegalArgumentException(f.getName() + " is not allowed. Use PDF, JPG or PNG files.");
            }
            if (!f.isFile()) throw new IllegalArgumentException(f.getName() + " could not be found.");
            if (f.length() > MedicalPolicy.MAX_FILE_MB * 1024L * 1024L) {
                throw new IllegalArgumentException(f.getName() + " is larger than " + MedicalPolicy.MAX_FILE_MB + " MB.");
            }
            ok.add(f);
        }
        return ok;
    }

    private static MedicalRequest toRequest(int me, String ref, List<MedicalRow> rows) {
        MedicalRow first = rows.get(0);
        LocalDate from = rows.stream().map(MedicalRow::sessionDate).min(LocalDate::compareTo).orElse(first.sessionDate());
        LocalDate to = rows.stream().map(MedicalRow::sessionDate).max(LocalDate::compareTo).orElse(first.sessionDate());
        boolean allApproved = rows.stream().allMatch(r -> "approved".equalsIgnoreCase(r.status()));
        boolean allRejected = rows.stream().allMatch(r -> "rejected".equalsIgnoreCase(r.status()));
        MedicalStatus status = allApproved ? MedicalStatus.APPROVED : allRejected ? MedicalStatus.REJECTED : MedicalStatus.PENDING;
        List<String> files = first.files() == null || first.files().isBlank() ? List.of()
                : List.of(first.files().split("\\|"));
        String remarks = rows.stream().map(MedicalRow::remarks).filter(s -> s != null && !s.isBlank()).findFirst().orElse(null);
        return new MedicalRequest(ref, ref, String.valueOf(me), from, to,
                rows.stream().map(r -> String.valueOf(r.attendanceId())).collect(Collectors.toList()),
                first.reason(), first.doctor() == null ? "" : first.doctor(),
                first.certificateNo() == null ? "" : first.certificateNo(), first.notes() == null ? "" : first.notes(),
                files, first.submitted().atStartOfDay(), status, remarks);
    }

    private static String clean(String s) {
        return s == null ? "" : s.trim();
    }

    private static void deleteQuietly(Path dir) {
        try {
            if (Files.isDirectory(dir)) {
                try (var walk = Files.walk(dir)) {
                    walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
                }
            }
        } catch (IOException ignored) {
            // best effort
        }
    }
}
