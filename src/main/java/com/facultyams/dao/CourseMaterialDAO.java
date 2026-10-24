package com.facultyams.dao;
import com.facultyams.config.DatabaseConnection;
import com.facultyams.model.CourseMaterial;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import com.facultyams.exception.DatabaseException;
public class CourseMaterialDAO {
    public List<CourseMaterial> getAllMaterials() {
        List<CourseMaterial> materials = new ArrayList<>();

        String sql = "SELECT materialID, courseID, description, notes, referenceText "
                + "FROM course_materials";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                CourseMaterial material = new CourseMaterial(
                        rs.getInt("materialID"),
                        rs.getInt("courseID"),
                        rs.getString("description"),
                        rs.getString("notes"),
                        rs.getString("referenceText")
                );

                materials.add(material);
            }

        } catch (SQLException | DatabaseException e) {
            throw new RuntimeException("Failed to retrieve course materials", e);
        }

        return materials;
        }
    public boolean addMaterial(CourseMaterial material) {
        String sql = "INSERT INTO course_materials "
                + "(courseID, description, notes, referenceText) "
                + "VALUES (?, ?, ?, ?)";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, material.getCourseID());
            ps.setString(2, material.getDescription());
            ps.setString(3, material.getNotes());
            ps.setString(4, material.getReferenceText());

            return ps.executeUpdate() > 0;

        } catch (SQLException | DatabaseException e) {
            throw new RuntimeException("Failed to add course material", e);
        }
    }

    // Update a course material
    public boolean updateMaterial(CourseMaterial material) {
        String sql = "UPDATE course_materials "
                + "SET courseID = ?, description = ?, notes = ?, referenceText = ? "
                + "WHERE materialID = ?";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, material.getCourseID());
            ps.setString(2, material.getDescription());
            ps.setString(3, material.getNotes());
            ps.setString(4, material.getReferenceText());
            ps.setInt(5, material.getMaterialID());

            return ps.executeUpdate() > 0;

        } catch (SQLException | DatabaseException e) {
            throw new RuntimeException("Failed to update course material", e);
        }
    }
}


//getAllMaterials() — retrieves all course materials from MySQL

//addMaterial() — inserts a new course material record.

//updateMaterial() - updates an existing record using its materialID.


