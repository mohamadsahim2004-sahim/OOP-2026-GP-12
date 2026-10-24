package com.facultyams.controller;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.SwingWorker;

import com.facultyams.model.Assignment;
import com.facultyams.model.Submission;
import com.facultyams.service.SubmissionService;

/**
 * Controller handling assignment validation, file submission, and deletion logic.
 */
public class SubmissionController {
    private final SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    public String validate(Assignment assignment, List<File> files) {
        if (files == null || files.isEmpty()) {
            return "Please choose or drop at least one file to submit.";
        }
        if (files.size() > assignment.maxFiles()) {
            return "Too many files selected. Maximum allowed is " + assignment.maxFiles() + ".";
        }
        long maxBytes = (long) assignment.maxFileMb() * 1024 * 1024;
        for (File f : files) {
            if (f.length() > maxBytes) {
                return "File '" + f.getName() + "' exceeds the maximum limit of " + assignment.maxFileMb() + "MB.";
            }
            String name = f.getName().toLowerCase();
            boolean valid = assignment.allowedTypes().isEmpty() ||
                            assignment.allowedTypes().stream().anyMatch(name::endsWith);
            if (!valid) {
                return "File type for '" + f.getName() + "' is not permitted.";
            }
        }
        LocalDateTime now = LocalDateTime.now();
        if (assignment.dueAt() != null && now.isAfter(assignment.dueAt()) && !assignment.allowLate()) {
            return "Deadline has passed. Late submissions are not accepted.";
        }
        return null; // Valid
    }

    public void submit(String studentId, String assignmentId, List<File> files,
                       Consumer<Submission> onSuccess, Consumer<Exception> onError) {
        new SwingWorker<Submission, Void>() {
            @Override
            protected Submission doInBackground() throws Exception {
                return submissionService.submit(studentId, assignmentId, files);
            }

            @Override
            protected void done() {
                try {
                    Submission sub = get();
                    if (onSuccess != null) onSuccess.accept(sub);
                } catch (Exception ex) {
                    if (onError != null) onError.accept(ex);
                }
            }
        }.execute();
    }

    public void remove(String studentId, String assignmentId, Runnable onSuccess, Consumer<Exception> onError) {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                submissionService.remove(studentId, assignmentId);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    if (onSuccess != null) onSuccess.run();
                } catch (Exception ex) {
                    if (onError != null) onError.accept(ex);
                }
            }
        }.execute();
    }
}
