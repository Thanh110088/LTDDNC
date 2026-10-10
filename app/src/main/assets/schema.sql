-- Fitness Tracker & Workout Planner
-- SQLite schema, database version 1
-- DatabaseHelper doc file nay trong onCreate().
-- Moi cau lenh ket thuc bang dau ; o cuoi dong. Khong dat dau ; trong comment hoac du lieu.
--
-- Anh xa yeu cau:
-- R01-R04, R06     users (username, password, role, is_active)
-- R05, R23        users.height_cm, weight_kg, goal, target_* ; BMI tinh bang view v_user_bmi
-- R07, R08, R13   exercises (nhom co, muc do, dung cu, anh, video, is_hidden)
-- R14             bookmarks
-- R09, R10, R13   workout_plans + plan_exercises (lo trinh ca nhan va mau theo BMI/muc tieu)
-- R11, R12        schedules (gio tap + alarm_code cho AlarmManager)
-- R15, R19, R22   workout_sessions (thoi gian, calo, trang thai)
-- R16             workout_sessions.steps
-- R17             route_points + workout_sessions.distance_m
-- R18             session_logs (sets, reps thuc te)
-- R20, R21        cot is_synced = 0 cho ban ghi cho dong bo
-- R04             moi bang nghiep vu co user_id; truy van luon loc theo user dang nhap

-- Mat khau luu plain text de doi chieu DB khi demo. Do an khong yeu cau ma hoa.

CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE COLLATE NOCASE,
    password TEXT NOT NULL,
    full_name TEXT NOT NULL,
    role TEXT NOT NULL DEFAULT 'user' CHECK (role IN ('admin', 'user')),
    height_cm REAL NOT NULL DEFAULT 0,
    weight_kg REAL NOT NULL DEFAULT 0,
    goal TEXT NOT NULL DEFAULT 'maintain' CHECK (goal IN ('lose_weight', 'gain_muscle', 'maintain', 'endurance')),
    target_steps INTEGER NOT NULL DEFAULT 8000,
    target_calories REAL NOT NULL DEFAULT 300,
    target_distance_km REAL NOT NULL DEFAULT 3,
    is_active INTEGER NOT NULL DEFAULT 1 CHECK (is_active IN (0, 1)),
    is_synced INTEGER NOT NULL DEFAULT 0 CHECK (is_synced IN (0, 1)),
    created_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    updated_at TEXT
);

-- muscle_group: chest ngu, back lung, legs chan, shoulders vai, arms tay, core bung, cardio, full_body
-- difficulty: beginner, intermediate, advanced
-- equipment: none, dumbbell, barbell, machine, band, mat
-- met: he so MET cho cong thuc calo R19 = MET * can_nang_kg * thoi_gian_gio

CREATE TABLE exercises (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    description TEXT,
    muscle_group TEXT NOT NULL CHECK (muscle_group IN ('chest', 'back', 'legs', 'shoulders', 'arms', 'core', 'cardio', 'full_body')),
    difficulty TEXT NOT NULL DEFAULT 'beginner' CHECK (difficulty IN ('beginner', 'intermediate', 'advanced')),
    equipment TEXT NOT NULL DEFAULT 'none' CHECK (equipment IN ('none', 'dumbbell', 'barbell', 'machine', 'band', 'mat')),
    image_res TEXT,
    image_url TEXT,
    video_url TEXT,
    met REAL NOT NULL DEFAULT 5.0,
    is_hidden INTEGER NOT NULL DEFAULT 0 CHECK (is_hidden IN (0, 1)),
    is_synced INTEGER NOT NULL DEFAULT 0 CHECK (is_synced IN (0, 1)),
    created_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))
);

CREATE TABLE bookmarks (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    exercise_id INTEGER NOT NULL,
    created_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    UNIQUE (user_id, exercise_id),
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    FOREIGN KEY (exercise_id) REFERENCES exercises (id) ON DELETE CASCADE
);

-- user_id NULL + is_template = 1: lo trinh mau cua admin (R10, R13).
-- user_id co gia tri + is_template = 0: lo trinh ca nhan (R09).
-- Goi y: goal trung khop va bmi_min <= BMI <= bmi_max va is_hidden = 0.

