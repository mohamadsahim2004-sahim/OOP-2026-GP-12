package com.facultyams.service;
import com.facultyams.model.Marks;
import com.facultyams.dao.MarksDAO;
import java.sql.SQLException;
public class MarksService {
    private final MarksDAO marksDAO = new MarksDAO();
    public boolean isValidateMarks(double marks){
        return marks >= 0 && marks <= 100;      //validate marks
    }
    public boolean isCAEligible(double caMarks) {
        if (!isValidateMarks(caMarks)){
            throw new IllegalArgumentException(
                    "Marks should be between 0 and 100"  //Check CA eligibility
            );
    }
        return caMarks >= 40;
    }

    //in here checking that examination eligiblity using CA &  attendance
    public boolean isExamEligiblity(
            double caMarks,double attendancePercentage) {

        if (!isValidateMarks(caMarks)) {
            throw new IllegalArgumentException(
                    "CA marks should be between 0 & 100"
            );
        }

        if (!isValidateMarks(attendancePercentage)) {
            throw new IllegalArgumentException(
                    "Attendance should be between 0 & 100"
            );

        }
        return isCAEligible(caMarks) && attendancePercentage >= 80;
    }
    public boolean validateMarksObject(Marks marks){
            if(marks == null) {
                throw new IllegalArgumentException(
                        "Marks can not be null"
                );
            }
            if(!isValidateMarks(marks.getObtainMarks())){
                throw new IllegalArgumentException(
                        "Obtained marks should be between 0 & 100"
                );

            }
                if(!isValidateMarks(marks.getFinalMarks())){
                    throw new IllegalArgumentException(
                            "Final marks should be between 0 & 100"
                    );

                }
                return true;
            }
    public Marks findMarksById(int marksID) throws SQLException {
        if (marksID <= 0) {
            throw new IllegalArgumentException(
                    "Marks ID must be greater than zero."
            );
        }

        return marksDAO.findMarksById(marksID);
    }
     public void saveMarks(Marks marks){
        validateMarksObject(marks);
        try{
            marksDAO.saveMarks(marks);
        }catch (SQLException e){
            throw new RuntimeException("Failed t save marks.",e);

        }
     }
        }

        //code for validate marks and check exam eligibility


