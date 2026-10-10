package com.btlltdd.fitnesstracker.util;

import android.content.Context;

import com.btlltdd.fitnesstracker.database.UserDao;
import com.btlltdd.fitnesstracker.database.WorkoutDao;
import com.btlltdd.fitnesstracker.model.User;

/** R23: so sánh kết quả trong ngày với mục tiêu và bắn thông báo một lần. */
public final class GoalChecker {
    private GoalChecker() {
    }

    public static void check(Context context, long userId) {
        User user = new UserDao(context).get(userId);
        if (user == null) {
            return;
        }
        double[] today = new WorkoutDao(context).todayTotals(userId);
        celebrate(context, userId, "steps", today[0], user.targetSteps, "bước chân");
        celebrate(context, userId, "calories", today[1], user.targetCalories, "calo");
        celebrate(context, userId, "distance", today[2], user.targetDistanceKm, "quãng đường");
    }

    private static void celebrate(Context context, long userId, String type, double actual, double target, String label) {
        if (target <= 0 || actual < target) {
            return;
        }
        boolean firstTime = new WorkoutDao(context).markGoal(userId, type, actual);
        if (firstTime) {
            NotificationHelper.goal(context, type, "Chúc mừng! Bạn đã đạt mục tiêu " + label + " hôm nay.");
        }
    }
}
