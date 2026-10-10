package com.btlltdd.fitnesstracker.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.btlltdd.fitnesstracker.activity.PlanEditorActivity;
import com.btlltdd.fitnesstracker.adapter.AdminExerciseAdapter;
import com.btlltdd.fitnesstracker.adapter.PlanAdapter;
import com.btlltdd.fitnesstracker.adapter.UserAdapter;
import com.btlltdd.fitnesstracker.database.ExerciseDao;
import com.btlltdd.fitnesstracker.database.UserDao;
import com.btlltdd.fitnesstracker.database.WorkoutDao;
import com.btlltdd.fitnesstracker.databinding.DialogExerciseBinding;
import com.btlltdd.fitnesstracker.databinding.FragmentAdminBinding;
import com.btlltdd.fitnesstracker.model.Exercise;
import com.btlltdd.fitnesstracker.model.User;
import com.btlltdd.fitnesstracker.model.WorkoutPlan;
import com.btlltdd.fitnesstracker.util.Labels;
import com.btlltdd.fitnesstracker.util.SessionManager;

import java.util.Locale;

public class AdminFragment extends Fragment {
    private FragmentAdminBinding binding;
    private UserAdapter userAdapter;
    private AdminExerciseAdapter exerciseAdapter;
    private PlanAdapter planAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminBinding.inflate(inflater, container, false);
        long me = new SessionManager(requireContext()).getUserId();
        userAdapter = new UserAdapter(me, (user, active) -> {
            if (user.id == me) {
                Toast.makeText(requireContext(), "Không khóa chính tài khoản đang dùng", Toast.LENGTH_SHORT).show();
                load();
                return;
            }
            new UserDao(requireContext()).setActive(user.id, active);
            Toast.makeText(requireContext(), active ? "Đã mở khóa" : "Đã khóa tài khoản", Toast.LENGTH_SHORT).show();
        });
        exerciseAdapter = new AdminExerciseAdapter(new AdminExerciseAdapter.Listener() {
            @Override
            public void onEdit(Exercise exercise) {
                showExerciseDialog(exercise);
            }

            @Override
            public void onHide(Exercise exercise) {
                new ExerciseDao(requireContext()).setHidden(exercise.id, !exercise.hidden);
                load();
            }

            @Override
            public void onDelete(Exercise exercise) {
                confirm("Xóa bài tập", "Xóa \"" + exercise.name + "\" khỏi thư viện?", () -> {
                    new ExerciseDao(requireContext()).delete(exercise.id);
                    load();
                });
            }
        });
        planAdapter = new PlanAdapter(new PlanAdapter.Listener() {
            @Override
            public void onPrimary(WorkoutPlan plan) {
                startActivity(PlanEditorActivity.intent(requireContext(), plan.id, true));
            }

            @Override
            public void onSecondary(WorkoutPlan plan) {
                new WorkoutDao(requireContext()).setPlanHidden(plan.id, !plan.hidden);
                load();
            }

            @Override
            public void onDelete(WorkoutPlan plan) {
                confirm("Xóa lộ trình mẫu", "Xóa \"" + plan.name + "\"?", () -> {
                    new WorkoutDao(requireContext()).deletePlan(plan.id);
                    load();
                });
            }
        });
        binding.recyclerUsers.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerExercises.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerPlans.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerUsers.setAdapter(userAdapter);
        binding.recyclerExercises.setAdapter(exerciseAdapter);
        binding.recyclerPlans.setAdapter(planAdapter);
        binding.btnAddExercise.setOnClickListener(v -> showExerciseDialog(null));
        binding.btnAddPlan.setOnClickListener(v -> startActivity(PlanEditorActivity.intent(requireContext(), 0, true)));
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        if (binding == null || getContext() == null) {
            return;
        }
        userAdapter.submit(new UserDao(requireContext()).listAll());
        long userId = new SessionManager(requireContext()).getUserId();
        exerciseAdapter.submit(new ExerciseDao(requireContext()).search(userId, "", "", "", "", true));
        planAdapter.submit(new WorkoutDao(requireContext()).adminTemplates(), PlanAdapter.ADMIN);
    }

    private void showExerciseDialog(@Nullable Exercise current) {
        DialogExerciseBinding form = DialogExerciseBinding.inflate(getLayoutInflater());
        setSpinner(form.spMuscle, Labels.muscles(false));
        setSpinner(form.spDifficulty, Labels.levels(false));
        setSpinner(form.spEquipment, Labels.equipment(false));
        if (current != null) {
            form.edtName.setText(current.name);
            form.edtDescription.setText(current.description);
            form.edtVideo.setText(current.videoUrl);
            form.edtMet.setText(String.format(Locale.US, "%.1f", current.met));
            form.spMuscle.setSelection(Labels.indexOf(Labels.muscles(false), current.muscleGroup));
            form.spDifficulty.setSelection(Labels.indexOf(Labels.levels(false), current.difficulty));
            form.spEquipment.setSelection(Labels.indexOf(Labels.equipment(false), current.equipment));
        } else {
            form.edtMet.setText("5");
        }
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(current == null ? "Thêm bài tập" : "Sửa bài tập")
                .setView(form.getRoot())
                .setPositiveButton("Lưu", null)
                .setNegativeButton("Huỷ", null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = text(form.edtName);
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Nhập tên bài tập", Toast.LENGTH_SHORT).show();
                return;
            }
            Exercise exercise = current == null ? new Exercise() : current;
            exercise.name = name;
            exercise.description = text(form.edtDescription);
            exercise.videoUrl = text(form.edtVideo);
            exercise.muscleGroup = Labels.key(form.spMuscle);
            exercise.difficulty = Labels.key(form.spDifficulty);
            exercise.equipment = Labels.key(form.spEquipment);
            double met = 5;
            try {
                met = Double.parseDouble(text(form.edtMet).replace(',', '.'));
            } catch (Exception ignored) {
                met = 5;
            }
            exercise.met = met <= 0 ? 5 : met;
            new ExerciseDao(requireContext()).save(exercise);
            dialog.dismiss();
            load();
        }));
        dialog.show();
    }

    private void confirm(String title, String message, Runnable action) {
        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Xóa", (dialog, which) -> action.run())
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void setSpinner(android.widget.Spinner spinner, java.util.List<Labels.Item> items) {
        ArrayAdapter<Labels.Item> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private String text(android.widget.EditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
