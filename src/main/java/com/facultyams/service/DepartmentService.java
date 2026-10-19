package com.facultyams.service;

import com.facultyams.dao.DepartmentDAO;
import com.facultyams.dao.DepartmentDAO.DepartmentHead;
import com.facultyams.dao.UserDAO;
import com.facultyams.exception.AuthorizationException;
import com.facultyams.exception.DatabaseException;
import com.facultyams.exception.ValidationException;
import com.facultyams.model.Department;
import com.facultyams.model.User;
import com.facultyams.security.Role;
import com.facultyams.security.Session;
import com.facultyams.util.ValidationUtil;

import java.util.List;
import java.util.Map;

/** Department management rules. Changes are Admin-only; reading is open to logged-in users. */
public class DepartmentService {

    private final DepartmentDAO departmentDAO;
    private final UserDAO userDAO;

    public DepartmentService() {
        this(new DepartmentDAO());
    }

    public DepartmentService(DepartmentDAO departmentDAO) {
        this(departmentDAO, new UserDAO());
    }

    public DepartmentService(DepartmentDAO departmentDAO, UserDAO userDAO) {
        this.departmentDAO = departmentDAO;
        this.userDAO = userDAO;
    }

    public List<Department> getAllDepartments() throws AuthorizationException, DatabaseException {
        Session.requireLogin();
        return departmentDAO.findAll();
    }

    public List<Department> searchDepartments(String keyword) throws AuthorizationException, DatabaseException {
        Session.requireLogin();
        return departmentDAO.search(keyword);
    }

    public Department getDepartmentById(int departmentId)
            throws ValidationException, AuthorizationException, DatabaseException {
        Session.requireLogin();
        ValidationUtil.requirePositiveId(departmentId, "department id");
        Department department = departmentDAO.findById(departmentId);
        if (department == null) {
            throw new ValidationException("Department with id " + departmentId + " was not found.");
        }
        return department;
    }

    /** Department id -> head lecturer (only departments that have one). */
    public Map<Integer, DepartmentHead> getDepartmentHeads() throws AuthorizationException, DatabaseException {
        Session.requireLogin();
        return departmentDAO.findHeads();
    }

    /**
     * Admin only. Sets the head of a department, or clears it when lecturerUserId is null.
     * The head must be a lecturer of that same department.
     */
    public void setDepartmentHead(int departmentId, Integer lecturerUserId)
            throws ValidationException, AuthorizationException, DatabaseException {
        Session.requireRole(Role.ADMIN);
        Department department = getDepartmentById(departmentId);
        requireValidHead(department, lecturerUserId);
        departmentDAO.setHead(departmentId, lecturerUserId);
    }

    private void requireValidHead(Department department, Integer lecturerUserId)
            throws ValidationException, DatabaseException {
        if (lecturerUserId == null) {
            return;
        }
        User lecturer = userDAO.findById(lecturerUserId);
        if (lecturer == null || lecturer.getRole() != Role.LECTURER) {
            throw new ValidationException("The head of a department must be a lecturer.");
        }
        if (lecturer.getDepartmentId() == null || lecturer.getDepartmentId() != department.getDepartmentId()) {
            throw new ValidationException(lecturer.getFullName() + " does not belong to "
                    + department.getDepartmentCode() + ". Move the lecturer to this department first.");
        }
    }

    public Department addDepartment(Department department)
            throws ValidationException, AuthorizationException, DatabaseException {
        Session.requireRole(Role.ADMIN);
        if (department == null) {
            throw new ValidationException("Department details are required.");
        }
        department.setDepartmentId(0);
        validate(department);
        departmentDAO.insert(department);
        return department;
    }

    public void updateDepartment(Department department)
            throws ValidationException, AuthorizationException, DatabaseException {
        Session.requireRole(Role.ADMIN);
        if (department == null) {
            throw new ValidationException("Department details are required.");
        }
        getDepartmentById(department.getDepartmentId());
        validate(department);
        departmentDAO.update(department);
    }

    /**
     * Admin only. Updates code, name and head of department together (one database transaction).
     * Needs migration 002; use {@link #updateDepartment(Department)} when that column is not available.
     *
     * @param headUserId the lecturer who heads the department (must belong to it), or null for none
     */
    public void updateDepartment(Department department, Integer headUserId)
            throws ValidationException, AuthorizationException, DatabaseException {
        Session.requireRole(Role.ADMIN);
        if (department == null) {
            throw new ValidationException("Department details are required.");
        }
        getDepartmentById(department.getDepartmentId());
        validate(department);
        requireValidHead(department, headUserId);
        departmentDAO.updateWithHead(department, headUserId);
    }

    /** Only allowed when no courses or users belong to the department. */
    public void deleteDepartment(int departmentId)
            throws ValidationException, AuthorizationException, DatabaseException {
        Session.requireRole(Role.ADMIN);
        Department department = getDepartmentById(departmentId);
        int courses = departmentDAO.countCourses(departmentId);
        int users = departmentDAO.countUsers(departmentId);
        if (courses > 0 || users > 0) {
            throw new ValidationException("Cannot delete " + department.getDepartmentCode() + ": it still has "
                    + courses + " course(s) and " + users + " user(s). Move or remove them first.");
        }
        departmentDAO.delete(departmentId);
    }

    private void validate(Department department) throws ValidationException, DatabaseException {
        String code = ValidationUtil.requireNotBlank(department.getDepartmentCode(), "Department code").toUpperCase();
        if (!ValidationUtil.isValidDepartmentCode(code)) {
            throw new ValidationException("Department code must be 2-10 capital letters (e.g. DICT).");
        }
        String name = ValidationUtil.requireNotBlank(department.getDepartmentName(), "Department name");
        ValidationUtil.requireMaxLength(name, 100, "Department name");

        if (departmentDAO.codeExists(code, department.getDepartmentId())) {
            throw new ValidationException("Department code '" + code + "' already exists.");
        }
        if (departmentDAO.nameExists(name, department.getDepartmentId())) {
            throw new ValidationException("Department name '" + name + "' already exists.");
        }
        department.setDepartmentCode(code);
        department.setDepartmentName(name);
    }
}