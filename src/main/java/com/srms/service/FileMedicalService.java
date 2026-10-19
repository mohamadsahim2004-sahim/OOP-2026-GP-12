package com.srms.service;

import com.srms.model.MedicalRequest;
import com.srms.model.MedicalStatus;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * FileMedicalService persists medical certificates to disk and manages requests.
 */
public class FileMedicalService implements MedicalService {
    private final Path rootDir;
    private final AttendanceService attendanceService;
    private final Map<String, MedicalRequest> requests = new LinkedHashMap<>();
    private final List<MedicalListener> listeners = new CopyOnWriteArrayList<>();
    private int refCounter = 7;

    public FileMedicalService(AttendanceService attendanceService) {
        this(Paths.get("data", "medical"), attendanceService);
    }

    public FileMedicalService(Path rootDir, AttendanceService attendanceService) {
        this.rootDir = rootDir;
        this.attendanceService = attendanceService;
        try {
            Files.createDirectories(rootDir);
        } catch (IOException ignored) {}

        seedSampleRequest();
    }

    private void seedSampleRequest() {
        // Seed 1 past approved request and 1 pending request for realistic display
        LocalDate now = LocalDate.now();
        MedicalRequest req1 = new MedicalRequest(
                "med_1",
                "MED-2026-0005",
                "ST1001",
                now.minusDays(18),
                now.minusDays(17),
                List.of("sess_1", "sess_2"),
                "Viral Fever",
                "City General Hospital",
                "MC-99420",
                "Bed rest advised for 2 days.",
                List.of("medical_cert_scan.pdf"),
                LocalDateTime.now().minusDays(16),
                MedicalStatus.APPROVED,
                "Verified and accepted by Academic Dean."
        );
        requests.put(req1.id(), req1);
    }

    @Override
    public synchronized MedicalRequest submit(MedicalRequest draft, List<File> files) throws IOException {
        String reqId = UUID.randomUUID().toString();
        String refNo = String.format("MED-2026-%04d", ++refCounter);
        Path targetDir = rootDir.resolve(draft.studentId()).resolve(reqId);
        Files.createDirectories(targetDir);

        List<String> savedFiles = new ArrayList<>();
        if (files != null) {
            for (File f : files) {
                Path dest = targetDir.resolve(f.getName());
                Files.copy(f.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
                savedFiles.add(f.getName());
            }
        }

        MedicalRequest saved = new MedicalRequest(
                reqId,
                refNo,
                draft.studentId(),
                draft.from(),
                draft.to(),
                draft.sessionIds(),
                draft.reason(),
                draft.doctor(),
                draft.certificateNo(),
                draft.notes(),
                savedFiles,
                LocalDateTime.now(),
                MedicalStatus.PENDING,
                null
        );

        requests.put(reqId, saved);
        notifyListeners();
        return saved;
    }

    @Override
    public synchronized List<MedicalRequest> list(String studentId) {
        List<MedicalRequest> result = new ArrayList<>();
        for (MedicalRequest mr : requests.values()) {
            if (mr.studentId().equals(studentId)) {
                result.add(mr);
            }
        }
        // Most recent first
        result.sort((a, b) -> b.submittedAt().compareTo(a.submittedAt()));
        return result;
    }

    @Override
    public synchronized void withdraw(String requestId) {
        MedicalRequest mr = requests.get(requestId);
        if (mr != null && mr.status() == MedicalStatus.PENDING) {
            requests.put(requestId, new MedicalRequest(
                    mr.id(), mr.referenceNo(), mr.studentId(), mr.from(), mr.to(), mr.sessionIds(),
                    mr.reason(), mr.doctor(), mr.certificateNo(), mr.notes(), mr.files(),
                    mr.submittedAt(), MedicalStatus.WITHDRAWN, "Withdrawn by student."
            ));
            notifyListeners();
        }
    }

    @Override
    public synchronized void approve(String requestId) {
        MedicalRequest mr = requests.get(requestId);
        if (mr != null && mr.status() == MedicalStatus.PENDING) {
            requests.put(requestId, new MedicalRequest(
                    mr.id(), mr.referenceNo(), mr.studentId(), mr.from(), mr.to(), mr.sessionIds(),
                    mr.reason(), mr.doctor(), mr.certificateNo(), mr.notes(), mr.files(),
                    mr.submittedAt(), MedicalStatus.APPROVED, "Approved by Faculty Academic Board."
            ));
            if (attendanceService != null && mr.sessionIds() != null && !mr.sessionIds().isEmpty()) {
                attendanceService.markSessionsMedicalApproved(mr.sessionIds());
            }
            notifyListeners();
        }
    }

    @Override
    public void addMedicalListener(MedicalListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    private void notifyListeners() {
        for (MedicalListener l : listeners) {
            try {
                l.onMedicalRequestsChanged();
            } catch (Exception ignored) {}
        }
    }
}
