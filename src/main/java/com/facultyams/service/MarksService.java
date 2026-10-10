package com.facultyams.service;
import com.facultyams.model.Marks;
public class MarksService {
    public boolean isValidateMarks(double marks){
        return marks >= 0 && marks <= 100;      //validate marks
    }
    public boolean isCAEligible(double caMarks){
        if(!isValidateMarks(caMarks)){
            throw new IllegalArgumentException(
                    "Marks should be between 0 and 100");   //Check CA eligibility
        }
        return caMarks >= 40;
    }

    //in here checking that examination eligiblity using CA &  attendance
    public boolean isExamEligiblity(
            double caMarks,double attendancePercentage) {

        if (!isValidateMarks(caMarks)) {
            throw new IllegalArgumentException(
                    "CA makrs should be between 0 & 100");
        }

        if (!isValidateMarks(attendancePercentage)) {
            throw new IllegalAccessException(
                    "Attendance should be between 0 & 100");

        }
        return isCAEligiblity(caMarks) && attendancePercentage >= 80;
    }
        return isCAEligible(caMarks){
            if(marks == null) {
                throw new IllegalAccessException(
                        "Marks can not be null");
            }
            if(!isValidateMarks(marks.getObtainMarks())){
                throw new IllegalAccessException(
                        "Obtained marks should be between 0 & 100");

            }
                if(!isValidateMarks(marks.getFinalMarks())){
                    throw new IllegalAccessException(
                            "Final marks should be between 0 & 100");

                }
            }
        }



