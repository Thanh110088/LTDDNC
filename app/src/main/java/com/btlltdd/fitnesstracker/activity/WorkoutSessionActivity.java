package com.btlltdd.fitnesstracker.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.btlltdd.fitnesstracker.database.ExerciseDao;
import com.btlltdd.fitnesstracker.database.WorkoutDao;
import com.btlltdd.fitnesstracker.databinding.ActivityWorkoutSessionBinding;
import com.btlltdd.fitnesstracker.databinding.DialogAddMoveBinding;
import com.btlltdd.fitnesstracker.databinding.ItemResultInputBinding;
import com.btlltdd.fitnesstracker.model.Exercise;
import com.btlltdd.fitnesstracker.model.PlanExercise;
import com.btlltdd.fitnesstracker.receiver.WorkoutUpdateReceiver;
import com.btlltdd.fitnesstracker.service.WorkoutForegroundService;
import com.btlltdd.fitnesstracker.service.WorkoutRuntime;
import com.btlltdd.fitnesstracker.util.Receivers;
import com.btlltdd.fitnesstracker.util.SessionManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WorkoutSessionActivity extends AppCompatActivity {
    private ActivityWorkoutSessionBinding binding;
    private WorkoutUpdateReceiver updateReceiver;
    private long planId;
    private String title = "Tập tự do";
    private boolean clockRunning;
    private boolean askedFinish;
    private AlertDialog resultDialog;

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> startWorkout());

    public static void open(Context context, long planId, String title) {
        if (WorkoutRuntime.active && WorkoutRuntime.planId != planId) {
            Toast.makeText(context, "Hãy kết thúc buổi tập hiện tại trước", Toast.LENGTH_SHORT).show();
        }
        Intent intent = new Intent(context, WorkoutSessionActivity.class);
        intent.putExtra(WorkoutForegroundService.EXTRA_PLAN_ID, planId);
        intent.putExtra(WorkoutForegroundService.EXTRA_TITLE, title);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWorkoutSessionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        readIntent(getIntent());
        binding.tvTitle.setText(title);
        showMoves();
        binding.btnPause.setOnClickListener(v -> {
            Intent intent = new Intent(this, WorkoutForegroundService.class);
            intent.setAction(WorkoutRuntime.running
                    ? WorkoutForegroundService.ACTION_PAUSE
                    : WorkoutForegroundService.ACTION_RESUME);
            startService(intent);
        });
        binding.btnFinish.setOnClickListener(v -> showResultDialog());
        if (!WorkoutRuntime.active) {
            permissionLauncher.launch(neededPermissions());
        } else {
            render(WorkoutRuntime.elapsedMs, WorkoutRuntime.running, WorkoutRuntime.steps,
                    WorkoutRuntime.distanceM, WorkoutRuntime.liveCalories(),
                    WorkoutRuntime.stepSource, WorkoutRuntime.locationOn);
        }
        if (WorkoutForegroundService.ACTION_REQUEST_FINISH.equals(getIntent().getAction())) {
            binding.getRoot().post(this::showResultDialog);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (WorkoutForegroundService.ACTION_REQUEST_FINISH.equals(intent.getAction())) {
            showResultDialog();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateReceiver = new WorkoutUpdateReceiver(this::onUpdate);
        IntentFilter filter = new IntentFilter(WorkoutForegroundService.ACTION_UPDATE);
        filter.addAction(WorkoutForegroundService.ACTION_FINISHED);
        Receivers.register(this, updateReceiver, filter, false);
    }

    @Override
    protected void onStop() {
        if (updateReceiver != null) {
            unregisterReceiver(updateReceiver);
            updateReceiver = null;
        }
        super.onStop();
    }

    private void readIntent(Intent intent) {
        planId = intent.getLongExtra(WorkoutForegroundService.EXTRA_PLAN_ID, 0);
        String extraTitle = intent.getStringExtra(WorkoutForegroundService.EXTRA_TITLE);
        if (extraTitle != null && !extraTitle.isEmpty()) {
            title = extraTitle;
        }
        if (WorkoutRuntime.active) {
            planId = WorkoutRuntime.planId;
            title = WorkoutRuntime.title;
        }
    }

    private void startWorkout() {
        if (WorkoutRuntime.active) {
            return;
        }
        Intent intent = new Intent(this, WorkoutForegroundService.class);
        intent.setAction(WorkoutForegroundService.ACTION_START);
        intent.putExtra(WorkoutForegroundService.EXTRA_PLAN_ID, planId);
        intent.putExtra(WorkoutForegroundService.EXTRA_TITLE, title);
        ContextCompat.startForegroundService(this, intent);
    }

    private void onUpdate(Intent intent) {
        if (WorkoutForegroundService.ACTION_FINISHED.equals(intent.getAction())) {
            double calories = intent.getDoubleExtra(WorkoutForegroundService.EXTRA_CALORIES, 0);
            Toast.makeText(this, String.format(Locale.US, "Đã lưu buổi tập · %.1f kcal", calories), Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        render(
                intent.getLongExtra(WorkoutForegroundService.EXTRA_ELAPSED, 0),
                intent.getBooleanExtra(WorkoutForegroundService.EXTRA_RUNNING, false),
                intent.getIntExtra(WorkoutForegroundService.EXTRA_STEPS, 0),
                intent.getDoubleExtra(WorkoutForegroundService.EXTRA_DISTANCE, 0),
                intent.getDoubleExtra(WorkoutForegroundService.EXTRA_CALORIES, 0),
                intent.getStringExtra(WorkoutForegroundService.EXTRA_SOURCE),
                intent.getBooleanExtra(WorkoutForegroundService.EXTRA_LOCATION, false));
    }

    private void render(long elapsed, boolean running, int steps, double distance, double calories,
                        String source, boolean locationOn) {
        if (running && !clockRunning) {
            binding.chronometer.setBase(SystemClock.elapsedRealtime() - elapsed);
            binding.chronometer.start();
            clockRunning = true;
        } else if (!running) {
            binding.chronometer.setBase(SystemClock.elapsedRealtime() - elapsed);
            binding.chronometer.stop();
            clockRunning = false;
        }
        binding.tvStatus.setText(running ? "Đang tập" : "Tạm dừng");
        binding.btnPause.setText(running ? "Tạm dừng" : "Tiếp tục");
        binding.tvSteps.setText(String.valueOf(steps));
        binding.tvDistance.setText(String.format(Locale.US, "%.2f", distance / 1000.0));
        binding.tvCalories.setText(String.format(Locale.US, "%.1f", calories));
        String locationText = locationOn ? "GPS đang ghi" : "Chưa có GPS";
        binding.tvSource.setText((source == null ? "" : source) + " · " + locationText);
    }

    private void showMoves() {
        binding.containerMoves.removeAllViews();
        long shownPlan = WorkoutRuntime.active ? WorkoutRuntime.planId : planId;
        if (shownPlan <= 0) {
            addLine("Tập tự do. Khi kết thúc, nhập số hiệp và số lần.");
            return;
        }
        List<PlanExercise> moves = new WorkoutDao(this).exercisesOf(shownPlan);
        if (moves.isEmpty()) {
            addLine("Lộ trình chưa có bài. Bạn vẫn bấm giờ và lưu được buổi tập.");
            return;
        }
        for (PlanExercise move : moves) {
            String line = move.exerciseName + " · " + move.sets + " hiệp × " + move.reps + " lần";
            if (move.durationSec > 0) {
                line += " · " + move.durationSec + " giây";
            }
            addLine(line);
        }
    }

    private void addLine(String text) {
        android.widget.TextView view = new android.widget.TextView(this);
        view.setText(text);
        view.setTextSize(16);
        view.setTextColor(ContextCompat.getColor(this, com.btlltdd.fitnesstracker.R.color.text_primary));
        view.setPadding(0, 8, 0, 8);
        binding.containerMoves.addView(view);
    }

    private void showResultDialog() {
        if (resultDialog != null && resultDialog.isShowing()) {
            return;
        }
        if (!WorkoutRuntime.active && !askedFinish) {
            Toast.makeText(this, "Buổi tập chưa bắt đầu", Toast.LENGTH_SHORT).show();
            return;
        }
        long shownPlan = WorkoutRuntime.planId > 0 ? WorkoutRuntime.planId : planId;
        List<PlanExercise> moves = shownPlan > 0 ? new WorkoutDao(this).exercisesOf(shownPlan) : new ArrayList<>();
        if (moves.isEmpty()) {
            showFreeDialog();
            return;
        }
        ScrollView scroll = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(48, 16, 48, 8);
        List<ItemResultInputBinding> rows = new ArrayList<>();
        for (PlanExercise move : moves) {
            ItemResultInputBinding row = ItemResultInputBinding.inflate(getLayoutInflater(), box, false);
            row.tvName.setText(move.exerciseName);
            row.edtSets.setText(String.valueOf(move.sets));
            row.edtReps.setText(String.valueOf(move.reps));
            row.getRoot().setTag(move);
            box.addView(row.getRoot());
            rows.add(row);
        }
        scroll.addView(box);
        resultDialog = new AlertDialog.Builder(this)
                .setTitle("Kết quả từng bài")
                .setView(scroll)
                .setPositiveButton("Lưu", null)
                .setNegativeButton("Tiếp tục tập", null)
                .create();
        resultDialog.setOnShowListener(dialog -> resultDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            JSONArray array = new JSONArray();
            try {
                for (ItemResultInputBinding row : rows) {
                    PlanExercise move = (PlanExercise) row.getRoot().getTag();
                    JSONObject item = new JSONObject();
                    item.put("exerciseId", move.exerciseId);
                    item.put("sets", parseInt(row.edtSets.getText().toString()));
                    item.put("reps", parseInt(row.edtReps.getText().toString()));
                    item.put("durationSec", move.durationSec);
                    array.put(item);
                }
            } catch (Exception exception) {
                Toast.makeText(this, "Nhập số hiệp và số lần", Toast.LENGTH_SHORT).show();
                return;
            }
            resultDialog.dismiss();
            sendSave(array.toString());
        }));
        resultDialog.show();
    }

    private void showFreeDialog() {
        List<Exercise> exercises = new ExerciseDao(this).search(new SessionManager(this).getUserId(), "", "", "", "", false);
        if (exercises.isEmpty()) {
            sendSave("[]");
            return;
        }
        DialogAddMoveBinding form = DialogAddMoveBinding.inflate(getLayoutInflater());
        ArrayAdapter<String> names = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, namesOf(exercises));
        names.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        form.spExercise.setAdapter(names);
        resultDialog = new AlertDialog.Builder(this)
                .setTitle("Kết quả bài tập")
                .setView(form.getRoot())
                .setPositiveButton("Lưu", null)
                .setNegativeButton("Tiếp tục tập", null)
                .create();
        resultDialog.setOnShowListener(dialog -> resultDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                Exercise exercise = exercises.get(form.spExercise.getSelectedItemPosition());
                JSONArray array = new JSONArray();
                JSONObject item = new JSONObject();
                item.put("exerciseId", exercise.id);
                item.put("sets", parseInt(form.edtSets.getText().toString()));
                item.put("reps", parseInt(form.edtReps.getText().toString()));
                item.put("durationSec", parseInt(form.edtDuration.getText().toString()));
                array.put(item);
                resultDialog.dismiss();
                sendSave(array.toString());
            } catch (Exception exception) {
                Toast.makeText(this, "Nhập số hiệp và số lần", Toast.LENGTH_SHORT).show();
            }
        }));
        resultDialog.show();
    }

    private List<String> namesOf(List<Exercise> exercises) {
        List<String> names = new ArrayList<>();
        for (Exercise exercise : exercises) {
            names.add(exercise.name);
        }
        return names;
    }

    private void sendSave(String json) {
        askedFinish = true;
        Intent intent = new Intent(this, WorkoutForegroundService.class);
        intent.setAction(WorkoutForegroundService.ACTION_SAVE);
        intent.putExtra(WorkoutForegroundService.EXTRA_LOGS, json);
        startService(intent);
    }

    private int parseInt(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0;
        }
        return Integer.parseInt(value.trim());
    }

    private String[] neededPermissions() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        if (Build.VERSION.SDK_INT >= 29) {
            permissions.add(Manifest.permission.ACTIVITY_RECOGNITION);
        }
        if (Build.VERSION.SDK_INT >= 33) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        return permissions.toArray(new String[0]);
    }
}
