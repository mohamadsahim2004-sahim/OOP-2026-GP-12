package com.facultyams.dao;

import com.facultyams.config.DatabaseConnection; //to build connection between java and SQL db
import com.facultyams.model.Marks;
import java.sql.Connection; // to import connection
public class MarksDAO {
    private Connection connection;   //this creates a variable that hods the database connection

    public MarksDAO(){    //when MarksDAO object is created then this constructor gets the java-SQL connection
                          //when marksDA object is created
        connection =DatabaseConnection.getConnection();
    }
    public void saveMarks(Marks marks){
        String sql = "INSERT INTO marks (marksid, obtainMarks, finalMarks, grade, isEligible) VALUES (?, ?, ?, ?, ?)";
    }
}
