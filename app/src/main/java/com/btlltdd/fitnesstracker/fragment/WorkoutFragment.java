package com.btlltdd.fitnesstracker.fragment;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
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
import com.btlltdd.fitnesstracker.activity.WorkoutSessionActivity;
import com.btlltdd.fitnesstracker.adapter.PlanAdapter;
import com.btlltdd.fitnesstracker.adapter.ScheduleAdapter;
import com.btlltdd.fitnesstracker.database.UserDao;
import com.btlltdd.fitnesstracker.database.WorkoutDao;
import com.btlltdd.fitnesstracker.databinding.DialogScheduleBinding;
import com.btlltdd.fitnesstracker.databinding.FragmentWorkoutBinding;
import com.btlltdd.fitnesstracker.model.User;
import com.btlltdd.fitnesstracker.model.WorkoutPlan;
import com.btlltdd.fitnesstracker.model.WorkoutSchedule;
import com.btlltdd.fitnesstracker.util.AlarmScheduler;
import com.btlltdd.fitnesstracker.util.BmiUtils;
import com.btlltdd.fitnesstracker.util.Labels;
import com.btlltdd.fitnesstracker.util.SessionManager;
import com.btlltdd.fitnesstracker.util.TimeUtil;

import java.util.Calendar;
import java.util.List;

