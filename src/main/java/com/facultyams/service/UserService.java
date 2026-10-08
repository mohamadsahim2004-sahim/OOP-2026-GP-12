package com.facultyams.service;

import com.facultyams.dao.DepartmentDAO;
import com.facultyams.dao.UserDAO;
import com.facultyams.exception.AuthorizationException;
import com.facultyams.exception.DatabaseException;
import com.facultyams.exception.ValidationException;
import com.facultyams.model.Lecturer;
import com.facultyams.model.Student;
import com.facultyams.model.User;
import com.facultyams.model.User.ProfileField;
import com.facultyams.security.PasswordUtil;
import com.facultyams.security.Role;
import com.facultyams.security.Session;
import com.facultyams.util.ValidationUtil;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * User management rules. Admin-only operations check the session role.
 * Other modules may use the read methods and updateOwnProfile().
 */
public class UserService {

    /** e.g. TG/2023/1234 */
    private static final Pattern REGISTRATION_NO = Pattern.compile("^[A-Z]{2,4}/\\d{4}/\\d{3,5}$");

    private final UserDAO userDAO;
    private final DepartmentDAO departmentDAO;

    public UserService() {
        this(new UserDAO(), new DepartmentDAO());
    }

    public UserService(UserDAO userDAO, DepartmentDAO departmentDAO) {
        this.userDAO = userDAO;
        this.departmentDAO = departmentDAO;
    }

    // ------------------------------------------------------------------ reading (any logged-in user)

    public List<User> getAllUsers() throws AuthorizationException, DatabaseException {
        Session.requireRole(Role.ADMIN);
        return userDAO.findAll();
    }

    public List<User> searchUsers(String keyword, Role roleFilter) throws AuthorizationException, DatabaseException {
        Session.requireRole(Role.ADMIN);
        return userDAO.search(keyword, roleFilter);
    }

    /** e.g. getUsersByRole(Role.STUDENT) - available to every logged-in user. */
    public List<User> getUsersByRole(Role role) throws AuthorizationException, DatabaseException {
        Session.requireLogin();
        return userDAO.findByRole(role);
    }

    public User getUserById(int userId) throws ValidationException, AuthorizationException, DatabaseException {
        Session.requireLogin();
        ValidationUtil.requirePositiveId(userId, "user id");
        User user = userDAO.findById(userId);
        if (user == null) {
            throw new ValidationException("User with id " + userId + " was not found.");
        }
        return user;
    }

    // ------------------------------------------------------------------ admin operations

    public User createUser(User user, String plainPassword)
            throws ValidationException, AuthorizationException, DatabaseException {
        Session.requireRole(Role.ADMIN);
        if (user == null) {
            throw new ValidationException("User details are required.");
        }
        user.setUserId(0);
        validateUser(user);
        ValidationUtil.requireValidPassword(plainPassword);
        user.setPasswordHash(PasswordUtil.hashPassword(plainPassword));
        userDAO.insert(user);
        return user;
    }

    /** Updates all details except the password. The role may be changed. */
    public void updateUser(User user) throws ValidationException, AuthorizationException, DatabaseException {
        Session.requireRole(Role.ADMIN);
        if (user == null) {
            throw new ValidationException("User details are required.");
        }
        User existing = getUserById(user.getUserId());
        User me = Session.getCurrentUser();
        if (me.getUserId() == existing.getUserId()) {
            if (user.getRole() != Role.ADMIN) {
                throw new ValidationException("You cannot remove your own Admin role.");
            }
            if (!user.isActive()) {
                throw new ValidationException("You cannot deactivate your own account.");
            }
        }
        validateUser(user);
        userDAO.update(user);
    }

    public void resetPassword(int userId, String newPassword)
            throws ValidationException, AuthorizationException, DatabaseException {
        Session.requireRole(Role.ADMIN);
        getUserById(userId);
        ValidationUtil.requireValidPassword(newPassword);
        userDAO.updatePassword(userId, PasswordUtil.hashPassword(newPassword));
    }

    /** Deactivated users cannot log in, but their records are kept. */
    public void setUserActive(int userId, boolean active)
            throws ValidationException, AuthorizationException, DatabaseException {
        Session.requireRole(Role.ADMIN);
        getUserById(userId);
        if (!active && Session.getCurrentUser().getUserId() == userId) {
            throw new ValidationException("You cannot deactivate your own account.");
        }
        userDAO.setActive(userId, active);
    }

    /** Permanently deletes a user. Prefer setUserActive(id, false) when the user has records. */
    public void deleteUser(int userId) throws ValidationException, AuthorizationException, DatabaseException {
        Session.requireRole(Role.ADMIN);
        getUserById(userId);
        if (Session.getCurrentUser().getUserId() == userId) {
            throw new ValidationException("You cannot delete your own account.");
        }
        userDAO.delete(userId);
    }

    // ------------------------------------------------------------------ own profile (all roles)

