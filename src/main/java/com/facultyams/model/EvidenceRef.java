package com.facultyams.model;

/** Where a medical's supporting document is stored, plus the student it belongs to (for access checks). */
public record EvidenceRef(int medicalId, int studentId, String storedName, String originalName) {
}