public class WorkoutFragment extends Fragment {
    private FragmentWorkoutBinding binding;
    private PlanAdapter suggestAdapter;
    private PlanAdapter mineAdapter;
    private ScheduleAdapter scheduleAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentWorkoutBinding.inflate(inflater, container, false);
        suggestAdapter = new PlanAdapter(new PlanAdapter.Listener() {
            @Override
            public void onPrimary(WorkoutPlan plan) {
                WorkoutSessionActivity.open(requireContext(), plan.id, plan.name);
            }

            @Override
            public void onSecondary(WorkoutPlan plan) {
                new WorkoutDao(requireContext()).copyTemplate(plan.id, new SessionManager(requireContext()).getUserId());
                Toast.makeText(requireContext(), "Đã thêm vào lộ trình của bạn", Toast.LENGTH_SHORT).show();
                load();
            }

            @Override
            public void onDelete(WorkoutPlan plan) {
            }
        });
        mineAdapter = new PlanAdapter(new PlanAdapter.Listener() {
            @Override
            public void onPrimary(WorkoutPlan plan) {
                WorkoutSessionActivity.open(requireContext(), plan.id, plan.name);
            }

            @Override
            public void onSecondary(WorkoutPlan plan) {
                startActivity(PlanEditorActivity.intent(requireContext(), plan.id, false));
            }

            @Override
            public void onDelete(WorkoutPlan plan) {
                confirmDelete(plan);
            }
        });
        scheduleAdapter = new ScheduleAdapter(new ScheduleAdapter.Listener() {
            @Override
            public void onToggle(WorkoutSchedule schedule, boolean enabled) {
                WorkoutDao dao = new WorkoutDao(requireContext());
                dao.setScheduleEnabled(schedule.id, enabled);
                schedule.enabled = enabled;
                if (enabled) {
                    Toast.makeText(requireContext(), AlarmScheduler.schedule(requireContext(), schedule), Toast.LENGTH_SHORT).show();
                } else {
                    AlarmScheduler.cancel(requireContext(), schedule.alarmCode);
                }
            }

            @Override
            public void onDelete(WorkoutSchedule schedule) {
                AlarmScheduler.cancel(requireContext(), schedule.alarmCode);
                new WorkoutDao(requireContext()).deleteSchedule(schedule.id);
                load();
            }
        });
        binding.recyclerSuggest.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerMine.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerSchedule.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerSuggest.setAdapter(suggestAdapter);
        binding.recyclerMine.setAdapter(mineAdapter);
        binding.recyclerSchedule.setAdapter(scheduleAdapter);
        binding.btnFree.setOnClickListener(v -> WorkoutSessionActivity.open(requireContext(), 0, "Tập tự do"));
        binding.btnAddPlan.setOnClickListener(v -> startActivity(PlanEditorActivity.intent(requireContext(), 0, false)));
        binding.btnAddSchedule.setOnClickListener(v -> showScheduleDialog());
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
        long userId = new SessionManager(requireContext()).getUserId();
        User user = new UserDao(requireContext()).get(userId);
        WorkoutDao dao = new WorkoutDao(requireContext());
        double bmi = user == null ? 0 : BmiUtils.bmi(user.heightCm, user.weightKg);
        String goal = user == null ? "maintain" : user.goal;
        List<WorkoutPlan> suggested = bmi <= 0 ? dao.templatesForGoal(goal) : dao.suggest(goal, bmi);
        boolean fallback = suggested.isEmpty();
        if (fallback) {
            suggested = dao.visibleTemplates();
        }
        binding.tvSuggestCaption.setText(fallback ? "Lộ trình mẫu" : "Gợi ý theo BMI và mục tiêu");
        suggestAdapter.submit(suggested, PlanAdapter.SUGGEST);
        binding.tvSuggestEmpty.setVisibility(suggested.isEmpty() ? View.VISIBLE : View.GONE);
        List<WorkoutPlan> mine = dao.myPlans(userId);
        mineAdapter.submit(mine, PlanAdapter.MINE);
        binding.tvMineEmpty.setVisibility(mine.isEmpty() ? View.VISIBLE : View.GONE);
        List<WorkoutSchedule> schedules = dao.schedulesOf(userId);
        scheduleAdapter.submit(schedules);
        binding.tvScheduleEmpty.setVisibility(schedules.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void confirmDelete(WorkoutPlan plan) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xóa lộ trình")
                .setMessage("Xóa \"" + plan.name + "\"?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    new WorkoutDao(requireContext()).deletePlan(plan.id);
                    load();
                })
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void showScheduleDialog() {
        DialogScheduleBinding form = DialogScheduleBinding.inflate(getLayoutInflater());
        ArrayAdapter<Labels.Item> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, Labels.repeats());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        form.spRepeat.setAdapter(adapter);
        Calendar chosen = Calendar.getInstance();
        chosen.add(Calendar.MINUTE, 2);
        chosen.set(Calendar.SECOND, 0);
        boolean[] picked = {false};
        form.btnPickTime.setOnClickListener(v -> new DatePickerDialog(requireContext(), (view, year, month, day) ->
                new TimePickerDialog(requireContext(), (timeView, hour, minute) -> {
                    chosen.set(year, month, day, hour, minute, 0);
                    picked[0] = true;
                    form.tvTime.setText(TimeUtil.display(TimeUtil.format(chosen.getTime())));
                }, chosen.get(Calendar.HOUR_OF_DAY), chosen.get(Calendar.MINUTE), true).show(),
                chosen.get(Calendar.YEAR), chosen.get(Calendar.MONTH), chosen.get(Calendar.DAY_OF_MONTH)).show());
        new AlertDialog.Builder(requireContext())
                .setTitle("Lịch tập")
                .setView(form.getRoot())
                .setPositiveButton("Đặt nhắc", (dialog, which) -> saveSchedule(form, chosen, picked[0]))
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void saveSchedule(DialogScheduleBinding form, Calendar chosen, boolean picked) {
        String title = form.edtTitle.getText() == null ? "" : form.edtTitle.getText().toString().trim();
        if (title.isEmpty() || !picked) {
            Toast.makeText(requireContext(), "Nhập tên và chọn ngày giờ", Toast.LENGTH_SHORT).show();
            return;
        }
        WorkoutSchedule schedule = new WorkoutSchedule();
        schedule.userId = new SessionManager(requireContext()).getUserId();
        schedule.title = title;
        schedule.scheduleTime = TimeUtil.format(chosen.getTime());
        schedule.repeatType = Labels.key(form.spRepeat);
        schedule.weekday = chosen.get(Calendar.DAY_OF_WEEK);
        schedule.alarmCode = AlarmScheduler.newCode();
        schedule.enabled = true;
        WorkoutDao dao = new WorkoutDao(requireContext());
        long id = dao.insertSchedule(schedule);
        if (id <= 0) {
            schedule.alarmCode = AlarmScheduler.newCode() + 7;
            id = dao.insertSchedule(schedule);
        }
        if (id <= 0) {
            Toast.makeText(requireContext(), "Không lưu được lịch", Toast.LENGTH_SHORT).show();
            return;
        }
        schedule.id = id;
        String message = AlarmScheduler.schedule(requireContext(), schedule);
        if (message.startsWith("Giờ")) {
            dao.deleteSchedule(id);
        }
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
        load();
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