    /**
     * Lets the logged-in user update their own profile. Which fields may change
     * depends on the user type (polymorphism: User.getEditableProfileFields()).
     * Pass the new values; unchanged values are simply ignored.
     */
    public void updateOwnProfile(String fullName, String email, String contactNumber, String profilePicture)
            throws ValidationException, AuthorizationException, DatabaseException {
        Session.requireLogin();
        User me = getUserById(Session.getCurrentUser().getUserId());

        String newFullName = ValidationUtil.emptyToNull(fullName);
        String newEmail = ValidationUtil.emptyToNull(email);
        String newContact = ValidationUtil.emptyToNull(contactNumber);
        String newPicture = ValidationUtil.emptyToNull(profilePicture);

        checkEditable(me, ProfileField.FULL_NAME, me.getFullName(), newFullName);
        checkEditable(me, ProfileField.EMAIL, me.getEmail(), newEmail);
        checkEditable(me, ProfileField.CONTACT_NUMBER, me.getContactNumber(), newContact);
        checkEditable(me, ProfileField.PROFILE_PICTURE, me.getProfilePicture(), newPicture);

        me.setFullName(newFullName);
        me.setEmail(newEmail);
        me.setContactNumber(newContact);
        me.setProfilePicture(newPicture);
        validateCommonFields(me);

        userDAO.updateProfile(me);
        Session.start(me); // refresh the session copy
    }

    private void checkEditable(User user, ProfileField field, String oldValue, String newValue)
            throws AuthorizationException {
        if (!Objects.equals(oldValue, newValue) && !user.canEditOwn(field)) {
            throw new AuthorizationException(user.getRole().getDisplayName()
                    + " users are not allowed to change " + field.name().toLowerCase().replace('_', ' ') + ".");
        }
    }

    // ------------------------------------------------------------------ validation

    private void validateUser(User user) throws ValidationException, DatabaseException {
        String username = ValidationUtil.requireNotBlank(user.getUsername(), "Username");
        if (!ValidationUtil.isValidUsername(username)) {
            throw new ValidationException("Username must be 3-30 characters: letters, digits, '.' or '_' only.");
        }
        user.setUsername(username);
        if (userDAO.usernameExists(username, user.getUserId())) {
            throw new ValidationException("Username '" + username + "' is already taken.");
        }

        validateCommonFields(user);

        if (user.getRole() != Role.ADMIN && user.getDepartmentId() == null) {
            throw new ValidationException("Please select a department for this " + user.getRole().getDisplayName() + ".");
        }

        if (user instanceof Lecturer) {
            Lecturer lecturer = (Lecturer) user;
            lecturer.setDesignation(ValidationUtil.requireNotBlank(lecturer.getDesignation(), "Designation"));
            ValidationUtil.requireMaxLength(lecturer.getDesignation(), 50, "Designation");
        }

        if (user instanceof Student) {
            Student student = (Student) user;
            String regNo = ValidationUtil.requireNotBlank(student.getRegistrationNumber(), "Registration number")
                    .toUpperCase();
            if (!REGISTRATION_NO.matcher(regNo).matches()) {
                throw new ValidationException("Registration number must look like TG/2023/1234.");
            }
            if (userDAO.registrationNumberExists(regNo, student.getUserId())) {
                throw new ValidationException("Registration number " + regNo + " is already used.");
            }
            student.setRegistrationNumber(regNo);
            student.setBatch(ValidationUtil.requireNotBlank(student.getBatch(), "Batch"));
            ValidationUtil.requireMaxLength(student.getBatch(), 10, "Batch");
            if (student.getStudentStatus() == null) {
                throw new ValidationException("Student status is required.");
            }
        }
    }

    /** Fields every user has (used for both admin edits and own-profile edits). */
    private void validateCommonFields(User user) throws ValidationException, DatabaseException {
        user.setFullName(ValidationUtil.requireNotBlank(user.getFullName(), "Full name"));
        ValidationUtil.requireMaxLength(user.getFullName(), 100, "Full name");

        String email = ValidationUtil.requireNotBlank(user.getEmail(), "Email").toLowerCase();
        if (!ValidationUtil.isValidEmail(email)) {
            throw new ValidationException("Please enter a valid email address.");
        }
        ValidationUtil.requireMaxLength(email, 100, "Email");
        if (userDAO.emailExists(email, user.getUserId())) {
            throw new ValidationException("Email '" + email + "' is already used by another user.");
        }
        user.setEmail(email);

        String contact = ValidationUtil.emptyToNull(user.getContactNumber());
        if (contact != null && !ValidationUtil.isValidPhone(contact)) {
            throw new ValidationException("Contact number must be 10 digits starting with 0 (e.g. 0771234567) or +94XXXXXXXXX.");
        }
        user.setContactNumber(contact);

        String picture = ValidationUtil.emptyToNull(user.getProfilePicture());
        ValidationUtil.requireMaxLength(picture, 255, "Profile picture path");
        user.setProfilePicture(picture);

        if (user.getDepartmentId() != null && departmentDAO.findById(user.getDepartmentId()) == null) {
            throw new ValidationException("The selected department does not exist.");
        }
    }
}