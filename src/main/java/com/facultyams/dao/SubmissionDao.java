package com.facultyams.dao;

import java.util.Optional;

import com.facultyams.model.Submission;

/**
 * Data Access Object contract for Submission and attached files persistence.
 */
public interface SubmissionDao {
    Optional<Submission> find(String studentId, String assignmentId);
    void save(Submission submission);
    void delete(String studentId, String assignmentId);
}
