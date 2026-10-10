package com.btlltdd.fitnesstracker.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Nhận broadcast nội bộ từ WorkoutForegroundService.
 * Đăng ký bằng code trong màn hình, không khai báo manifest.
 */
public class WorkoutUpdateReceiver extends BroadcastReceiver {
    public interface Listener {
        void onWorkoutUpdate(Intent intent);
    }

    private final Listener listener;

    public WorkoutUpdateReceiver(Listener listener) {
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (listener != null && intent != null) {
            listener.onWorkoutUpdate(intent);
        }
    }
}
