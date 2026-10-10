package com.btlltdd.fitnesstracker.model;

public class User {
    public long id;
    public String username = "";
    public String fullName = "";
    public String role = "user";
    public double heightCm;
    public double weightKg;
    public String goal = "maintain";
    public int targetSteps = 8000;
    public double targetCalories = 300;
    public double targetDistanceKm = 3;
    public boolean active = true;
    public boolean synced;

    public boolean isAdmin() {
        return "admin".equals(role);
    }
}
