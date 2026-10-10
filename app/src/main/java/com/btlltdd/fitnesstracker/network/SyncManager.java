package com.btlltdd.fitnesstracker.network;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.btlltdd.fitnesstracker.database.UserDao;
import com.btlltdd.fitnesstracker.database.WorkoutDao;
import com.btlltdd.fitnesstracker.model.User;
import com.btlltdd.fitnesstracker.model.WorkoutSession;
import com.btlltdd.fitnesstracker.util.SessionManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Thay cho AsyncTask (đã deprecated): Executor chạy mạng, Handler trả kết quả về Main Thread.
 * Không gửi mật khẩu. Thành công thì đặt is_synced = 1.
 */
public final class SyncManager {
    public interface Callback {
        void onDone(String message);
    }

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);

    private SyncManager() {
    }

    public static void sync(Context context, Callback callback) {
        Context app = context.getApplicationContext();
        if (!RUNNING.compareAndSet(false, true)) {
            deliver(callback, "Đang đồng bộ");
            return;
        }
        EXECUTOR.execute(() -> {
            String message;
            try {
                message = run(app);
            } catch (Exception exception) {
                message = "Chưa đồng bộ được: " + exception.getMessage();
            } finally {
                RUNNING.set(false);
            }
            deliver(callback, message);
        });
    }

    private static String run(Context context) throws Exception {
        long userId = new SessionManager(context).getUserId();
        if (userId <= 0) {
            return "Chưa đăng nhập";
        }
        UserDao userDao = new UserDao(context);
        WorkoutDao workoutDao = new WorkoutDao(context);
        User user = userDao.get(userId);
        if (user == null) {
            return "Không thấy tài khoản";
        }
        List<WorkoutSession> sessions = workoutDao.unsynced(userId);
        if (user.synced && sessions.isEmpty()) {
            return "Không có dữ liệu cần đồng bộ";
        }
        JSONObject root = new JSONObject();
        root.put("userId", userId);
        JSONObject profile = new JSONObject();
        profile.put("fullName", user.fullName);
        profile.put("heightCm", user.heightCm);
        profile.put("weightKg", user.weightKg);
        profile.put("goal", user.goal);
        profile.put("targetSteps", user.targetSteps);
        profile.put("targetCalories", user.targetCalories);
        profile.put("targetDistanceKm", user.targetDistanceKm);
        root.put("profile", profile);
        JSONArray array = new JSONArray();
        List<Long> ids = new ArrayList<>();
        for (WorkoutSession session : sessions) {
            JSONObject item = new JSONObject();
            item.put("id", session.id);
            item.put("title", session.title);
            item.put("startTime", session.startTime);
            item.put("endTime", session.endTime);
            item.put("durationSec", session.durationSec);
            item.put("steps", session.steps);
            item.put("distanceM", session.distanceM);
            item.put("calories", session.calories);
            array.put(item);
            ids.add(session.id);
        }
        root.put("sessions", array);
        int code = HttpHelper.postJson(root.toString());
        if (code < 200 || code >= 300) {
            return "Máy chủ trả về mã " + code;
        }
        workoutDao.markSessionsSynced(ids);
        userDao.markSynced(userId);
        return "Đã đồng bộ " + ids.size() + " buổi tập";
    }

    private static void deliver(Callback callback, String message) {
        if (callback == null) {
            return;
        }
        MAIN.post(() -> callback.onDone(message));
    }
}
