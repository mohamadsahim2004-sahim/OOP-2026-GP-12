package com.facultyams;
import com.facultyams.config.DatabaseConnection;
import java.sql.Connection;
import java.sql.SQLException;
public class DatabaseTest {
    public static void main(String[] args) {
        try(Connection connection = DatabaseConnection.getConnection()){
            if(connection != null) {
                System.out.println("Database connected successfully");
            }

            }
        catch(SQLException e){
            System.out.println("Database connection failed!");
            e.printStackTrace();
        }
    }
}
