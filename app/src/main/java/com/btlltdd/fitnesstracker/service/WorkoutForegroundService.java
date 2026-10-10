package com.btlltdd.fitnesstracker.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.btlltdd.fitnesstracker.R;
import com.btlltdd.fitnesstracker.activity.WorkoutSessionActivity;
import com.btlltdd.fitnesstracker.database.UserDao;
import com.btlltdd.fitnesstracker.database.WorkoutDao;
import com.btlltdd.fitnesstracker.model.User;
import com.btlltdd.fitnesstracker.network.SyncManager;
import com.btlltdd.fitnesstracker.util.CalorieUtils;
import com.btlltdd.fitnesstracker.util.GoalChecker;
import com.btlltdd.fitnesstracker.util.NotificationHelper;
import com.btlltdd.fitnesstracker.util.SessionManager;
import com.btlltdd.fitnesstracker.util.TimeUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Foreground Service của buổi tập.
 * startForeground() được gọi ngay trong onStartCommand để không bị hệ thống kill
 * khi người dùng tắt màn hình. Quyền cần có: FOREGROUND_SERVICE, POST_NOTIFICATIONS,
 * ACTIVITY_RECOGNITION (bước chân từ API 29), ACCESS_FINE_LOCATION (GPS).
 */
public class WorkoutForegroundService extends Service implements SensorEventListener {

    public static final String ACTION_START = "com.btlltdd.fitnesstracker.START";
    public static final String ACTION_PAUSE = "com.btlltdd.fitnesstracker.PAUSE";
    public static final String ACTION_RESUME = "com.btlltdd.fitnesstracker.RESUME";
    public static final String ACTION_SAVE = "com.btlltdd.fitnesstracker.SAVE";
    public static final String ACTION_CANCEL = "com.btlltdd.fitnesstracker.CANCEL";
    public static final String ACTION_UPDATE = "com.btlltdd.fitnesstracker.UPDATE";
    public static final String ACTION_FINISHED = "com.btlltdd.fitnesstracker.FINISHED";
    public static final String ACTION_REQUEST_FINISH = "com.btlltdd.fitnesstracker.REQUEST_FINISH";

