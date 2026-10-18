package com.facultyams.model;

public abstract class User {   //srs variables
    private int userID;
    private String username;
    private String password;
    private String role;
    private String fullName;
    private String email;
    private String phone;
    private String profilePicture;
    private int departmentID;

    //"protected" is for allow child classes(lecturer)to call this constructor(User)
    protected User(int userID, String username, String password, String role, String fullName, String email, String phone, String profilePicture, int departmentID) {
        this.userID = userID;
        this.username = username;
        this.password = password;
        this.role = role;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.profilePicture = profilePicture;
        this.departmentID = departmentID;

    }

    //variables are private therefor getters and setters for access them
    //getters for read and setters for update
    public int getUserID() {
        return userID;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getRole() {
        return role;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }
}
