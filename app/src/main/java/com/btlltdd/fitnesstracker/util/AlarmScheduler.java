package com.btlltdd.fitnesstracker.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.btlltdd.fitnesstracker.model.WorkoutSchedule;
import com.btlltdd.fitnesstracker.receiver.ReminderReceiver;

import java.util.Calendar;
import java.util.Date;

/**
 * R12: AlarmManager bắn Intent tới ReminderReceiver.
 * API 31+ có thể không cho exact alarm; khi đó dùng set() để demo vẫn có thông báo.
 */
public final class AlarmScheduler {
    private AlarmScheduler() {
    }

    public static int newCode() {
        return (int) (System.currentTimeMillis() % 1_000_000_000L);
    }

    public static String schedule(Context context, WorkoutSchedule schedule) {
        long trigger = nextTrigger(schedule);
        if (trigger <= 0) {
            return "Giờ này đã qua. Hãy chọn thời điểm trong tương lai.";
        }
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pending = pending(context, schedule);
        try {
            if (Build.VERSION.SDK_INT >= 31 && !manager.canScheduleExactAlarms()) {
                manager.set(AlarmManager.RTC_WAKEUP, trigger, pending);
            } else if (Build.VERSION.SDK_INT >= 23) {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending);
            } else {
                manager.setExact(AlarmManager.RTC_WAKEUP, trigger, pending);
            }
        } catch (SecurityException exception) {
            manager.set(AlarmManager.RTC_WAKEUP, trigger, pending);
        }
        return "Đã đặt nhắc lúc " + TimeUtil.display(TimeUtil.format(new Date(trigger)));
    }

    public static void cancel(Context context, int alarmCode) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ReminderReceiver.class);
        PendingIntent pending = PendingIntent.getBroadcast(
                context,
                alarmCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        manager.cancel(pending);
    }

    public static long nextTrigger(WorkoutSchedule schedule) {
        Date stored;
        try {
            stored = TimeUtil.parse(schedule.scheduleTime);
        } catch (Exception exception) {
            return -1;
        }
        if ("once".equals(schedule.repeatType)) {
            return stored.getTime() > System.currentTimeMillis() ? stored.getTime() : -1;
        }
        Calendar clock = Calendar.getInstance();
        clock.setTime(stored);
        Calendar fire = Calendar.getInstance();
        fire.set(Calendar.SECOND, 0);
        fire.set(Calendar.MILLISECOND, 0);
        fire.set(Calendar.HOUR_OF_DAY, clock.get(Calendar.HOUR_OF_DAY));
        fire.set(Calendar.MINUTE, clock.get(Calendar.MINUTE));
        if ("weekly".equals(schedule.repeatType)) {
            int target = schedule.weekday > 0 ? schedule.weekday : clock.get(Calendar.DAY_OF_WEEK);
            for (int i = 0; i < 8; i++) {
                if (fire.get(Calendar.DAY_OF_WEEK) == target && fire.getTimeInMillis() > System.currentTimeMillis()) {
                    return fire.getTimeInMillis();
                }
                fire.add(Calendar.DAY_OF_YEAR, 1);
            }
        }
        if (fire.getTimeInMillis() <= System.currentTimeMillis()) {
            fire.add(Calendar.DAY_OF_YEAR, 1);
        }
        return fire.getTimeInMillis();
    }

    private static PendingIntent pending(Context context, WorkoutSchedule schedule) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra("title", schedule.title);
        intent.putExtra("scheduleId", schedule.id);
        intent.putExtra("repeat", schedule.repeatType);
        return PendingIntent.getBroadcast(
                context,
                schedule.alarmCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
