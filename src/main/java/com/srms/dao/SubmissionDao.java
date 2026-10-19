package com.srms.dao;

import com.srms.model.Submission;
import java.util.Optional;

/**
 * Data Access Object contract for Submission and attached files persistence.
 */
public interface SubmissionDao {
    Optional<Submission> find(String studentId, String assignmentId);
    void save(Submission submission);
    void delete(String studentId, String assignmentId);
}
