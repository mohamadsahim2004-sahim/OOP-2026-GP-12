package com.facultyams.model;

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
    public String getSpecialisation(){
        return specialisation;
    }
    public void setSpecialisation(String specialisation){
        this.specialisation=specialisation;
    }
}
