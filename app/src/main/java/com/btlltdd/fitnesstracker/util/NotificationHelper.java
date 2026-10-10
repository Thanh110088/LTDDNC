package com.btlltdd.fitnesstracker.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.btlltdd.fitnesstracker.R;

public final class NotificationHelper {
    public static final String CHANNEL_WORKOUT = "workout_running";
    public static final String CHANNEL_REMINDER = "workout_reminder";
    public static final String CHANNEL_GOAL = "workout_goal";
    public static final int WORKOUT_ID = 1001;

    private NotificationHelper() {
    }

    public static void createChannels(Context context) {
        if (Build.VERSION.SDK_INT < 26) {
            return;
        }
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        NotificationChannel workout = new NotificationChannel(
                CHANNEL_WORKOUT, "Buổi tập", NotificationManager.IMPORTANCE_LOW);
        workout.setDescription("Bấm giờ khi ứng dụng nằm nền hoặc tắt màn hình");
        NotificationChannel reminder = new NotificationChannel(
                CHANNEL_REMINDER, "Nhắc lịch tập", NotificationManager.IMPORTANCE_HIGH);
        NotificationChannel goal = new NotificationChannel(
                CHANNEL_GOAL, "Mục tiêu", NotificationManager.IMPORTANCE_HIGH);
        manager.createNotificationChannel(workout);
        manager.createNotificationChannel(reminder);
        manager.createNotificationChannel(goal);
    }

    public static void reminder(Context context, int scheduleId, String title) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_REMINDER)
                .setSmallIcon(R.drawable.ic_stat_workout)
                .setContentTitle("Đến giờ tập")
                .setContentText(title == null || title.isEmpty() ? "Bạn có một lịch tập" : title)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(2000 + Math.abs(scheduleId), builder.build());
    }

    public static void goal(Context context, String type, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_GOAL)
                .setSmallIcon(R.drawable.ic_stat_workout)
                .setContentTitle("Hoàn thành mục tiêu")
                .setContentText(message)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(3000 + Math.abs(type.hashCode() % 500), builder.build());
    }
}
