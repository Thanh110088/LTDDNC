package com.btlltdd.fitnesstracker.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.btlltdd.fitnesstracker.model.User;
import com.btlltdd.fitnesstracker.util.TimeUtil;

/**
 * R01-R06. Mật khẩu so sánh plain text để dễ đối chiếu trong DB khi demo.
 * Mọi câu hỏi hồ sơ đều lọc theo id của người đang đăng nhập (R04).
 */
public class UserDao {
    private final SQLiteDatabase db;

    public UserDao(Context context) {
        db = DatabaseHelper.getInstance(context).getWritableDatabase();
    }

    public String register(String username, String password, String fullName) {
        String name = username == null ? "" : username.trim();
        String pass = password == null ? "" : password;
        String person = fullName == null ? "" : fullName.trim();
        if (name.length() < 3 || pass.length() < 4 || person.isEmpty()) {
            return "Nhập họ tên, tài khoản từ 3 ký tự và mật khẩu từ 4 ký tự";
        }
        Cursor cursor = db.rawQuery(
                "SELECT id FROM users WHERE username = ? COLLATE NOCASE",
                new String[]{name});
        try {
            if (cursor.moveToFirst()) {
                return "Tên đăng nhập đã tồn tại";
            }
        } finally {
            cursor.close();
        }
        ContentValues values = new ContentValues();
        values.put("username", name);
        values.put("password", pass);
        values.put("full_name", person);
        values.put("role", "user");
        values.put("is_active", 1);
        values.put("is_synced", 0);
        long id = db.insert("users", null, values);
        return id > 0 ? null : "Không tạo được tài khoản";
    }

    public Result login(String username, String password) {
        Result result = new Result();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM users WHERE username = ? COLLATE NOCASE",
                new String[]{username == null ? "" : username.trim()});
        try {
            if (!cursor.moveToFirst()) {
                result.error = "Sai tài khoản hoặc mật khẩu";
                return result;
            }
            if (!cursor.getString(cursor.getColumnIndexOrThrow("password")).equals(password)) {
                result.error = "Sai tài khoản hoặc mật khẩu";
                return result;
            }
            User user = map(cursor);
            if (!user.active) {
                result.error = "Tài khoản đã bị khóa";
                return result;
            }
            result.user = user;
            return result;
        } finally {
            cursor.close();
        }
    }

    public String resetPassword(String username, String newPassword) {
        if (username == null || username.trim().isEmpty() || newPassword == null || newPassword.length() < 4) {
            return "Nhập tài khoản và mật khẩu mới từ 4 ký tự";
        }
        ContentValues values = new ContentValues();
        values.put("password", newPassword);
        values.put("is_synced", 0);
        values.put("updated_at", TimeUtil.now());
        int rows = db.update("users", values, "username = ? COLLATE NOCASE", new String[]{username.trim()});
        return rows > 0 ? null : "Không tìm thấy tài khoản";
    }

    public User get(long userId) {
        Cursor cursor = db.rawQuery("SELECT * FROM users WHERE id = ?", new String[]{String.valueOf(userId)});
        try {
            if (!cursor.moveToFirst()) {
                return null;
            }
            return map(cursor);
        } finally {
            cursor.close();
        }
    }

    public void updateProfile(User user) {
        ContentValues values = new ContentValues();
        values.put("height_cm", user.heightCm);
        values.put("weight_kg", user.weightKg);
        values.put("goal", user.goal);
        values.put("target_steps", user.targetSteps);
        values.put("target_calories", user.targetCalories);
        values.put("target_distance_km", user.targetDistanceKm);
        values.put("is_synced", 0);
        values.put("updated_at", TimeUtil.now());
        db.update("users", values, "id = ?", new String[]{String.valueOf(user.id)});
    }

    public void markSynced(long userId) {
        ContentValues values = new ContentValues();
        values.put("is_synced", 1);
        db.update("users", values, "id = ?", new String[]{String.valueOf(userId)});
    }

    public java.util.List<User> listAll() {
        java.util.List<User> users = new java.util.ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT * FROM users ORDER BY role DESC, username COLLATE NOCASE", null);
        try {
            while (cursor.moveToNext()) {
                users.add(map(cursor));
            }
        } finally {
            cursor.close();
        }
        return users;
    }

    public void setActive(long userId, boolean active) {
        ContentValues values = new ContentValues();
        values.put("is_active", active ? 1 : 0);
        values.put("updated_at", TimeUtil.now());
        db.update("users", values, "id = ?", new String[]{String.valueOf(userId)});
    }

    private User map(Cursor cursor) {
        User user = new User();
        user.id = Db.lng(cursor, "id");
        user.username = Db.str(cursor, "username");
        user.fullName = Db.str(cursor, "full_name");
        user.role = Db.str(cursor, "role");
        user.heightCm = Db.dbl(cursor, "height_cm");
        user.weightKg = Db.dbl(cursor, "weight_kg");
        user.goal = Db.str(cursor, "goal");
        user.targetSteps = Db.num(cursor, "target_steps");
        user.targetCalories = Db.dbl(cursor, "target_calories");
        user.targetDistanceKm = Db.dbl(cursor, "target_distance_km");
        user.active = Db.flag(cursor, "is_active");
        user.synced = Db.flag(cursor, "is_synced");
        return user;
    }

    public static class Result {
        public User user;
        public String error;
    }
}
