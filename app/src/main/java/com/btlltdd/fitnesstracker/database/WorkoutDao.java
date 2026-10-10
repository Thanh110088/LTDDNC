package com.btlltdd.fitnesstracker.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.btlltdd.fitnesstracker.model.DayStat;
import com.btlltdd.fitnesstracker.model.PlanExercise;
import com.btlltdd.fitnesstracker.model.WorkoutPlan;
import com.btlltdd.fitnesstracker.model.WorkoutSchedule;
import com.btlltdd.fitnesstracker.model.WorkoutSession;
import com.btlltdd.fitnesstracker.util.CalorieUtils;
import com.btlltdd.fitnesstracker.util.TimeUtil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/** R09-R12, R15-R23 phần lưu nhật ký. */
public class WorkoutDao {
    private final SQLiteDatabase db;

    public WorkoutDao(Context context) {
        db = DatabaseHelper.getInstance(context).getWritableDatabase();
    }

    public List<WorkoutPlan> myPlans(long userId) {
        return plans("SELECT * FROM workout_plans WHERE user_id = ? AND is_template = 0 ORDER BY id DESC",
                new String[]{String.valueOf(userId)});
    }

    public List<WorkoutPlan> suggest(String goal, double bmi) {
        return plans("SELECT * FROM workout_plans WHERE is_template = 1 AND is_hidden = 0 "
                        + "AND goal = ? AND bmi_min <= ? AND bmi_max >= ? ORDER BY id",
                new String[]{goal, String.valueOf(bmi), String.valueOf(bmi)});
    }

    public List<WorkoutPlan> templatesForGoal(String goal) {
        return plans("SELECT * FROM workout_plans WHERE is_template = 1 AND is_hidden = 0 AND goal = ? ORDER BY id",
                new String[]{goal});
    }

    public List<WorkoutPlan> visibleTemplates() {
        return plans("SELECT * FROM workout_plans WHERE is_template = 1 AND is_hidden = 0 ORDER BY id", null);
    }

    public List<WorkoutPlan> adminTemplates() {
        return plans("SELECT * FROM workout_plans WHERE is_template = 1 ORDER BY id DESC", null);
    }

    public WorkoutPlan getPlan(long id) {
        List<WorkoutPlan> plans = plans("SELECT * FROM workout_plans WHERE id = ?", new String[]{String.valueOf(id)});
        return plans.isEmpty() ? null : plans.get(0);
    }

    public long savePlan(WorkoutPlan plan) {
        ContentValues values = new ContentValues();
        if (plan.template) {
            values.putNull("user_id");
            values.put("is_template", 1);
        } else {
            values.put("user_id", plan.userId);
            values.put("is_template", 0);
        }
        values.put("name", plan.name);
        values.put("description", plan.description);
        values.put("goal", plan.goal);
        values.put("bmi_min", plan.bmiMin);
        values.put("bmi_max", plan.bmiMax);
        values.put("level", plan.level == null || plan.level.isEmpty() ? "beginner" : plan.level);
        values.put("is_hidden", plan.hidden ? 1 : 0);
        values.put("is_synced", 0);
        if (plan.id > 0) {
            db.update("workout_plans", values, "id = ?", new String[]{String.valueOf(plan.id)});
            return plan.id;
        }
        return db.insert("workout_plans", null, values);
    }

    public void setPlanHidden(long id, boolean hidden) {
        ContentValues values = new ContentValues();
        values.put("is_hidden", hidden ? 1 : 0);
        db.update("workout_plans", values, "id = ?", new String[]{String.valueOf(id)});
    }

    public void deletePlan(long id) {
        db.delete("workout_plans", "id = ?", new String[]{String.valueOf(id)});
    }

    public long copyTemplate(long templateId, long userId) {
        WorkoutPlan source = getPlan(templateId);
        if (source == null) {
            return -1;
        }
        db.beginTransaction();
        try {
            WorkoutPlan copy = new WorkoutPlan();
            copy.userId = userId;
            copy.name = source.name;
            copy.description = source.description;
            copy.goal = source.goal;
            copy.bmiMin = source.bmiMin;
            copy.bmiMax = source.bmiMax;
            copy.level = source.level;
            copy.template = false;
            long newId = savePlan(copy);
            for (PlanExercise item : exercisesOf(templateId)) {
                item.id = 0;
                item.planId = newId;
                insertExercise(item);
            }
            db.setTransactionSuccessful();
            return newId;
        } finally {
            db.endTransaction();
        }
    }

