package com.example.myapplication.model;

public class AttendanceRecord {
    public String roll;
    public String date;
    public boolean present;

    public AttendanceRecord(String roll, String date, boolean present) {
        this.roll = roll;
        this.date = date;
        this.present = present;
    }
}