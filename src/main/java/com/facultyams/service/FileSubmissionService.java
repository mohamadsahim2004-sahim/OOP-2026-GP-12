package com.facultyams.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import com.facultyams.model.Submission;
import com.facultyams.model.SubmissionStatus;

/**
 * File-based implementation of SubmissionService that persists files into
 * ./data/submissions/{studentId}/{assignmentId}/ and notifies listeners on change.
 */
public class FileSubmissionService implements SubmissionService {
    private final Path rootDir;
    private final Map<String, Submission> submissions = new HashMap<>();
    private final List<SubmissionListener> listeners = new CopyOnWriteArrayList<>();

    public FileSubmissionService() {
        this(Paths.get("data", "submissions"));
    }

    public FileSubmissionService(Path rootDir) {
        this.rootDir = rootDir;
        try {
            Files.createDirectories(rootDir);
        } catch (IOException ignored) {
        }
    }

    private String key(String studentId, String assignmentId) {
        return studentId + ":" + assignmentId;
    }

    @Override
    public synchronized Optional<Submission> find(String studentId, String assignmentId) {
        return Optional.ofNullable(submissions.get(key(studentId, assignmentId)));
    }

    @Override
    public synchronized Submission submit(String studentId, String assignmentId, List<File> files) throws IOException {
        Path targetDir = rootDir.resolve(studentId).resolve(assignmentId);
        Files.createDirectories(targetDir);

        List<String> savedFileNames = new ArrayList<>();
        for (File f : files) {
            Path dest = targetDir.resolve(f.getName());
            Files.copy(f.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
            savedFileNames.add(f.getName());
        }

        Submission submission = new Submission(
                UUID.randomUUID().toString(),
                assignmentId,
                studentId,
                savedFileNames,
                LocalDateTime.now(),
                SubmissionStatus.SUBMITTED,
                null,
                null
        );

        submissions.put(key(studentId, assignmentId), submission);
        notifyListeners(studentId, assignmentId);
        return submission;
    }

    @Override
    public synchronized void remove(String studentId, String assignmentId) throws IOException {
        Path targetDir = rootDir.resolve(studentId).resolve(assignmentId);
        if (Files.exists(targetDir)) {
            try (var stream = Files.walk(targetDir)) {
                stream.sorted(Comparator.reverseOrder())
                      .map(Path::toFile)
                      .forEach(File::delete);
            }
        }
        submissions.remove(key(studentId, assignmentId));
        notifyListeners(studentId, assignmentId);
    }

    @Override
    public void addSubmissionListener(SubmissionListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    private void notifyListeners(String studentId, String assignmentId) {
        for (SubmissionListener l : listeners) {
            try {
                l.onSubmissionChanged(studentId, assignmentId);
            } catch (Exception ignored) {
            }
        }
    }
}
