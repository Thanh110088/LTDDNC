package com.btlltdd.fitnesstracker.database;

/**
 * Tên bảng và cột trùng với assets/schema.sql.
 * Fragment và DAO dùng hằng số này, không gõ chuỗi rải rác.
 */
public final class DatabaseContract {

    private DatabaseContract() {
    }

    public static final class User {
        public static final String TABLE = "users";
        public static final String ID = "id";
        public static final String USERNAME = "username";
        public static final String PASSWORD = "password";
        public static final String FULL_NAME = "full_name";
        public static final String ROLE = "role";
        public static final String HEIGHT_CM = "height_cm";
        public static final String WEIGHT_KG = "weight_kg";
        public static final String GOAL = "goal";
        public static final String TARGET_STEPS = "target_steps";
        public static final String TARGET_CALORIES = "target_calories";
        public static final String TARGET_DISTANCE_KM = "target_distance_km";
        public static final String IS_ACTIVE = "is_active";
        public static final String IS_SYNCED = "is_synced";
        public static final String CREATED_AT = "created_at";
        public static final String UPDATED_AT = "updated_at";

        public static final String ROLE_ADMIN = "admin";
        public static final String ROLE_USER = "user";
        public static final String GOAL_LOSE = "lose_weight";
        public static final String GOAL_GAIN = "gain_muscle";
        public static final String GOAL_MAINTAIN = "maintain";
        public static final String GOAL_ENDURANCE = "endurance";

        private User() {
        }
    }

    public static final class Exercise {
        public static final String TABLE = "exercises";
        public static final String ID = "id";
        public static final String NAME = "name";
        public static final String DESCRIPTION = "description";
        public static final String MUSCLE_GROUP = "muscle_group";
        public static final String DIFFICULTY = "difficulty";
        public static final String EQUIPMENT = "equipment";
        public static final String IMAGE_RES = "image_res";
        public static final String IMAGE_URL = "image_url";
        public static final String VIDEO_URL = "video_url";
        public static final String MET = "met";
        public static final String IS_HIDDEN = "is_hidden";
        public static final String IS_SYNCED = "is_synced";
        public static final String CREATED_AT = "created_at";

        private Exercise() {
        }
    }

    public static final class Bookmark {
        public static final String TABLE = "bookmarks";
        public static final String ID = "id";
        public static final String USER_ID = "user_id";
        public static final String EXERCISE_ID = "exercise_id";
        public static final String CREATED_AT = "created_at";

        private Bookmark() {
        }
    }

    public static final class WorkoutPlan {
        public static final String TABLE = "workout_plans";
        public static final String ID = "id";
        public static final String USER_ID = "user_id";
        public static final String NAME = "name";
        public static final String DESCRIPTION = "description";
        public static final String GOAL = "goal";
        public static final String BMI_MIN = "bmi_min";
        public static final String BMI_MAX = "bmi_max";
        public static final String LEVEL = "level";
        public static final String IS_TEMPLATE = "is_template";
        public static final String IS_HIDDEN = "is_hidden";
        public static final String IS_SYNCED = "is_synced";
        public static final String CREATED_AT = "created_at";

        private WorkoutPlan() {
        }
    }

    public static final class PlanExercise {
        public static final String TABLE = "plan_exercises";
        public static final String ID = "id";
        public static final String PLAN_ID = "plan_id";
        public static final String EXERCISE_ID = "exercise_id";
        public static final String WEEKDAY = "weekday";
        public static final String SETS = "sets";
        public static final String REPS = "reps";
        public static final String DURATION_SEC = "duration_sec";
        public static final String REST_SEC = "rest_sec";
        public static final String SORT_ORDER = "sort_order";

        private PlanExercise() {
        }
    }

    public static final class Schedule {
        public static final String TABLE = "schedules";
        public static final String ID = "id";
        public static final String USER_ID = "user_id";
        public static final String PLAN_ID = "plan_id";
        public static final String TITLE = "title";
        public static final String SCHEDULE_TIME = "schedule_time";
        public static final String REPEAT_TYPE = "repeat_type";
        public static final String WEEKDAY = "weekday";
        public static final String ALARM_CODE = "alarm_code";
        public static final String IS_ENABLED = "is_enabled";
        public static final String CREATED_AT = "created_at";

        private Schedule() {
        }
    }

    public static final class WorkoutSession {
        public static final String TABLE = "workout_sessions";
        public static final String ID = "id";
        public static final String USER_ID = "user_id";
        public static final String PLAN_ID = "plan_id";
        public static final String TITLE = "title";
        public static final String START_TIME = "start_time";
        public static final String END_TIME = "end_time";
        public static final String DURATION_SEC = "duration_sec";
        public static final String STEPS = "steps";
        public static final String DISTANCE_M = "distance_m";
        public static final String CALORIES = "calories";
        public static final String STATUS = "status";
        public static final String IS_SYNCED = "is_synced";
        public static final String CREATED_AT = "created_at";

        public static final String STATUS_RUNNING = "running";
        public static final String STATUS_PAUSED = "paused";
        public static final String STATUS_FINISHED = "finished";

        private WorkoutSession() {
        }
    }

    public static final class SessionLog {
        public static final String TABLE = "session_logs";
        public static final String ID = "id";
        public static final String SESSION_ID = "session_id";
        public static final String EXERCISE_ID = "exercise_id";
        public static final String SETS_DONE = "sets_done";
        public static final String REPS_DONE = "reps_done";
        public static final String DURATION_SEC = "duration_sec";
        public static final String NOTE = "note";

        private SessionLog() {
        }
    }

    public static final class RoutePoint {
        public static final String TABLE = "route_points";
        public static final String ID = "id";
        public static final String SESSION_ID = "session_id";
        public static final String LATITUDE = "latitude";
        public static final String LONGITUDE = "longitude";
        public static final String ACCURACY = "accuracy";
        public static final String RECORDED_AT = "recorded_at";

        private RoutePoint() {
        }
    }

    public static final class GoalLog {
        public static final String TABLE = "goal_logs";
        public static final String ID = "id";
        public static final String USER_ID = "user_id";
        public static final String GOAL_TYPE = "goal_type";
        public static final String PERIOD_KEY = "period_key";
        public static final String ACHIEVED_VALUE = "achieved_value";
        public static final String NOTIFIED = "notified";
        public static final String ACHIEVED_AT = "achieved_at";

        public static final String TYPE_STEPS = "steps";
        public static final String TYPE_CALORIES = "calories";
        public static final String TYPE_DISTANCE = "distance";

        private GoalLog() {
        }
    }
}