    public static final String EXTRA_PLAN_ID = "plan_id";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_LOGS = "logs";
    public static final String EXTRA_STEPS = "steps";
    public static final String EXTRA_DISTANCE = "distance";
    public static final String EXTRA_ELAPSED = "elapsed";
    public static final String EXTRA_RUNNING = "running";
    public static final String EXTRA_CALORIES = "calories";
    public static final String EXTRA_SOURCE = "source";
    public static final String EXTRA_LOCATION = "location_on";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<double[]> route = new ArrayList<>();
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (!WorkoutRuntime.active) {
                return;
            }
            if (WorkoutRuntime.running) {
                WorkoutRuntime.elapsedMs = WorkoutRuntime.accumulatedMs
                        + (SystemClock.elapsedRealtime() - WorkoutRuntime.runStartedAt);
            }
            publish();
            handler.postDelayed(this, 1000);
        }
    };

    private SensorManager sensorManager;
    private LocationManager locationManager;
    private Location lastLocation;
    private boolean useStepCounter;
    private float stepBaseline = -1f;
    private int stepsAtPause;
    private int accelSteps;
    private long lastAccelStepAt;
    private boolean foregroundStarted;
    private boolean saving;

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            if (!WorkoutRuntime.running || location == null) {
                return;
            }
            if (location.getAccuracy() > 40) {
                return;
            }
            route.add(new double[]{
                    location.getLatitude(),
                    location.getLongitude(),
                    location.getAccuracy(),
                    location.getTime()
            });
            if (lastLocation != null) {
                float moved = lastLocation.distanceTo(location);
                if (moved >= 1f && moved <= 80f) {
                    WorkoutRuntime.distanceM += moved;
                }
            }
            lastLocation = location;
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? null : intent.getAction();
        if (ACTION_START.equals(action)) {
            ensureForeground();
            if (!WorkoutRuntime.active) {
                begin(intent);
            }
        } else if (ACTION_PAUSE.equals(action)) {
            pause();
        } else if (ACTION_RESUME.equals(action)) {
            resume();
        } else if (ACTION_SAVE.equals(action)) {
            saveAndStop(intent.getStringExtra(EXTRA_LOGS));
        } else if (ACTION_CANCEL.equals(action)) {
            if (WorkoutRuntime.active) {
                saveAndStop("[]");
            } else {
                shutdown();
            }
        } else if (!WorkoutRuntime.active) {
            stopSelf();
        }
        return START_STICKY;
    }

    private void begin(Intent intent) {
        long userId = new SessionManager(this).getUserId();
        if (userId <= 0) {
            shutdown();
            return;
        }
        long planId = intent.getLongExtra(EXTRA_PLAN_ID, 0);
        String title = intent.getStringExtra(EXTRA_TITLE);
        if (title == null || title.isEmpty()) {
            title = "Tập tự do";
        }
        WorkoutDao dao = new WorkoutDao(this);
        User user = new UserDao(this).get(userId);
        WorkoutRuntime.reset();
        WorkoutRuntime.active = true;
        WorkoutRuntime.running = true;
        WorkoutRuntime.userId = userId;
        WorkoutRuntime.planId = planId;
        WorkoutRuntime.title = title;
        WorkoutRuntime.runStartedAt = SystemClock.elapsedRealtime();
        WorkoutRuntime.met = planId > 0 ? dao.averageMet(planId) : 6;
        WorkoutRuntime.weightKg = user != null && user.weightKg > 0 ? user.weightKg : 60;
        WorkoutRuntime.sessionId = dao.openSession(userId, planId, title);
        listenSensors();
        listenLocation();
        handler.removeCallbacks(tick);
        handler.post(tick);
    }

    private void pause() {
        if (!WorkoutRuntime.active || !WorkoutRuntime.running) {
            return;
        }
        WorkoutRuntime.elapsedMs = WorkoutRuntime.accumulatedMs
                + (SystemClock.elapsedRealtime() - WorkoutRuntime.runStartedAt);
        WorkoutRuntime.accumulatedMs = WorkoutRuntime.elapsedMs;
        WorkoutRuntime.running = false;
        stepsAtPause = WorkoutRuntime.steps;
        accelSteps = 0;
        stepBaseline = -1f;
        publish();
    }

    private void resume() {
        if (!WorkoutRuntime.active || WorkoutRuntime.running) {
            return;
        }
        WorkoutRuntime.runStartedAt = SystemClock.elapsedRealtime();
        WorkoutRuntime.running = true;
        stepBaseline = -1f;
        publish();
    }

    private void saveAndStop(String logsJson) {
        if (saving || !WorkoutRuntime.active) {
            shutdown();
            return;
        }
        saving = true;
        if (WorkoutRuntime.running) {
            WorkoutRuntime.elapsedMs = WorkoutRuntime.accumulatedMs
                    + (SystemClock.elapsedRealtime() - WorkoutRuntime.runStartedAt);
        }
        int duration = (int) (WorkoutRuntime.elapsedMs / 1000);
        if (duration < 1) {
            duration = 1;
        }
        double calories = CalorieUtils.burn(WorkoutRuntime.met, WorkoutRuntime.weightKg, duration);
        WorkoutDao dao = new WorkoutDao(this);
        dao.finishSession(WorkoutRuntime.sessionId, duration, WorkoutRuntime.steps, WorkoutRuntime.distanceM, calories);
        writeLogs(dao, logsJson);
        dao.insertRoute(WorkoutRuntime.sessionId, new ArrayList<>(route));
        long userId = WorkoutRuntime.userId;
        GoalChecker.check(this, userId);
        SyncManager.sync(this, null);
        Intent done = new Intent(ACTION_FINISHED);
        done.setPackage(getPackageName());
        done.putExtra(EXTRA_CALORIES, calories);
        done.putExtra(EXTRA_ELAPSED, duration);
        sendBroadcast(done);
        shutdown();
    }

    private void writeLogs(WorkoutDao dao, String logsJson) {
        if (logsJson == null || logsJson.isEmpty()) {
            return;
        }
        try {
            JSONArray array = new JSONArray(logsJson);
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                dao.insertLog(
                        WorkoutRuntime.sessionId,
                        item.getLong("exerciseId"),
                        item.optInt("sets"),
                        item.optInt("reps"),
                        item.optInt("durationSec"));
            }
        } catch (Exception ignored) {
            // Form kết quả hỏng thì buổi tập vẫn được lưu theo thời gian và bước chân.
        }
    }

    /**
     * Ưu tiên TYPE_STEP_COUNTER. Máy ảo thường không có cảm biến này,
     * khi đó đếm đỉnh gia tốc để buổi tập vẫn có số bước khi demo.
     */
    private void listenSensors() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        boolean recognitionGranted = Build.VERSION.SDK_INT < 29
                || ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACTIVITY_RECOGNITION)
                == PackageManager.PERMISSION_GRANTED;
        Sensor stepSensor = recognitionGranted ? sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) : null;
        if (stepSensor != null) {
            useStepCounter = true;
            WorkoutRuntime.stepSource = "Cảm biến đếm bước";
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
            return;
        }
        Sensor accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accel != null) {
            useStepCounter = false;
            WorkoutRuntime.stepSource = "Gia tốc (máy không có đếm bước)";
            sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            WorkoutRuntime.stepSource = "Không có cảm biến bước";
        }
    }

    @SuppressLint("MissingPermission")
    private void listenLocation() {
        boolean fine = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        if (!fine && !coarse) {
            WorkoutRuntime.locationOn = false;
            return;
        }
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        WorkoutRuntime.locationOn = false;
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 1f, locationListener);
                WorkoutRuntime.locationOn = true;
            }
        } catch (Exception ignored) {
            // Máy ảo có thể không gắn GPS_PROVIDER.
        }
        try {
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000, 1f, locationListener);
                WorkoutRuntime.locationOn = true;
            }
        } catch (Exception ignored) {
            // Bỏ qua nếu không có nhà cung cấp mạng.
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!WorkoutRuntime.running || event == null) {
            return;
        }
        if (useStepCounter && event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            float totalSinceBoot = event.values[0];
            if (stepBaseline < 0) {
                stepBaseline = totalSinceBoot;
            }
            int delta = Math.round(totalSinceBoot - stepBaseline);
            WorkoutRuntime.steps = stepsAtPause + Math.max(delta, 0);
            return;
        }
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            float magnitude = (float) Math.sqrt(x * x + y * y + z * z);
            long now = SystemClock.elapsedRealtime();
            if (magnitude > 13f && now - lastAccelStepAt > 280) {
                lastAccelStepAt = now;
                accelSteps++;
                WorkoutRuntime.steps = stepsAtPause + accelSteps;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void publish() {
        Intent update = new Intent(ACTION_UPDATE);
        update.setPackage(getPackageName());
        update.putExtra(EXTRA_STEPS, WorkoutRuntime.steps);
        update.putExtra(EXTRA_DISTANCE, WorkoutRuntime.distanceM);
        update.putExtra(EXTRA_ELAPSED, WorkoutRuntime.elapsedMs);
        update.putExtra(EXTRA_RUNNING, WorkoutRuntime.running);
        update.putExtra(EXTRA_CALORIES, WorkoutRuntime.liveCalories());
        update.putExtra(EXTRA_SOURCE, WorkoutRuntime.stepSource);
        update.putExtra(EXTRA_LOCATION, WorkoutRuntime.locationOn);
        sendBroadcast(update);
        showNotification();
    }

    private void ensureForeground() {
        showNotification();
    }

    private void showNotification() {
        String status = WorkoutRuntime.running ? "Đang tập" : "Tạm dừng";
        String text = status + " · " + TimeUtil.clock((int) (WorkoutRuntime.elapsedMs / 1000))
                + " · " + WorkoutRuntime.steps + " bước · "
                + String.format(Locale.US, "%.2f km", WorkoutRuntime.distanceM / 1000.0);
        Notification notification = new NotificationCompat.Builder(this, NotificationHelper.CHANNEL_WORKOUT)
                .setSmallIcon(R.drawable.ic_stat_workout)
                .setContentTitle(WorkoutRuntime.title == null || WorkoutRuntime.title.isEmpty()
                        ? "Buổi tập" : WorkoutRuntime.title)
                .setContentText(text)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setContentIntent(openSession(false))
                .addAction(WorkoutRuntime.running ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play,
                        WorkoutRuntime.running ? "Tạm dừng" : "Tiếp tục",
                        serviceAction(WorkoutRuntime.running ? ACTION_PAUSE : ACTION_RESUME))
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Kết thúc", openSession(true))
                .build();
        if (!foregroundStarted) {
            if (Build.VERSION.SDK_INT >= 29) {
                int type = ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC;
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    type |= ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION;
                }
                startForeground(NotificationHelper.WORKOUT_ID, notification, type);
            } else {
                startForeground(NotificationHelper.WORKOUT_ID, notification);
            }
            foregroundStarted = true;
        } else {
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            manager.notify(NotificationHelper.WORKOUT_ID, notification);
        }
    }

    private PendingIntent openSession(boolean requestFinish) {
        Intent intent = new Intent(this, WorkoutSessionActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        if (requestFinish) {
            intent.setAction(ACTION_REQUEST_FINISH);
        }
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getActivity(this, requestFinish ? 13 : 11, intent, flags);
    }

    private PendingIntent serviceAction(String action) {
        Intent intent = new Intent(this, WorkoutForegroundService.class);
        intent.setAction(action);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getService(this, action.hashCode(), intent, flags);
    }

    private void stopLocationUpdates() {
        if (locationManager == null) {
            return;
        }
        try {
            locationManager.removeUpdates(locationListener);
        } catch (SecurityException ignored) {
            // Quyền vị trí có thể đã bị thu hồi trong lúc tập.
        }
    }

    private void shutdown() {
        handler.removeCallbacks(tick);
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        stopLocationUpdates();
        WorkoutRuntime.reset();
        route.clear();
        if (foregroundStarted) {
            stopForeground(STOP_FOREGROUND_REMOVE);
            foregroundStarted = false;
        }
        stopSelf();
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(tick);
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        stopLocationUpdates();
        super.onDestroy();
    }
}
