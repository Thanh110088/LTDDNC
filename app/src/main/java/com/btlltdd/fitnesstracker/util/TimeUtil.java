package com.btlltdd.fitnesstracker.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class TimeUtil {
    public static final String STORE = "yyyy-MM-dd HH:mm:ss";

    private TimeUtil() {
    }

    public static String now() {
        return format(new Date());
    }

    public static String format(Date date) {
        return new SimpleDateFormat(STORE, Locale.US).format(date);
    }

    public static Date parse(String value) throws ParseException {
        return new SimpleDateFormat(STORE, Locale.US).parse(value);
    }

    public static String display(String stored) {
        try {
            Date date = parse(stored);
            return new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(date);
        } catch (Exception exception) {
            return stored == null ? "" : stored;
        }
    }

    public static String dayKey(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date);
    }

    public static String todayKey() {
        return dayKey(new Date());
    }

    public static String shortDay(Date date) {
        return new SimpleDateFormat("dd/MM", Locale.US).format(date);
    }

    public static String clock(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        if (hours > 0) {
            return String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format(Locale.US, "%02d:%02d", minutes, seconds);
    }
}
