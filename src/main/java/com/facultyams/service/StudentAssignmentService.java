package com.facultyams.service;

import com.facultyams.dao.StudentAssignmentDAO;
import com.facultyams.exception.DatabaseException;
import com.facultyams.model.student.StudentAssignment;
import com.facultyams.model.student.StudentSubmissionDetails;
import com.facultyams.security.StudentAccess;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Student-visible assignments and uploads for the current student's enrolled courses. */
public class StudentAssignmentService {
    private static final long DEFAULT_FILE_LIMIT = 20L * 1024 * 1024;
    private final int studentId;
    private final StudentAssignmentDAO dao;
    private final Path uploadRoot;

    public StudentAssignmentService() {
        this(StudentAccess.currentStudentId(), new StudentAssignmentDAO(), uploadRoot());
    }

    public StudentAssignmentService(int studentId, StudentAssignmentDAO dao, Path uploadRoot) {
        this.studentId = StudentAccess.requireOwn(String.valueOf(studentId));
        this.dao = dao;
        this.uploadRoot = uploadRoot.toAbsolutePath().normalize();
    }

    public List<StudentAssignment> getAssignmentsForStudent(String courseCode) {
        StudentAccess.requireOwn(String.valueOf(studentId));
        try {
            return dao.findForStudent(studentId, courseCode);
        } catch (DatabaseException ex) {
            throw new StudentServiceException("Assignments could not be loaded. Please try again.", ex);
        }
    }

    public StudentAssignment getAssignment(long assignmentId) {
        StudentAccess.requireOwn(String.valueOf(studentId));
        try {
            return dao.findForStudent(studentId, assignmentId);
        } catch (DatabaseException ex) {
            throw new StudentServiceException("The assignment could not be loaded.", ex);
        }
    }

    public StudentSubmissionDetails getSubmission(long assignmentId) {
        StudentAccess.requireOwn(String.valueOf(studentId));
        try {
            return dao.findSubmission(studentId, assignmentId);
        } catch (DatabaseException ex) {
            throw new StudentServiceException("Submission details could not be loaded.", ex);
        }
    }

    public void submit(long assignmentId, List<Path> files, String comment) throws IOException {
        StudentAccess.requireOwn(String.valueOf(studentId));
        StudentAssignment assignment = getAssignment(assignmentId);
        if (assignment == null) throw new IOException("This assignment is not available for your courses.");
        if (files == null || files.isEmpty()) throw new IOException("Choose at least one file.");
        if (files.size() > assignment.maxFiles()) {
            throw new IOException("This assignment accepts at most " + assignment.maxFiles() + " files.");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = assignment.cutoffAt() == null ? assignment.dueAt() : assignment.cutoffAt();
        boolean late = now.isAfter(assignment.dueAt());
        if (late && (!assignment.allowLate() || now.isAfter(cutoff))) {
            throw new IOException(assignment.allowLate()
                    ? "The submission cutoff has passed." : "Late submissions are not allowed.");
        }

        Path targetDir = uploadRoot.resolve(safeSegment(assignment.courseCode()))
                .resolve(Long.toString(assignment.id())).resolve(Integer.toString(studentId)).normalize();
        if (!targetDir.startsWith(uploadRoot)) throw new IOException("Invalid upload destination.");
        Files.createDirectories(targetDir);

        List<StudentSubmissionDetails.SubmittedFile> saved = new ArrayList<>();
        List<Path> createdFiles = new ArrayList<>();
        try {
            for (Path source : files) {
                if (source == null || !Files.isRegularFile(source)) {
                    throw new IOException("A selected file is no longer available.");
                }
                long size = Files.size(source);
                long limit = (long) Math.max(1, assignment.maxFileMb()) * 1024 * 1024;
                if (size > limit || size > DEFAULT_FILE_LIMIT) {
                    throw new IOException("Each file must be no larger than "
                            + Math.min(20, assignment.maxFileMb()) + " MB.");
                }
                String originalName = source.getFileName().toString();
                String extension = extensionOf(originalName);
                if (!assignment.allowedTypes().contains(extension)) {
                    throw new IOException("File type ." + extension + " is not allowed.");
                }
                Path stored = targetDir.resolve(UUID.randomUUID() + "__" + safeFileName(originalName)).normalize();
                if (!stored.startsWith(targetDir)) throw new IOException("Invalid file name.");
                Files.copy(source, stored);
                createdFiles.add(stored);
                saved.add(new StudentSubmissionDetails.SubmittedFile(originalName,
                        uploadRoot.relativize(stored).toString().replace('\\', '/'), size));
            }
            dao.saveSubmission(studentId, assignmentId, comment == null ? "" : comment.trim(),
                    late ? "LATE" : "SUBMITTED", saved);
        } catch (DatabaseException ex) {
            cleanupCreatedFiles(createdFiles, ex);
            throw new IOException("The submission could not be saved to the database.", ex);
        } catch (IOException | RuntimeException ex) {
            cleanupCreatedFiles(createdFiles, ex);
            throw ex;
        }
    }

    public Path resolveStoredFile(String relativePath) throws IOException {
        StudentAccess.requireOwn(String.valueOf(studentId));
        Path path = uploadRoot.resolve(relativePath).normalize();
        if (!path.startsWith(uploadRoot) || !Files.isRegularFile(path)) {
            throw new IOException("The submitted file is no longer available.");
        }
        return path;
    }

    private static Path uploadRoot() {
        String configured = System.getProperty("facultyams.uploads.dir");
        if (configured == null || configured.isBlank()) configured = System.getenv("FACULTYAMS_UPLOADS_DIR");
        return configured == null || configured.isBlank()
                ? Path.of("uploads") : Path.of(configured);
    }

    private static String safeSegment(String value) {
        return value == null ? "course" : value.replaceAll("[^A-Za-z0-9_-]", "_");
    }

    private static String safeFileName(String value) {
        String name = Path.of(value).getFileName().toString().replaceAll("[^A-Za-z0-9._-]", "_");
        return name.isBlank() ? "upload" : name;
    }

    private static String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static void cleanupCreatedFiles(List<Path> paths, Exception original) {
        for (Path path : paths) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException cleanupFailure) {
                original.addSuppressed(cleanupFailure);
            }
        }
    }
}
