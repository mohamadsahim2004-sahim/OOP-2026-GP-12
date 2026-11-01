package com.facultyams.model;

/** A department of the Faculty of Technology. */
public class Department {

    private int departmentId;
    private String departmentCode;
    private String departmentName;

    public Department() {
    }

    public Department(int departmentId, String departmentCode, String departmentName) {
        this.departmentId = departmentId;
        this.departmentCode = departmentCode;
        this.departmentName = departmentName;
    }

    public int getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(int departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentCode() {
        return departmentCode;
    }

    public void setDepartmentCode(String departmentCode) {
        this.departmentCode = departmentCode;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    /** Used by combo boxes in the GUI. */
    @Override
    public String toString() {
        return departmentCode + " - " + departmentName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Department)) {
            return false;
        }
        return departmentId == ((Department) o).departmentId;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(departmentId);
    }
}