package com.btlltdd.fitnesstracker.activity;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.btlltdd.fitnesstracker.adapter.MoveAdapter;
import com.btlltdd.fitnesstracker.database.ExerciseDao;
import com.btlltdd.fitnesstracker.database.WorkoutDao;
import com.btlltdd.fitnesstracker.databinding.ActivityPlanEditorBinding;
import com.btlltdd.fitnesstracker.databinding.DialogAddMoveBinding;
import com.btlltdd.fitnesstracker.model.Exercise;
import com.btlltdd.fitnesstracker.model.PlanExercise;
import com.btlltdd.fitnesstracker.model.WorkoutPlan;
import com.btlltdd.fitnesstracker.util.Labels;
import com.btlltdd.fitnesstracker.util.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class PlanEditorActivity extends AppCompatActivity {
    public static final String EXTRA_PLAN_ID = "plan_id";
    public static final String EXTRA_TEMPLATE = "template";

    private ActivityPlanEditorBinding binding;
    private final List<PlanExercise> moves = new ArrayList<>();
    private MoveAdapter adapter;
    private long planId;
    private boolean template;

    public static android.content.Intent intent(android.content.Context context, long planId, boolean template) {
        android.content.Intent intent = new android.content.Intent(context, PlanEditorActivity.class);
        intent.putExtra(EXTRA_PLAN_ID, planId);
        intent.putExtra(EXTRA_TEMPLATE, template);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPlanEditorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        planId = getIntent().getLongExtra(EXTRA_PLAN_ID, 0);
        template = getIntent().getBooleanExtra(EXTRA_TEMPLATE, false);
        binding.tvHeading.setText(template ? "Lộ trình mẫu" : "Lộ trình của tôi");
        binding.groupTemplate.setVisibility(template ? android.view.View.VISIBLE : android.view.View.GONE);
        setSpinner(binding.spGoal, Labels.goals());
        setSpinner(binding.spLevel, Labels.levels(false));
        adapter = new MoveAdapter(item -> {
            moves.remove(item);
            adapter.submit(new ArrayList<>(moves));
        });
        binding.recyclerMoves.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerMoves.setAdapter(adapter);
        if (planId > 0) {
            WorkoutPlan plan = new WorkoutDao(this).getPlan(planId);
            if (plan != null) {
                binding.edtName.setText(plan.name);
                binding.edtDescription.setText(plan.description);
                binding.spGoal.setSelection(Labels.indexOf(Labels.goals(), plan.goal));
                binding.edtBmiMin.setText(String.valueOf(plan.bmiMin));
                binding.edtBmiMax.setText(String.valueOf(plan.bmiMax));
                binding.spLevel.setSelection(Labels.indexOf(Labels.levels(false), plan.level));
            }
            moves.addAll(new WorkoutDao(this).exercisesOf(planId));
            adapter.submit(new ArrayList<>(moves));
        } else if (template) {
            binding.edtBmiMin.setText("0");
            binding.edtBmiMax.setText("99");
        }
        binding.btnAddMove.setOnClickListener(v -> addMove());
        binding.btnSave.setOnClickListener(v -> save());
    }

    private void addMove() {
        List<Exercise> exercises = new ExerciseDao(this).search(
                new SessionManager(this).getUserId(), "", "", "", "", false);
        if (exercises.isEmpty()) {
            Toast.makeText(this, "Chưa có bài tập", Toast.LENGTH_SHORT).show();
            return;
        }
        DialogAddMoveBinding form = DialogAddMoveBinding.inflate(getLayoutInflater());
        ArrayAdapter<Exercise> exerciseAdapter = new ArrayAdapter<Exercise>(this, android.R.layout.simple_spinner_item, exercises) {
            @Override
            public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                android.view.View view = super.getView(position, convertView, parent);
                ((android.widget.TextView) view).setText(exercises.get(position).name);
                return view;
            }

            @Override
            public android.view.View getDropDownView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                android.view.View view = super.getDropDownView(position, convertView, parent);
                ((android.widget.TextView) view).setText(exercises.get(position).name);
                return view;
            }
        };
        exerciseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        form.spExercise.setAdapter(exerciseAdapter);
        new AlertDialog.Builder(this)
                .setTitle("Thêm bài")
                .setView(form.getRoot())
                .setPositiveButton("Thêm", (dialog, which) -> {
                    Exercise exercise = exercises.get(form.spExercise.getSelectedItemPosition());
                    PlanExercise move = new PlanExercise();
                    move.exerciseId = exercise.id;
                    move.exerciseName = exercise.name;
                    move.met = exercise.met;
                    move.sets = (int) number(form.edtSets, 3);
                    move.reps = (int) number(form.edtReps, 10);
                    move.durationSec = (int) number(form.edtDuration, 0);
                    moves.add(move);
                    adapter.submit(new ArrayList<>(moves));
                })
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void save() {
        String name = text(binding.edtName);
        if (name.isEmpty()) {
            Toast.makeText(this, "Nhập tên lộ trình", Toast.LENGTH_SHORT).show();
            return;
        }
        WorkoutPlan plan = planId > 0 ? new WorkoutDao(this).getPlan(planId) : new WorkoutPlan();
        if (plan == null) {
            plan = new WorkoutPlan();
        }
        plan.id = planId;
        plan.template = template;
        plan.userId = new SessionManager(this).getUserId();
        plan.name = name;
        plan.description = text(binding.edtDescription);
        plan.goal = Labels.key(binding.spGoal);
        plan.level = template ? Labels.key(binding.spLevel) : "beginner";
        plan.bmiMin = template ? number(binding.edtBmiMin, 0) : 0;
        plan.bmiMax = template ? number(binding.edtBmiMax, 99) : 99;
        long id = new WorkoutDao(this).savePlan(plan);
        if (id <= 0) {
            Toast.makeText(this, "Không lưu được", Toast.LENGTH_SHORT).show();
            return;
        }
        new WorkoutDao(this).replaceExercises(id, moves);
        Toast.makeText(this, "Đã lưu lộ trình", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void setSpinner(android.widget.Spinner spinner, java.util.List<Labels.Item> items) {
        ArrayAdapter<Labels.Item> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(arrayAdapter);
    }

    private String text(android.widget.EditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private double number(android.widget.EditText editText, double fallback) {
        try {
            return Double.parseDouble(text(editText).replace(',', '.'));
        } catch (Exception exception) {
            return fallback;
        }
    }
}
