package com.btlltdd.fitnesstracker.database;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Vòng đời SQLiteOpenHelper:
 * - onConfigure: bật khóa ngoại (SQLite tắt mặc định).
 * - onCreate: chạy một lần khi file fitness_tracker.db chưa tồn tại.
 * - onUpgrade: khi tăng DB_VERSION. Đồ án tạo lại schema để demo sạch.
 *
 * Một process dùng một helper. Truy vấn cục bộ nằm ở các lớp Dao.
 * Gọi mạng không đặt ở đây; đồng bộ chạy trên Executor riêng.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "fitness_tracker.db";
    public static final int DB_VERSION = 1;
    private static final String SCHEMA_ASSET = "schema.sql";

    private static final String[] DROP_VIEWS = {"v_user_bmi"};

    private static final String[] DROP_TABLES = {
            "goal_logs",
            "route_points",
            "session_logs",
            "workout_sessions",
            "schedules",
            "plan_exercises",
            "bookmarks",
            "workout_plans",
            "exercises",
            "users"
    };

    private static DatabaseHelper instance;

    private final Context appContext;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context);
        }
        return instance;
    }

    public DatabaseHelper(Context context) {
        super(context.getApplicationContext(), DB_NAME, null, DB_VERSION);
        this.appContext = context.getApplicationContext();
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        execScript(db, readAsset(SCHEMA_ASSET));
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.setForeignKeyConstraintsEnabled(false);
        for (String view : DROP_VIEWS) {
            db.execSQL("DROP VIEW IF EXISTS " + view);
        }
        for (String table : DROP_TABLES) {
            db.execSQL("DROP TABLE IF EXISTS " + table);
        }
        try {
            db.execSQL("DELETE FROM sqlite_sequence");
        } catch (SQLException ignored) {
            // sqlite_sequence chỉ xuất hiện sau khi có bảng AUTOINCREMENT.
        }
        onCreate(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    private void execScript(SQLiteDatabase db, String script) {
        StringBuilder statement = new StringBuilder();
        for (String rawLine : script.split("\n")) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("--")) {
                continue;
            }
            statement.append(line).append('\n');
            if (line.endsWith(";")) {
                String sql = statement.toString().trim();
                if (sql.endsWith(";")) {
                    sql = sql.substring(0, sql.length() - 1);
                }
                db.execSQL(sql);
                statement.setLength(0);
            }
        }
        if (statement.length() > 0) {
            throw new IllegalStateException("schema.sql còn câu lệnh chưa kết thúc bằng dấu chấm phẩy");
        }
    }

    private String readAsset(String name) {
        try (InputStream in = appContext.getAssets().open(name);
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
            return builder.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Không đọc được " + name, e);
        }
    }
}
