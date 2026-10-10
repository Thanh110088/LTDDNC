package com.btlltdd.fitnesstracker.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.btlltdd.fitnesstracker.activity.LoginActivity;
import com.btlltdd.fitnesstracker.database.UserDao;
import com.btlltdd.fitnesstracker.database.WorkoutDao;
import com.btlltdd.fitnesstracker.databinding.FragmentProfileBinding;
import com.btlltdd.fitnesstracker.model.DayStat;
import com.btlltdd.fitnesstracker.model.User;
import com.btlltdd.fitnesstracker.network.SyncManager;
import com.btlltdd.fitnesstracker.service.WorkoutForegroundService;
import com.btlltdd.fitnesstracker.util.BmiUtils;
import com.btlltdd.fitnesstracker.util.Labels;
import com.btlltdd.fitnesstracker.util.SessionManager;

import java.util.List;
import java.util.Locale;

public class ProfileFragment extends Fragment {
    private FragmentProfileBinding binding;
    private boolean month;
    private boolean formReady;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        ArrayAdapter<Labels.Item> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, Labels.goals());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spGoal.setAdapter(adapter);
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                showBmi();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };
        binding.edtHeight.addTextChangedListener(watcher);
        binding.edtWeight.addTextChangedListener(watcher);
        binding.btnWeek.setOnClickListener(v -> {
            month = false;
            loadChart();
        });
        binding.btnMonth.setOnClickListener(v -> {
            month = true;
            loadChart();
        });
        binding.btnSave.setOnClickListener(v -> save());
        binding.btnSync.setOnClickListener(v -> SyncManager.sync(requireContext(), message -> {
            if (binding != null) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                refreshSync();
                loadChart();
            }
        }));
        binding.btnLogout.setOnClickListener(v -> logout());
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!formReady) {
            fillForm();
            formReady = true;
        }
        showBmi();
        refreshSync();
        loadChart();
        showCompare();
    }

    private void fillForm() {
        User user = new UserDao(requireContext()).get(new SessionManager(requireContext()).getUserId());
        if (user == null) {
            return;
        }
        binding.edtHeight.setText(num(user.heightCm));
        binding.edtWeight.setText(num(user.weightKg));
        binding.edtSteps.setText(String.valueOf(user.targetSteps));
        binding.edtCalories.setText(num(user.targetCalories));
        binding.edtDistance.setText(num(user.targetDistanceKm));
        binding.spGoal.setSelection(Labels.indexOf(Labels.goals(), user.goal));
    }

    private void save() {
        double height = parse(text(binding.edtHeight));
        double weight = parse(text(binding.edtWeight));
        if (height < 50 || height > 250 || weight < 20 || weight > 300) {
            Toast.makeText(requireContext(), "Chiều cao 50–250 cm, cân nặng 20–300 kg", Toast.LENGTH_SHORT).show();
            return;
        }
        User user = new UserDao(requireContext()).get(new SessionManager(requireContext()).getUserId());
        if (user == null) {
            return;
        }
        user.heightCm = height;
        user.weightKg = weight;
        user.goal = Labels.key(binding.spGoal);
        user.targetSteps = (int) Math.max(0, parse(text(binding.edtSteps)));
        user.targetCalories = Math.max(0, parse(text(binding.edtCalories)));
        user.targetDistanceKm = Math.max(0, parse(text(binding.edtDistance)));
        new UserDao(requireContext()).updateProfile(user);
        Toast.makeText(requireContext(), "Đã lưu hồ sơ", Toast.LENGTH_SHORT).show();
        refreshSync();
        showCompare();
    }

    private void showBmi() {
        if (binding == null) {
            return;
        }
        double bmi = BmiUtils.bmi(parse(text(binding.edtHeight)), parse(text(binding.edtWeight)));
        binding.tvBmi.setText(bmi <= 0 ? "BMI —" : "BMI " + bmi);
        binding.tvBmiCategory.setText(BmiUtils.category(bmi));
    }

    private void loadChart() {
        if (binding == null) {
            return;
        }
        List<DayStat> stats = new WorkoutDao(requireContext()).stats(new SessionManager(requireContext()).getUserId(), month);
        String[] labels = new String[stats.size()];
        float[] values = new float[stats.size()];
        for (int i = 0; i < stats.size(); i++) {
            labels[i] = stats.get(i).label;
            values[i] = stats.get(i).calories;
        }
        binding.chart.setData(labels, values);
        binding.btnWeek.setTextColor(requireContext().getColor(month
                ? com.btlltdd.fitnesstracker.R.color.text_secondary
                : com.btlltdd.fitnesstracker.R.color.primary_dark));
        binding.btnMonth.setTextColor(requireContext().getColor(month
                ? com.btlltdd.fitnesstracker.R.color.primary_dark
                : com.btlltdd.fitnesstracker.R.color.text_secondary));
    }

    private void showCompare() {
        User user = new UserDao(requireContext()).get(new SessionManager(requireContext()).getUserId());
        if (user == null || binding == null) {
            return;
        }
        double[] today = new WorkoutDao(requireContext()).todayTotals(user.id);
        binding.tvCompare.setText("Hôm nay so với mục tiêu\n"
                + line("Bước chân", today[0], user.targetSteps, "")
                + "\n" + line("Calo", today[1], user.targetCalories, "kcal")
                + "\n" + line("Quãng đường", today[2], user.targetDistanceKm, "km"));
    }

    private void refreshSync() {
        User user = new UserDao(requireContext()).get(new SessionManager(requireContext()).getUserId());
        if (user == null || binding == null) {
            return;
        }
        binding.tvSyncProfile.setText(user.synced ? "Hồ sơ đã đồng bộ" : "Hồ sơ chưa đồng bộ");
    }

    private void logout() {
        Intent stop = new Intent(requireContext(), WorkoutForegroundService.class);
        stop.setAction(WorkoutForegroundService.ACTION_CANCEL);
        requireContext().startService(stop);
        new SessionManager(requireContext()).clear();
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private String line(String label, double actual, double target, String unit) {
        if (target <= 0) {
            return label + ": chưa đặt mục tiêu";
        }
        String state = actual + 0.001 >= target ? "Đã đạt" : "Chưa đạt";
        return label + ": " + String.format(Locale.US, "%.1f / %.1f %s · %s", actual, target, unit, state);
    }

    private String text(android.widget.EditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString();
    }

    private double parse(String value) {
        try {
            return Double.parseDouble(value.trim().replace(',', '.'));
        } catch (Exception exception) {
            return -1;
        }
    }

    private String num(double value) {
        if (value == (long) value) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
