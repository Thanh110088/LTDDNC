package com.btlltdd.fitnesstracker.model;

public class PlanExercise {
    public long id;
    public long planId;
    public long exerciseId;
    public String exerciseName = "";
    public int weekday;
    public int sets = 3;
    public int reps = 10;
    public int durationSec;
    public int restSec = 60;
    public int sortOrder;
    public double met = 5;
}
