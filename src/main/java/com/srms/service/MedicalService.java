package com.srms.service;

import com.srms.model.MedicalRequest;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Service contract for managing medical excuse requests and certificate files.
 */
public interface MedicalService {
    MedicalRequest submit(MedicalRequest draft, List<File> files) throws IOException;
    List<MedicalRequest> list(String studentId);
    void withdraw(String requestId);
    void approve(String requestId);
    void addMedicalListener(MedicalListener listener);

    @FunctionalInterface
    interface MedicalListener {
        void onMedicalRequestsChanged();
    }
}