CREATE TABLE workout_plans (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER,
    name TEXT NOT NULL,
    description TEXT,
    goal TEXT NOT NULL DEFAULT 'maintain' CHECK (goal IN ('lose_weight', 'gain_muscle', 'maintain', 'endurance')),
    bmi_min REAL NOT NULL DEFAULT 0,
    bmi_max REAL NOT NULL DEFAULT 99,
    level TEXT NOT NULL DEFAULT 'beginner' CHECK (level IN ('beginner', 'intermediate', 'advanced')),
    is_template INTEGER NOT NULL DEFAULT 0 CHECK (is_template IN (0, 1)),
    is_hidden INTEGER NOT NULL DEFAULT 0 CHECK (is_hidden IN (0, 1)),
    is_synced INTEGER NOT NULL DEFAULT 0 CHECK (is_synced IN (0, 1)),
    created_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE plan_exercises (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    plan_id INTEGER NOT NULL,
    exercise_id INTEGER NOT NULL,
    weekday INTEGER CHECK (weekday IS NULL OR (weekday BETWEEN 1 AND 7)),
    sets INTEGER NOT NULL DEFAULT 3,
    reps INTEGER NOT NULL DEFAULT 10,
    duration_sec INTEGER NOT NULL DEFAULT 0,
    rest_sec INTEGER NOT NULL DEFAULT 60,
    sort_order INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (plan_id) REFERENCES workout_plans (id) ON DELETE CASCADE,
    FOREIGN KEY (exercise_id) REFERENCES exercises (id) ON DELETE CASCADE
);

-- schedule_time dang 'yyyy-MM-dd HH:mm'. alarm_code la requestCode cua PendingIntent.

CREATE TABLE schedules (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    plan_id INTEGER,
    title TEXT NOT NULL,
    schedule_time TEXT NOT NULL,
    repeat_type TEXT NOT NULL DEFAULT 'once' CHECK (repeat_type IN ('once', 'daily', 'weekly')),
    weekday INTEGER CHECK (weekday IS NULL OR (weekday BETWEEN 1 AND 7)),
    alarm_code INTEGER NOT NULL UNIQUE,
    is_enabled INTEGER NOT NULL DEFAULT 1 CHECK (is_enabled IN (0, 1)),
    created_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    CHECK (repeat_type != 'weekly' OR weekday IS NOT NULL),
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    FOREIGN KEY (plan_id) REFERENCES workout_plans (id) ON DELETE SET NULL
);

-- status: running dang tap, paused tam dung, finished da luu nhat ky.
-- distance_m don vi met. calories da tinh luc ket thuc buoi tap.

CREATE TABLE workout_sessions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    plan_id INTEGER,
    title TEXT,
    start_time TEXT NOT NULL,
    end_time TEXT,
    duration_sec INTEGER NOT NULL DEFAULT 0,
    steps INTEGER NOT NULL DEFAULT 0,
    distance_m REAL NOT NULL DEFAULT 0,
    calories REAL NOT NULL DEFAULT 0,
    status TEXT NOT NULL DEFAULT 'running' CHECK (status IN ('running', 'paused', 'finished')),
    is_synced INTEGER NOT NULL DEFAULT 0 CHECK (is_synced IN (0, 1)),
    created_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    FOREIGN KEY (plan_id) REFERENCES workout_plans (id) ON DELETE SET NULL
);

CREATE TABLE session_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    session_id INTEGER NOT NULL,
    exercise_id INTEGER NOT NULL,
    sets_done INTEGER NOT NULL DEFAULT 0,
    reps_done INTEGER NOT NULL DEFAULT 0,
    duration_sec INTEGER NOT NULL DEFAULT 0,
    note TEXT,
    FOREIGN KEY (session_id) REFERENCES workout_sessions (id) ON DELETE CASCADE,
    FOREIGN KEY (exercise_id) REFERENCES exercises (id) ON DELETE CASCADE
);

CREATE TABLE route_points (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    session_id INTEGER NOT NULL,
    latitude REAL NOT NULL,
    longitude REAL NOT NULL,
    accuracy REAL,
    recorded_at TEXT NOT NULL,
    FOREIGN KEY (session_id) REFERENCES workout_sessions (id) ON DELETE CASCADE
);

-- period_key: 'yyyy-MM-dd' hoac 'yyyy-MM'. notified tranh ban thong bao chuc mung lap lai.

