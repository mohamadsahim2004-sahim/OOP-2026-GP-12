package com.facultyams.model;

<<<<<<< HEAD
import com.facultyams.security.Role;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Lecturer. Can update their profile except username and password.
 * Lecturer-specific screens/services are implemented by Member 2.
 */
public class Lecturer extends User {

    private static final Set<ProfileField> EDITABLE =
            Collections.unmodifiableSet(EnumSet.allOf(ProfileField.class));

    private String designation = "Lecturer";

    public Lecturer() {
        super();
    }

    public Lecturer(int userId, String username, String fullName, String email, String designation) {
        super(userId, username, fullName, email);
        this.designation = designation;
    }

    @Override
    public Role getRole() {
        return Role.LECTURER;
    }

    @Override
    public Set<ProfileField> getEditableProfileFields() {
        return EDITABLE;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }
}
=======
public class Lecturer extends User{//lecturer inherits common fields from User
    private int lecturerID;
    private String designation;
    private String specialisation;

    public Lecturer(int userID,String username,String password,String role,
                    String fullName,String email,String phone,
                    String profilePicture,int departmentID,
                    int lecturerID,String designation,String specialisation){
//"super" calls the User constructor and passes the user infrmation
        super(userID, username, password, role, fullName, email, phone, profilePicture, departmentID);

        this.lecturerID=lecturerID;
        this.designation=designation;
        this.specialisation=specialisation;

    }
    public int getLecturerID(){
        return lecturerID;
    }
    public String getDesignation(){
        return designation;
    }
    public void setDesignation(String designation){
        this.designation = designation;
    }
    public String getSpecialisation(){
        return specialisation;
    }
    public void setSpecialisation(String specialisation){
        this.specialisation=specialisation;
    }
}
>>>>>>> eae9b8deb3b78eb767a318e49b2dbc95081b07aa
