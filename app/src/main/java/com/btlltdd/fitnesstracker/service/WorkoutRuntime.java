package com.btlltdd.fitnesstracker.service;

import com.btlltdd.fitnesstracker.util.CalorieUtils;

/** Trạng thái buổi tập trong cùng process, để UI vẽ ngay trước broadcast đầu tiên. */
public final class WorkoutRuntime {
    public static boolean active;
    public static boolean running;
    public static long elapsedMs;
    public static long accumulatedMs;
    public static long runStartedAt;
    public static int steps;
    public static double distanceM;
    public static long sessionId;
    public static long planId;
    public static long userId;
    public static String title = "";
    public static String stepSource = "";
    public static double met = 6;
    public static double weightKg = 60;
    public static boolean locationOn;

    private WorkoutRuntime() {
    }

    public static double liveCalories() {
        return CalorieUtils.burn(met, weightKg, (int) (elapsedMs / 1000));
    }

    public static void reset() {
        active = false;
        running = false;
        elapsedMs = 0;
        accumulatedMs = 0;
        runStartedAt = 0;
        steps = 0;
        distanceM = 0;
        sessionId = 0;
        planId = 0;
        userId = 0;
        title = "";
        stepSource = "";
        met = 6;
        weightKg = 60;
        locationOn = false;
    }
}
