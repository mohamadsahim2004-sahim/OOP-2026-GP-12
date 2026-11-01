package com.facultyams.service;

import com.facultyams.dao.CourseMaterialDAO;
import com.facultyams.exception.AuthorizationException;
import com.facultyams.exception.DatabaseException;
import com.facultyams.exception.ValidationException;
import com.facultyams.model.CourseMaterial;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Course materials of the lecturer's own courses only. */
public class CourseMaterialService {

    private final LecturerAccessService access;
    private final CourseMaterialDAO dao;

    public CourseMaterialService() {
        this(new LecturerAccessService(), new CourseMaterialDAO());
    }

    public CourseMaterialService(LecturerAccessService access, CourseMaterialDAO dao) {
        this.access = access;
        this.dao = dao;
    }

    /** Document types a student can open (same list as the undergraduate Course Materials screen). */
    public static final Set<String> FILE_TYPES = Set.of("pdf", "doc", "docx", "ppt", "pptx",
            "xls", "xlsx", "txt", "md", "csv", "zip", "png", "jpg", "jpeg", "java");
    /** Largest upload: it is stored in the database, whose packet limit must stay above this. */
    public static final long MAX_FILE_BYTES = 10L * 1024 * 1024;

    /** Uploaded file of each material (material id to name and size) for the logged-in lecturer. */
    public Map<Integer, CourseMaterialDAO.FileInfo> getMyFiles() throws DatabaseException {
        return dao.findFileInfoByLecturer(access.currentLecturerId());
    }

    /** Adds a material and stores the chosen file (PDF, Word, ...) with it, so students can download it. */
    public void addMaterial(CourseMaterial material, File file) throws DatabaseException, ValidationException {
        if (file == null) {
            addMaterial(material);
            return;
        }
        validate(material);
        access.requireOwnedCourse(material.getCourseID());
        byte[] data = readChecked(file);
        dao.saveWithFile(withReference(material, file), true, file.getName(), data);
    }

    /** Updates a material and replaces its file with the chosen one (null keeps the current file). */
    public void updateMaterial(CourseMaterial material, File file) throws DatabaseException, ValidationException {
        if (file == null) {
            updateMaterial(material);
            return;
        }
        validate(material);
        if (material.getMaterialID() <= 0) {
            throw new ValidationException("Choose a material to update.");
        }
        int currentCourse = dao.findCourseId(material.getMaterialID());
        if (currentCourse < 0) {
            throw new AuthorizationException("This material is not in one of your courses.");
        }
        try {
            access.requireOwnedCourse(currentCourse);
        } catch (AuthorizationException e) {
            throw new AuthorizationException("This material is not in one of your courses.");
        }
        access.requireOwnedCourse(material.getCourseID());
        byte[] data = readChecked(file);
        dao.saveWithFile(withReference(material, file), false, file.getName(), data);
    }

    /** The reference shown to students becomes the file name, so the student screen treats it as a file. */
    private static CourseMaterial withReference(CourseMaterial m, File file) {
        return new CourseMaterial(m.getMaterialID(), m.getCourseID(), m.getDescription(), m.getNotes(), file.getName());
    }

    private static byte[] readChecked(File file) throws ValidationException {
        if (file == null || !file.isFile()) {
            throw new ValidationException("The chosen file could not be found.");
        }
        String name = file.getName();
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toLowerCase() : "";
        if (!FILE_TYPES.contains(ext)) {
            throw new ValidationException("This file type is not allowed. Use one of: "
                    + String.join(", ", new java.util.TreeSet<>(FILE_TYPES)) + ".");
        }
        if (name.length() > 200) {
            throw new ValidationException("The file name is too long.");
        }
        if (file.length() == 0) {
            throw new ValidationException("The chosen file is empty.");
        }
        if (file.length() > MAX_FILE_BYTES) {
            throw new ValidationException("The file is larger than " + (MAX_FILE_BYTES / (1024 * 1024)) + " MB.");
        }
        try {
            return Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            throw new ValidationException("The file could not be read: " + e.getMessage());
        }
    }

    public List<CourseMaterial> getMyMaterials() throws DatabaseException {
        return dao.findByLecturer(access.currentLecturerId());
    }

    public void addMaterial(CourseMaterial material) throws DatabaseException, ValidationException {
        validate(material);
        access.requireOwnedCourse(material.getCourseID());
        dao.addMaterial(material);
    }

    public void updateMaterial(CourseMaterial material) throws DatabaseException, ValidationException {
        validate(material);
        if (material.getMaterialID() <= 0) {
            throw new ValidationException("Choose a material to update.");
        }
        int currentCourse = dao.findCourseId(material.getMaterialID());
        if (currentCourse < 0) {
            throw new AuthorizationException("This material is not in one of your courses.");
        }
        try {
            access.requireOwnedCourse(currentCourse);          // may only touch materials of own courses
        } catch (AuthorizationException e) {
            throw new AuthorizationException("This material is not in one of your courses.");
        }
        access.requireOwnedCourse(material.getCourseID());      // ...and only move it to an own course
        dao.updateMaterial(material);
    }
    public void deleteMaterial(int materialId)
            throws DatabaseException, ValidationException {

        if (materialId <= 0) {
            throw new ValidationException("Choose a material to delete.");
        }

        int currentCourse = dao.findCourseId(materialId);

        if (currentCourse < 0) {
            throw new ValidationException("Material not found.");
        }

        access.requireOwnedCourse(currentCourse);

        dao.deleteMaterial(materialId);
    }

    private static void validate(CourseMaterial m) throws ValidationException {
        if (m == null) {
            throw new ValidationException("Course material is required.");
        }
        if (m.getCourseID() <= 0) {
            throw new ValidationException("Choose a course.");
        }
        if (m.getDescription() == null || m.getDescription().trim().isEmpty()) {
            throw new ValidationException("Description is required.");
        }
        if (m.getDescription().length() > 255) {
            throw new ValidationException("Description must be at most 255 characters.");
        }
    }
}
