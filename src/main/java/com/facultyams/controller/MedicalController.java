package com.facultyams.controller;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.SwingWorker;

import com.facultyams.model.MedicalRequest;
import com.facultyams.service.StudentMedicalService;

/**
 * Controller orchestrating asynchronous medical excuse operations.
 */
public class MedicalController {
    private final StudentMedicalService medicalService;

    public MedicalController(StudentMedicalService medicalService) {
        this.medicalService = medicalService;
    }

    public void submit(MedicalRequest draft, List<File> files, Consumer<MedicalRequest> onSuccess, Consumer<Exception> onError) {
        new SwingWorker<MedicalRequest, Void>() {
            @Override
            protected MedicalRequest doInBackground() throws Exception {
                return medicalService.submit(draft, files);
            }

            @Override
            protected void done() {
                try {
                    MedicalRequest res = get();
                    if (onSuccess != null) onSuccess.accept(res);
                } catch (Exception ex) {
                    if (onError != null) onError.accept(ex);
                }
            }
        }.execute();
    }

    public void withdraw(String requestId, Runnable onSuccess, Consumer<Exception> onError) {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                medicalService.withdraw(requestId);
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