CREATE TABLE goal_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    goal_type TEXT NOT NULL CHECK (goal_type IN ('steps', 'calories', 'distance')),
    period_key TEXT NOT NULL,
    achieved_value REAL NOT NULL,
    notified INTEGER NOT NULL DEFAULT 0 CHECK (notified IN (0, 1)),
    achieved_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    UNIQUE (user_id, goal_type, period_key),
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_users_sync ON users (is_synced);
CREATE INDEX idx_exercises_filter ON exercises (muscle_group, difficulty, equipment, is_hidden);
CREATE INDEX idx_bookmarks_user ON bookmarks (user_id);
CREATE INDEX idx_plans_owner ON workout_plans (user_id, is_template, is_hidden);
CREATE INDEX idx_plan_exercises_plan ON plan_exercises (plan_id);
CREATE INDEX idx_schedules_user ON schedules (user_id);
CREATE INDEX idx_sessions_user_time ON workout_sessions (user_id, start_time);
CREATE INDEX idx_sessions_sync ON workout_sessions (is_synced);
CREATE INDEX idx_session_logs_session ON session_logs (session_id);
CREATE INDEX idx_route_session ON route_points (session_id);
CREATE INDEX idx_goal_user ON goal_logs (user_id, period_key);

-- BMI khong luu cot that. Doi chieu cao/can nang la BMI tu tinh lai.

CREATE VIEW v_user_bmi AS
SELECT
    id,
    username,
    full_name,
    role,
    height_cm,
    weight_kg,
    goal,
    CASE
        WHEN height_cm <= 0 OR weight_kg <= 0 THEN NULL
        ELSE ROUND(weight_kg / ((height_cm / 100.0) * (height_cm / 100.0)), 1)
    END AS bmi
FROM users;

INSERT INTO users (id, username, password, full_name, role, height_cm, weight_kg, goal, target_steps, target_calories, target_distance_km, is_active, is_synced) VALUES (1, 'admin', 'admin123', 'Quản trị viên', 'admin', 0, 0, 'maintain', 8000, 300, 3, 1, 1);
INSERT INTO users (id, username, password, full_name, role, height_cm, weight_kg, goal, target_steps, target_calories, target_distance_km, is_active, is_synced) VALUES (2, 'user', '123456', 'Nguyễn Văn A', 'user', 170, 78, 'lose_weight', 8000, 400, 3, 1, 1);

INSERT INTO exercises (id, name, description, muscle_group, difficulty, equipment, image_res, video_url, met, is_hidden, is_synced) VALUES (1, 'Hít đất', 'Chống đẩy, giữ thân thẳng và siết bụng.', 'chest', 'beginner', 'none', 'ex_pushup', 'https://www.youtube.com/results?search_query=bai+tap+hit+dat', 8.0, 0, 1);
INSERT INTO exercises (id, name, description, muscle_group, difficulty, equipment, image_res, video_url, met, is_hidden, is_synced) VALUES (2, 'Squat', 'Ngồi xuống đến khi đùi song song mặt đất.', 'legs', 'beginner', 'none', 'ex_squat', 'https://www.youtube.com/results?search_query=bai+tap+squat', 5.0, 0, 1);
INSERT INTO exercises (id, name, description, muscle_group, difficulty, equipment, image_res, video_url, met, is_hidden, is_synced) VALUES (3, 'Plank', 'Giữ người thẳng trên cẳng tay.', 'core', 'beginner', 'mat', 'ex_plank', 'https://www.youtube.com/results?search_query=bai+tap+plank', 3.8, 0, 1);
INSERT INTO exercises (id, name, description, muscle_group, difficulty, equipment, image_res, video_url, met, is_hidden, is_synced) VALUES (4, 'Chạy tại chỗ', 'Nâng gối đều, dùng cho bài cardio.', 'cardio', 'beginner', 'none', 'ex_run', 'https://www.youtube.com/results?search_query=chay+tai+cho', 8.0, 0, 1);
INSERT INTO exercises (id, name, description, muscle_group, difficulty, equipment, image_res, video_url, met, is_hidden, is_synced) VALUES (5, 'Lunge', 'Chùng chân từng bên, gối sau hạ thấp.', 'legs', 'beginner', 'none', 'ex_lunge', 'https://www.youtube.com/results?search_query=bai+tap+lunge', 6.0, 0, 1);
INSERT INTO exercises (id, name, description, muscle_group, difficulty, equipment, image_res, video_url, met, is_hidden, is_synced) VALUES (6, 'Gập bụng', 'Nâng vai khỏi thảm, không kéo cổ.', 'core', 'beginner', 'mat', 'ex_crunch', 'https://www.youtube.com/results?search_query=gap+bung', 3.8, 0, 1);
INSERT INTO exercises (id, name, description, muscle_group, difficulty, equipment, image_res, video_url, met, is_hidden, is_synced) VALUES (7, 'Đẩy vai tạ đơn', 'Đẩy tạ từ vai lên thẳng cánh tay.', 'shoulders', 'intermediate', 'dumbbell', 'ex_press', 'https://www.youtube.com/results?search_query=dumbbell+shoulder+press', 5.0, 0, 1);
INSERT INTO exercises (id, name, description, muscle_group, difficulty, equipment, image_res, video_url, met, is_hidden, is_synced) VALUES (8, 'Deadlift', 'Kéo tạ đòn từ sàn, lưng giữ trung tính.', 'back', 'intermediate', 'barbell', 'ex_deadlift', 'https://www.youtube.com/results?search_query=deadlift+ky+thuat', 6.0, 0, 1);

