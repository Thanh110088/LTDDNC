package com.btlltdd.fitnesstracker.model;

public class WorkoutSession {
    public long id;
    public long userId;
    public long planId;
    public String title = "";
    public String startTime = "";
    public String endTime = "";
    public int durationSec;
    public int steps;
    public double distanceM;
    public double calories;
    public String status = "finished";
    public boolean synced;
}
