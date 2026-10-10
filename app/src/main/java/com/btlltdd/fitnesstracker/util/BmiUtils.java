package com.btlltdd.fitnesstracker.util;

public final class BmiUtils {
    private BmiUtils() {
    }

    public static double bmi(double heightCm, double weightKg) {
        if (heightCm <= 0 || weightKg <= 0) {
            return 0;
        }
        double meters = heightCm / 100.0;
        double value = weightKg / (meters * meters);
        return Math.round(value * 10.0) / 10.0;
    }

    public static String category(double bmi) {
        if (bmi <= 0) {
            return "Chưa đủ số đo";
        }
        if (bmi < 18.5) {
            return "Thiếu cân";
        }
        if (bmi < 25) {
            return "Bình thường";
        }
        if (bmi < 30) {
            return "Thừa cân";
        }
        return "Béo phì";
    }
}
