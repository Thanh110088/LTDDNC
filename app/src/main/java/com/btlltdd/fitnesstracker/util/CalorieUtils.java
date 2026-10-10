package com.btlltdd.fitnesstracker.util;

/**
 * R19: Calories = MET * cân nặng (kg) * thời gian (giờ).
 */
public final class CalorieUtils {
    private CalorieUtils() {
    }

    public static double burn(double met, double weightKg, int durationSec) {
        if (met <= 0) {
            met = 6;
        }
        if (weightKg <= 0) {
            weightKg = 60;
        }
        if (durationSec < 0) {
            durationSec = 0;
        }
        return met * weightKg * (durationSec / 3600.0);
    }
}
