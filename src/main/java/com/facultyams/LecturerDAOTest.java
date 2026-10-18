package com.facultyams;
import com.facultyams.dao.LecturerDAO;
import com.facultyams.model.Lecturer;
public class LecturerDAOTest {
    public static void main(String[] args) {
        LecturerDAO lecturerDAO = new LecturerDAO();
        Lecturer lecturer = lecturerDAO.findByLecturerID(1);
        if (lecturer != null) {
            System.out.println("Lecturer found");
            System.out.println("Lecturer ID: " + lecturer.getLecturerID());
            System.out.println("Designation: " + lecturer.getDesignation());
            System.out.println("Specialisation: " + lecturer.getSpecialisation());
        } else {
            System.out.println("Lecturer not found");
        }
    }


    }


