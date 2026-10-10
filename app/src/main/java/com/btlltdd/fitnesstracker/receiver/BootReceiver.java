package com.btlltdd.fitnesstracker.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.btlltdd.fitnesstracker.database.WorkoutDao;
import com.btlltdd.fitnesstracker.model.WorkoutSchedule;
import com.btlltdd.fitnesstracker.util.AlarmScheduler;

/** Sau khi máy khởi động, AlarmManager bị xóa nên đặt lại các lịch đang bật. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }
        for (WorkoutSchedule schedule : new WorkoutDao(context).enabledSchedules()) {
            AlarmScheduler.schedule(context, schedule);
        }
    }
}
