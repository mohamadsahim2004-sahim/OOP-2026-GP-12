package com.facultyams;

import com.facultyams.model.Lecturer;
import com.facultyams.service.LecturerService;

public class LecturerServiceTest {

    public static void main(String[] args) {

        LecturerService service = new LecturerService();

        try {
            Lecturer lecturer = service.findByLecturerID(2);

            System.out.println("Lecturer Service Test Passed");
            System.out.println("Lecturer ID: " + lecturer.getLecturerID());
            System.out.println("Designation: " + lecturer.getDesignation());
            System.out.println("Specialisation: " + lecturer.getSpecialisation());

        } catch (Exception e) {
            System.out.println("Lecturer Service Test Failed");
            System.out.println(e.getMessage());
        }
    }
}

//to test the LecturerService
