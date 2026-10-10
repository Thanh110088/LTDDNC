package com.btlltdd.fitnesstracker.model;

public class WorkoutPlan {
    public long id;
    public long userId;
    public String name = "";
    public String description = "";
    public String goal = "maintain";
    public double bmiMin;
    public double bmiMax = 99;
    public String level = "beginner";
    public boolean template;
    public boolean hidden;
}
