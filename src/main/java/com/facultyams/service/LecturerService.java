package com.facultyams.service;
import com.facultyams.dao.LecturerDAO;
import com.facultyams.model.Lecturer;
public class LecturerService {
    private final LecturerDAO lecturerDAO;
        public LecturerService(){
            lecturerDAO = new LecturerDAO();
        }

    public Lecturer findByLecturerID(int lecturerID) {

        if (lecturerID <= 0) { //validates the id
            throw new IllegalArgumentException(
                    "Lecturer ID must be greater than 0"
            );
        }

        Lecturer lecturer = lecturerDAO.findByLecturerID(lecturerID);//search the database

        if (lecturer == null) {//checks whether if lecturer exists
            throw new IllegalArgumentException("Lecturer not found");
        }

        return lecturer;
    }
        public void updateProfile(int userID, String fullName, String email,
                String phone, String profilePicture) {

            if (userID <= 0) {
                throw new IllegalArgumentException("Invalid User ID");
            }

            if (fullName == null || fullName.trim().isEmpty()) {
                throw new IllegalArgumentException("Name cannot be empty");
            }

            if (email == null || email.trim().isEmpty()) {
                throw new IllegalArgumentException("Email cannot be empty");
            }

            if (phone == null || phone.trim().isEmpty()) {
                throw new IllegalArgumentException("Phone cannot be empty");
            }

            lecturerDAO.updateProfile(
                    userID,
                    fullName.trim(),
                    email.trim(),
                    phone.trim(),
                    profilePicture
            );
        }
    }






//This is act as a middle layer between dashboard and database access