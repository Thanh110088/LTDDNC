package com.btlltdd.fitnesstracker.model;

public class Exercise {
    public long id;
    public String name = "";
    public String description = "";
    public String muscleGroup = "full_body";
    public String difficulty = "beginner";
    public String equipment = "none";
    public String imageRes = "";
    public String videoUrl = "";
    public double met = 5;
    public boolean hidden;
    public boolean bookmarked;
}
