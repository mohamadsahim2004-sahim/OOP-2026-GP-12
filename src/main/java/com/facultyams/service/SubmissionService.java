package com.facultyams.service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import com.facultyams.model.Submission;

/**
 * Service contract for querying and persisting assignment submissions.
 */
public interface SubmissionService {
    Optional<Submission> find(String studentId, String assignmentId);
    Submission submit(String studentId, String assignmentId, List<File> files) throws IOException;
    void remove(String studentId, String assignmentId) throws IOException;
    void addSubmissionListener(SubmissionListener listener);

    @FunctionalInterface
    interface SubmissionListener {
        void onSubmissionChanged(String studentId, String assignmentId);
    }
}
