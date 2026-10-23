package com.facultyams.service;
import com.facultyams.dao.CourseMaterialDAO;
import com.facultyams.model.CourseMaterial;

import java.util.List;
public class CourseMaterialService {
    private final CourseMaterialDAO courseMaterialDAO=
            new CourseMaterialDAO();


    public List<CourseMaterial>getAllMaterials(){
        return courseMaterialDAO.getAllMaterials();
    }

    public boolean addMaterial(CourseMaterial material){
        if(material == null){
            throw new IllegalArgumentException(
                    "Course material cannot be null"
            );
        }
        return courseMaterialDAO.addMaterial(material);
    }
    public boolean updateMaterial(CourseMaterial material){
        if(material==null){
            throw new IllegalArgumentException(
                    "Course material cannot be null"
            );
        }

        if(material.getMaterialID()<=0){
            throw new IllegalArgumentException(
                    "valid material ID is required"
            );
        }
        return courseMaterialDAO.updateMaterial(material);
    }
}
