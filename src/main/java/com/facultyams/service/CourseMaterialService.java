package com.facultyams.service;

import com.facultyams.dao.CourseMaterialDAO;
import com.facultyams.exception.AuthorizationException;
import com.facultyams.exception.DatabaseException;
import com.facultyams.exception.ValidationException;
import com.facultyams.model.CourseMaterial;

import java.util.List;

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
