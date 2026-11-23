package com.example.myapplication.model;

public class Student {
    public long id;
    public String roll;
    public String name;
    public boolean present = true;
    public int total;
    public int presentCount;

    public int percent() {
        return total == 0 ? 0 : Math.round(presentCount * 100f / total);
    }
}