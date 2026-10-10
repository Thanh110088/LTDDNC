package com.btlltdd.fitnesstracker.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.btlltdd.fitnesstracker.model.Exercise;

import java.util.ArrayList;
import java.util.List;

/** R07, R08, R13, R14. */
public class ExerciseDao {
    private final SQLiteDatabase db;

    public ExerciseDao(Context context) {
        db = DatabaseHelper.getInstance(context).getWritableDatabase();
    }

    public List<Exercise> search(long userId, String keyword, String muscle, String difficulty,
                                 String equipment, boolean includeHidden) {
        StringBuilder sql = new StringBuilder();
        List<String> args = new ArrayList<>();
        sql.append("SELECT e.*, CASE WHEN b.id IS NULL THEN 0 ELSE 1 END AS bookmarked ");
        sql.append("FROM exercises e ");
        sql.append("LEFT JOIN bookmarks b ON b.exercise_id = e.id AND b.user_id = ? ");
        args.add(String.valueOf(userId));
        sql.append("WHERE 1 = 1 ");
        if (!includeHidden) {
            sql.append("AND e.is_hidden = 0 ");
        }
        if (muscle != null && !muscle.isEmpty()) {
            sql.append("AND e.muscle_group = ? ");
            args.add(muscle);
        }
        if (difficulty != null && !difficulty.isEmpty()) {
            sql.append("AND e.difficulty = ? ");
            args.add(difficulty);
        }
        if (equipment != null && !equipment.isEmpty()) {
            sql.append("AND e.equipment = ? ");
            args.add(equipment);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append("AND (e.name LIKE ? OR IFNULL(e.description,'') LIKE ?) ");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
        }
        sql.append("ORDER BY e.name COLLATE NOCASE");
        List<Exercise> items = new ArrayList<>();
        Cursor cursor = db.rawQuery(sql.toString(), args.toArray(new String[0]));
        try {
            while (cursor.moveToNext()) {
                items.add(map(cursor));
            }
        } finally {
            cursor.close();
        }
        return items;
    }

    public boolean toggleBookmark(long userId, long exerciseId) {
        Cursor cursor = db.rawQuery(
                "SELECT id FROM bookmarks WHERE user_id = ? AND exercise_id = ?",
                new String[]{String.valueOf(userId), String.valueOf(exerciseId)});
        try {
            if (cursor.moveToFirst()) {
                db.delete("bookmarks", "user_id = ? AND exercise_id = ?",
                        new String[]{String.valueOf(userId), String.valueOf(exerciseId)});
                return false;
            }
        } finally {
            cursor.close();
        }
        ContentValues values = new ContentValues();
        values.put("user_id", userId);
        values.put("exercise_id", exerciseId);
        db.insert("bookmarks", null, values);
        return true;
    }

    public long save(Exercise exercise) {
        ContentValues values = new ContentValues();
        values.put("name", exercise.name);
        values.put("description", exercise.description);
        values.put("muscle_group", exercise.muscleGroup);
        values.put("difficulty", exercise.difficulty);
        values.put("equipment", exercise.equipment);
        values.put("video_url", exercise.videoUrl);
        values.put("met", exercise.met <= 0 ? 5 : exercise.met);
        values.put("is_synced", 0);
        if (exercise.id > 0) {
            db.update("exercises", values, "id = ?", new String[]{String.valueOf(exercise.id)});
            return exercise.id;
        }
        values.put("is_hidden", 0);
        return db.insert("exercises", null, values);
    }

    public void setHidden(long id, boolean hidden) {
        ContentValues values = new ContentValues();
        values.put("is_hidden", hidden ? 1 : 0);
        db.update("exercises", values, "id = ?", new String[]{String.valueOf(id)});
    }

    public void delete(long id) {
        db.delete("exercises", "id = ?", new String[]{String.valueOf(id)});
    }

    private Exercise map(Cursor cursor) {
        Exercise exercise = new Exercise();
        exercise.id = Db.lng(cursor, "id");
        exercise.name = Db.str(cursor, "name");
        exercise.description = Db.str(cursor, "description");
        exercise.muscleGroup = Db.str(cursor, "muscle_group");
        exercise.difficulty = Db.str(cursor, "difficulty");
        exercise.equipment = Db.str(cursor, "equipment");
        exercise.imageRes = Db.str(cursor, "image_res");
        exercise.videoUrl = Db.str(cursor, "video_url");
        exercise.met = Db.dbl(cursor, "met");
        exercise.hidden = Db.flag(cursor, "is_hidden");
        exercise.bookmarked = Db.flag(cursor, "bookmarked");
        return exercise;
    }
}
