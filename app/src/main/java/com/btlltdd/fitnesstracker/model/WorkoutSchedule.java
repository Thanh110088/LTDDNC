package com.btlltdd.fitnesstracker.model;

public class WorkoutSchedule {
    public long id;
    public long userId;
    public long planId;
    public String title = "";
    public String scheduleTime = "";
    public String repeatType = "once";
    public int weekday;
    public int alarmCode;
    public boolean enabled = true;
}
