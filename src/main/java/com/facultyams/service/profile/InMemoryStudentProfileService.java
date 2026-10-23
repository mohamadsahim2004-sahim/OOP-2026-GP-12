package com.facultyams.service.profile;

import com.facultyams.model.Student;
import com.facultyams.service.DataService;
import java.util.Objects;

/** In-memory profile service adapter, replaceable by a JDBC-backed implementation. */
public final class InMemoryStudentProfileService implements StudentProfileService {
    private final DataService dataService;
    private final PasswordService passwordService;

    /** Creates the adapter for the application's shared data service. */
    public InMemoryStudentProfileService(DataService dataService) {
        this(dataService, new SaltedSha256PasswordService());
    }

    /** Creates the adapter with a replaceable password hashing strategy. */
    public InMemoryStudentProfileService(DataService dataService, PasswordService passwordService) {
        this.dataService = Objects.requireNonNull(dataService);
        this.passwordService = Objects.requireNonNull(passwordService);
    }

    @Override public Student getProfile() {
        return dataService.getCurrentStudent();
    }

    @Override public void updateContactInfo(String email, String phone, String address,
                                            String emergencyNumber, String relationship) {
        dataService.updateStudentContactInfo(email, phone, address, emergencyNumber, relationship);
    }

    @Override public void updatePhoto(String imagePath) {
        dataService.updateStudentPhoto(imagePath);
    }

    @Override public void changePassword(char[] currentPassword, char[] newPassword) {
        Student student = dataService.getCurrentStudent();
        passwordService.changePassword(student.getPassword(), currentPassword, newPassword, student::setPassword);
    }
}
