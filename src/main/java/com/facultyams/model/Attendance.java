package com.facultyams.model;

import java.time.LocalDate;

public class Attendance {
    private int attendanceId;
    private int userId;
    private int componentId;
    private int sessionNo;
    private LocalDate date;
    private String status;

public Attendance(){}

public Attendance(int userId,int componentId,int sessionNo,LocalDate date,String status){
    this.userId=userId;
    this.componentId=componentId;
    setSessionNo(sessionNo);
    this.date=date;
    this.status=status;
}

public Attendance(int attendanceId,int userId,int componentId,int sessionNo,LocalDate date,String status){
    this.attendanceId=attendanceId;
    this.userId=userId;
    this.componentId=componentId;
    setSessionNo(sessionNo);
    this.date=date;
    this.status=status;
}

 public int getAttendanceId() {
        return attendanceId;
    }

public void setAttendanceId(int attendanceId) {
        this.attendanceId=attendanceId;
    }

public int getUserId() {
        return userId;
    }

public void setUserId(int userId) {
        this.userId=userId;
    }

public int getComponentId() {
        return componentId;
    }

public void setComponentId(int componentId) {
        this.componentId=componentId;
    }

public int getSessionNo() {
        return sessionNo;
    }

public void setSessionNo(int sessionNo) {
    if(sessionNo>15||sessionNo<1){
        throw new IllegalArgumentException("session must be between 1 and 15");
    }
        this.sessionNo=sessionNo;
    }

public LocalDate getDate() {
        return date;
    }

public void setDate(LocalDate date) {
        this.date=date;
    }

public String getStatus() {
        return status;
    }

public void setStatus(String status) {
        this.status=status;
    }
}