    public List<PlanExercise> exercisesOf(long planId) {
        List<PlanExercise> items = new ArrayList<>();
        Cursor cursor = db.rawQuery(
                "SELECT pe.*, e.name AS exercise_name, e.met FROM plan_exercises pe "
                        + "JOIN exercises e ON e.id = pe.exercise_id WHERE pe.plan_id = ? "
                        + "ORDER BY pe.sort_order, pe.id",
                new String[]{String.valueOf(planId)});
        try {
            while (cursor.moveToNext()) {
                PlanExercise item = new PlanExercise();
                item.id = Db.lng(cursor, "id");
                item.planId = Db.lng(cursor, "plan_id");
                item.exerciseId = Db.lng(cursor, "exercise_id");
                item.exerciseName = Db.str(cursor, "exercise_name");
                item.weekday = Db.num(cursor, "weekday");
                item.sets = Db.num(cursor, "sets");
                item.reps = Db.num(cursor, "reps");
                item.durationSec = Db.num(cursor, "duration_sec");
                item.restSec = Db.num(cursor, "rest_sec");
                item.sortOrder = Db.num(cursor, "sort_order");
                item.met = Db.dbl(cursor, "met");
                items.add(item);
            }
        } finally {
            cursor.close();
        }
        return items;
    }

    public void replaceExercises(long planId, List<PlanExercise> items) {
        db.beginTransaction();
        try {
            db.delete("plan_exercises", "plan_id = ?", new String[]{String.valueOf(planId)});
            int order = 1;
            for (PlanExercise item : items) {
                item.planId = planId;
                item.sortOrder = order++;
                insertExercise(item);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public double averageMet(long planId) {
        Cursor cursor = db.rawQuery(
                "SELECT AVG(e.met) FROM plan_exercises pe JOIN exercises e ON e.id = pe.exercise_id WHERE pe.plan_id = ?",
                new String[]{String.valueOf(planId)});
        try {
            if (cursor.moveToFirst() && !cursor.isNull(0)) {
                return cursor.getDouble(0);
            }
            return 6;
        } finally {
            cursor.close();
        }
    }

    public long insertSchedule(WorkoutSchedule schedule) {
        ContentValues values = scheduleValues(schedule);
        return db.insert("schedules", null, values);
    }

    public List<WorkoutSchedule> schedulesOf(long userId) {
        return schedules("SELECT * FROM schedules WHERE user_id = ? ORDER BY schedule_time",
                new String[]{String.valueOf(userId)});
    }

    public List<WorkoutSchedule> enabledSchedules() {
        return schedules("SELECT * FROM schedules WHERE is_enabled = 1", null);
    }

    public WorkoutSchedule getSchedule(long id) {
        List<WorkoutSchedule> items = schedules("SELECT * FROM schedules WHERE id = ?", new String[]{String.valueOf(id)});
        return items.isEmpty() ? null : items.get(0);
    }

    public void setScheduleEnabled(long id, boolean enabled) {
        ContentValues values = new ContentValues();
        values.put("is_enabled", enabled ? 1 : 0);
        db.update("schedules", values, "id = ?", new String[]{String.valueOf(id)});
    }

    public void deleteSchedule(long id) {
        db.delete("schedules", "id = ?", new String[]{String.valueOf(id)});
    }

    public long openSession(long userId, long planId, String title) {
        ContentValues values = new ContentValues();
        values.put("user_id", userId);
        if (planId > 0) {
            values.put("plan_id", planId);
        } else {
            values.putNull("plan_id");
        }
        values.put("title", title);
        values.put("start_time", TimeUtil.now());
        values.put("status", "running");
        values.put("is_synced", 0);
        return db.insert("workout_sessions", null, values);
    }

    public void finishSession(long id, int durationSec, int steps, double distanceM, double calories) {
        ContentValues values = new ContentValues();
        values.put("end_time", TimeUtil.now());
        values.put("duration_sec", Math.max(durationSec, 1));
        values.put("steps", steps);
        values.put("distance_m", distanceM);
        values.put("calories", calories);
        values.put("status", "finished");
        values.put("is_synced", 0);
        db.update("workout_sessions", values, "id = ?", new String[]{String.valueOf(id)});
    }

    public void insertLog(long sessionId, long exerciseId, int sets, int reps, int durationSec) {
        ContentValues values = new ContentValues();
        values.put("session_id", sessionId);
        values.put("exercise_id", exerciseId);
        values.put("sets_done", sets);
        values.put("reps_done", reps);
        values.put("duration_sec", durationSec);
        db.insert("session_logs", null, values);
    }

    public void insertRoute(long sessionId, List<double[]> points) {
        if (points == null || points.isEmpty()) {
            return;
        }
        db.beginTransaction();
        try {
            for (double[] point : points) {
                ContentValues values = new ContentValues();
                values.put("session_id", sessionId);
                values.put("latitude", point[0]);
                values.put("longitude", point[1]);
                values.put("accuracy", point[2]);
                values.put("recorded_at", TimeUtil.format(new Date((long) point[3])));
                db.insert("route_points", null, values);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public int countRoute(long sessionId) {
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM route_points WHERE session_id = ?",
                new String[]{String.valueOf(sessionId)});
        try {
            cursor.moveToFirst();
            return cursor.getInt(0);
        } finally {
            cursor.close();
        }
    }

    public double[] todayTotals(long userId) {
        Cursor cursor = db.rawQuery(
                "SELECT COALESCE(SUM(steps),0), COALESCE(SUM(calories),0), COALESCE(SUM(distance_m),0) "
                        + "FROM workout_sessions WHERE user_id = ? AND status = 'finished' AND substr(start_time,1,10) = ?",
                new String[]{String.valueOf(userId), TimeUtil.todayKey()});
        try {
            cursor.moveToFirst();
            return new double[]{cursor.getDouble(0), cursor.getDouble(1), cursor.getDouble(2) / 1000.0};
        } finally {
            cursor.close();
        }
    }

    public List<WorkoutSession> recent(long userId, int limit) {
        List<WorkoutSession> items = new ArrayList<>();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM workout_sessions WHERE user_id = ? ORDER BY id DESC LIMIT " + limit,
                new String[]{String.valueOf(userId)});
        try {
            while (cursor.moveToNext()) {
                items.add(mapSession(cursor));
            }
        } finally {
            cursor.close();
        }
        return items;
    }

    public WorkoutSession getSession(long id) {
        Cursor cursor = db.rawQuery("SELECT * FROM workout_sessions WHERE id = ?", new String[]{String.valueOf(id)});
        try {
            if (!cursor.moveToFirst()) {
                return null;
            }
            return mapSession(cursor);
        } finally {
            cursor.close();
        }
    }

    public boolean hasOpenSession(long userId) {
        Cursor cursor = db.rawQuery(
                "SELECT id FROM workout_sessions WHERE user_id = ? AND status != 'finished' LIMIT 1",
                new String[]{String.valueOf(userId)});
        try {
            return cursor.moveToFirst();
        } finally {
            cursor.close();
        }
    }

    public void closeOpenSessions(long userId, double weightKg) {
        Cursor cursor = db.rawQuery(
                "SELECT id, duration_sec, steps, distance_m FROM workout_sessions WHERE user_id = ? AND status != 'finished'",
                new String[]{String.valueOf(userId)});
        try {
            while (cursor.moveToNext()) {
                int duration = cursor.getInt(1);
                if (duration < 1) {
                    duration = 60;
                }
                finishSession(cursor.getLong(0), duration, cursor.getInt(2), cursor.getDouble(3),
                        CalorieUtils.burn(6, weightKg, duration));
            }
        } finally {
            cursor.close();
        }
    }

    public List<DayStat> stats(long userId, boolean month) {
        List<DayStat> items = new ArrayList<>();
        Calendar day = Calendar.getInstance();
        if (!month) {
            day.add(Calendar.DAY_OF_YEAR, -6);
            for (int i = 0; i < 7; i++) {
                String key = TimeUtil.dayKey(day.getTime());
                DayStat stat = new DayStat();
                stat.label = TimeUtil.shortDay(day.getTime());
                stat.calories = (float) sumCalories(userId, key, key);
                items.add(stat);
                day.add(Calendar.DAY_OF_YEAR, 1);
            }
            return items;
        }
        day.add(Calendar.DAY_OF_YEAR, -27);
        for (int week = 1; week <= 4; week++) {
            String start = TimeUtil.dayKey(day.getTime());
            Calendar end = (Calendar) day.clone();
            end.add(Calendar.DAY_OF_YEAR, 6);
            DayStat stat = new DayStat();
            stat.label = "T" + week;
            stat.calories = (float) sumCalories(userId, start, TimeUtil.dayKey(end.getTime()));
            items.add(stat);
            day.add(Calendar.DAY_OF_YEAR, 7);
        }
        return items;
    }

    public List<WorkoutSession> unsynced(long userId) {
        List<WorkoutSession> items = new ArrayList<>();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM workout_sessions WHERE user_id = ? AND status = 'finished' AND is_synced = 0 ORDER BY id",
                new String[]{String.valueOf(userId)});
        try {
            while (cursor.moveToNext()) {
                items.add(mapSession(cursor));
            }
        } finally {
            cursor.close();
        }
        return items;
    }

    public void markSessionsSynced(List<Long> ids) {
        for (Long id : ids) {
            ContentValues values = new ContentValues();
            values.put("is_synced", 1);
            db.update("workout_sessions", values, "id = ?", new String[]{String.valueOf(id)});
        }
    }

    public boolean markGoal(long userId, String type, double value) {
        ContentValues values = new ContentValues();
        values.put("user_id", userId);
        values.put("goal_type", type);
        values.put("period_key", TimeUtil.todayKey());
        values.put("achieved_value", value);
        values.put("notified", 1);
        return db.insert("goal_logs", null, values) > 0;
    }

    private double sumCalories(long userId, String fromDay, String toDay) {
        Cursor cursor = db.rawQuery(
                "SELECT COALESCE(SUM(calories),0) FROM workout_sessions WHERE user_id = ? AND status = 'finished' "
                        + "AND substr(start_time,1,10) >= ? AND substr(start_time,1,10) <= ?",
                new String[]{String.valueOf(userId), fromDay, toDay});
        try {
            cursor.moveToFirst();
            return cursor.getDouble(0);
        } finally {
            cursor.close();
        }
    }

    private void insertExercise(PlanExercise item) {
        ContentValues values = new ContentValues();
        values.put("plan_id", item.planId);
        values.put("exercise_id", item.exerciseId);
        if (item.weekday > 0) {
            values.put("weekday", item.weekday);
        } else {
            values.putNull("weekday");
        }
        values.put("sets", item.sets);
        values.put("reps", item.reps);
        values.put("duration_sec", item.durationSec);
        values.put("rest_sec", item.restSec <= 0 ? 60 : item.restSec);
        values.put("sort_order", item.sortOrder);
        db.insert("plan_exercises", null, values);
    }

    private ContentValues scheduleValues(WorkoutSchedule schedule) {
        ContentValues values = new ContentValues();
        values.put("user_id", schedule.userId);
        if (schedule.planId > 0) {
            values.put("plan_id", schedule.planId);
        } else {
            values.putNull("plan_id");
        }
        values.put("title", schedule.title);
        values.put("schedule_time", schedule.scheduleTime);
        values.put("repeat_type", schedule.repeatType);
        if ("weekly".equals(schedule.repeatType) && schedule.weekday > 0) {
            values.put("weekday", schedule.weekday);
        } else {
            values.putNull("weekday");
        }
        values.put("alarm_code", schedule.alarmCode);
        values.put("is_enabled", schedule.enabled ? 1 : 0);
        return values;
    }

    private List<WorkoutPlan> plans(String sql, String[] args) {
        List<WorkoutPlan> items = new ArrayList<>();
        Cursor cursor = db.rawQuery(sql, args);
        try {
            while (cursor.moveToNext()) {
                WorkoutPlan plan = new WorkoutPlan();
                plan.id = Db.lng(cursor, "id");
                plan.userId = Db.lng(cursor, "user_id");
                plan.name = Db.str(cursor, "name");
                plan.description = Db.str(cursor, "description");
                plan.goal = Db.str(cursor, "goal");
                plan.bmiMin = Db.dbl(cursor, "bmi_min");
                plan.bmiMax = Db.dbl(cursor, "bmi_max");
                plan.level = Db.str(cursor, "level");
                plan.template = Db.flag(cursor, "is_template");
                plan.hidden = Db.flag(cursor, "is_hidden");
                items.add(plan);
            }
        } finally {
            cursor.close();
        }
        return items;
    }

    private List<WorkoutSchedule> schedules(String sql, String[] args) {
        List<WorkoutSchedule> items = new ArrayList<>();
        Cursor cursor = db.rawQuery(sql, args);
        try {
            while (cursor.moveToNext()) {
                WorkoutSchedule schedule = new WorkoutSchedule();
                schedule.id = Db.lng(cursor, "id");
                schedule.userId = Db.lng(cursor, "user_id");
                schedule.planId = Db.lng(cursor, "plan_id");
                schedule.title = Db.str(cursor, "title");
                schedule.scheduleTime = Db.str(cursor, "schedule_time");
                schedule.repeatType = Db.str(cursor, "repeat_type");
                schedule.weekday = Db.num(cursor, "weekday");
                schedule.alarmCode = Db.num(cursor, "alarm_code");
                schedule.enabled = Db.flag(cursor, "is_enabled");
                items.add(schedule);
            }
        } finally {
            cursor.close();
        }
        return items;
    }

    private WorkoutSession mapSession(Cursor cursor) {
        WorkoutSession session = new WorkoutSession();
        session.id = Db.lng(cursor, "id");
        session.userId = Db.lng(cursor, "user_id");
        session.planId = Db.lng(cursor, "plan_id");
        session.title = Db.str(cursor, "title");
        session.startTime = Db.str(cursor, "start_time");
        session.endTime = Db.str(cursor, "end_time");
        session.durationSec = Db.num(cursor, "duration_sec");
        session.steps = Db.num(cursor, "steps");
        session.distanceM = Db.dbl(cursor, "distance_m");
        session.calories = Db.dbl(cursor, "calories");
        session.status = Db.str(cursor, "status");
        session.synced = Db.flag(cursor, "is_synced");
        return session;
    }
}
