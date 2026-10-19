package com.facultyams.service;
import com.facultyams.dao.LecturerDAO;
import com.facultyams.model.Lecturer;
public class LecturerService {
    private final LecturerDAO lecturerDAO;
        public LecturerService(){
            lecturerDAO = new LecturerDAO();
        }
public Lecturer findByLecturerID(int lecturerID) {
    if (lecturerID <= 0) {
        throw new IllegalArgumentException(
                "Lecturer ID must be greater than 0"

        );
    }
    return lecturerDAO.findByLecturerID(lecturerID);


}
    }

//This is act as a middle layer between dashboard and database access