INSERT INTO workout_plans (id, user_id, name, description, goal, bmi_min, bmi_max, level, is_template, is_hidden, is_synced) VALUES (1, NULL, 'Giảm cân 7 ngày', 'Cardio và thân trọng cho BMI từ 25 trở lên.', 'lose_weight', 25, 99, 'beginner', 1, 0, 1);
INSERT INTO workout_plans (id, user_id, name, description, goal, bmi_min, bmi_max, level, is_template, is_hidden, is_synced) VALUES (2, NULL, 'Tăng cơ tại nhà', 'Bài thân trọng cho BMI dưới 25.', 'gain_muscle', 0, 24.9, 'beginner', 1, 0, 1);
INSERT INTO workout_plans (id, user_id, name, description, goal, bmi_min, bmi_max, level, is_template, is_hidden, is_synced) VALUES (3, NULL, 'Duy trì sức khỏe', 'Lịch nhẹ cho BMI trong vùng bình thường.', 'maintain', 18.5, 24.9, 'beginner', 1, 0, 1);

INSERT INTO plan_exercises (plan_id, exercise_id, weekday, sets, reps, duration_sec, rest_sec, sort_order) VALUES (1, 4, 1, 1, 0, 1200, 60, 1);
INSERT INTO plan_exercises (plan_id, exercise_id, weekday, sets, reps, duration_sec, rest_sec, sort_order) VALUES (1, 2, 2, 3, 15, 0, 60, 2);
INSERT INTO plan_exercises (plan_id, exercise_id, weekday, sets, reps, duration_sec, rest_sec, sort_order) VALUES (1, 1, 3, 3, 12, 0, 60, 3);
INSERT INTO plan_exercises (plan_id, exercise_id, weekday, sets, reps, duration_sec, rest_sec, sort_order) VALUES (2, 1, 1, 4, 10, 0, 90, 1);
INSERT INTO plan_exercises (plan_id, exercise_id, weekday, sets, reps, duration_sec, rest_sec, sort_order) VALUES (2, 2, 1, 4, 12, 0, 90, 2);
INSERT INTO plan_exercises (plan_id, exercise_id, weekday, sets, reps, duration_sec, rest_sec, sort_order) VALUES (2, 7, 3, 3, 10, 0, 90, 3);
INSERT INTO plan_exercises (plan_id, exercise_id, weekday, sets, reps, duration_sec, rest_sec, sort_order) VALUES (3, 3, 1, 3, 1, 40, 45, 1);
INSERT INTO plan_exercises (plan_id, exercise_id, weekday, sets, reps, duration_sec, rest_sec, sort_order) VALUES (3, 6, 1, 3, 15, 0, 45, 2);
INSERT INTO plan_exercises (plan_id, exercise_id, weekday, sets, reps, duration_sec, rest_sec, sort_order) VALUES (3, 5, 3, 3, 10, 0, 45, 3);
