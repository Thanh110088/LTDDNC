package com.btlltdd.fitnesstracker.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.btlltdd.fitnesstracker.database.WorkoutDao;
import com.btlltdd.fitnesstracker.model.WorkoutSchedule;
import com.btlltdd.fitnesstracker.util.AlarmScheduler;
import com.btlltdd.fitnesstracker.util.NotificationHelper;

/** R12: AlarmManager gửi tới receiver này để hiện thông báo nhắc tập. */
public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra("title");
        long scheduleId = intent.getLongExtra("scheduleId", 0);
        NotificationHelper.reminder(context, (int) scheduleId, title);
        WorkoutSchedule schedule = new WorkoutDao(context).getSchedule(scheduleId);
        if (schedule != null && schedule.enabled && !"once".equals(schedule.repeatType)) {
            AlarmScheduler.schedule(context, schedule);
        }
    }
}
