package com.facultyams;

/**
 * Obsolete. This class used to run CREATE TABLE IF NOT EXISTS users with a layout (password, phone,
 * profilePicture, departmentID) that no DAO in the application uses; on an empty database it would have
 * created a users table the application cannot work with, and every later script would then have kept it.
 *
 * The users table - and every other table - is created ONLY by database/schema.sql and the migrations in
 * database/migrations (see README.md). This class no longer touches the database.
 */
public class CreateUsersTable {

    public static void main(String[] args) {
        System.out.println("CreateUsersTable is obsolete and does nothing.");
        System.out.println("Create the database objects with database/schema.sql and database/migrations/*.sql (README.md).");
    }
}