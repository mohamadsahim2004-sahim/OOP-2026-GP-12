package com.facultyams.service;

import com.facultyams.model.MedicalRequest;

import java.io.File;
import java.io.IOException;
import java.util.List;

/** Medical excuse requests of the logged-in student. */
public interface StudentMedicalService {
    /** Validates and saves a new PENDING request for the student's own absent sessions. */
    MedicalRequest submit(MedicalRequest draft, List<File> files) throws IOException;

    List<MedicalRequest> list(String studentId);

    /** Removes one of the student's own PENDING requests. */
    void withdraw(String requestId);

    void addMedicalListener(MedicalListener listener);

    @FunctionalInterface
    interface MedicalListener {
        void onMedicalRequestsChanged();
    }
}
