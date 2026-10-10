package com.btlltdd.fitnesstracker.fragment;

import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.btlltdd.fitnesstracker.activity.WorkoutSessionActivity;
import com.btlltdd.fitnesstracker.adapter.HistoryAdapter;
import com.btlltdd.fitnesstracker.database.UserDao;
import com.btlltdd.fitnesstracker.database.WorkoutDao;
import com.btlltdd.fitnesstracker.databinding.FragmentHomeBinding;
import com.btlltdd.fitnesstracker.model.User;
import com.btlltdd.fitnesstracker.model.WorkoutPlan;
import com.btlltdd.fitnesstracker.model.WorkoutSession;
import com.btlltdd.fitnesstracker.receiver.WorkoutUpdateReceiver;
import com.btlltdd.fitnesstracker.service.WorkoutForegroundService;
import com.btlltdd.fitnesstracker.service.WorkoutRuntime;
import com.btlltdd.fitnesstracker.util.BmiUtils;
import com.btlltdd.fitnesstracker.util.Labels;
import com.btlltdd.fitnesstracker.util.Receivers;
import com.btlltdd.fitnesstracker.util.SessionManager;
import com.btlltdd.fitnesstracker.util.TimeUtil;

import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;
    private WorkoutUpdateReceiver updateReceiver;
    private HistoryAdapter historyAdapter;
    private WorkoutPlan suggestion;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        historyAdapter = new HistoryAdapter(this::showSession);
        binding.recyclerHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerHistory.setAdapter(historyAdapter);
        binding.btnStart.setOnClickListener(v -> WorkoutSessionActivity.open(requireContext(), 0, "Tập tự do"));
        binding.btnOpenLive.setOnClickListener(v -> WorkoutSessionActivity.open(requireContext(), WorkoutRuntime.planId, WorkoutRuntime.title));
        binding.btnSuggestStart.setOnClickListener(v -> {
            if (suggestion != null) {
                WorkoutSessionActivity.open(requireContext(), suggestion.id, suggestion.name);
            }
        });
        binding.btnSuggestSave.setOnClickListener(v -> {
            if (suggestion == null) {
                return;
            }
            new WorkoutDao(requireContext()).copyTemplate(suggestion.id, new SessionManager(requireContext()).getUserId());
            Toast.makeText(requireContext(), "Đã lưu vào lộ trình của bạn", Toast.LENGTH_SHORT).show();
        });
        binding.btnCloseDangling.setOnClickListener(v -> {
            User user = new UserDao(requireContext()).get(new SessionManager(requireContext()).getUserId());
            double weight = user == null ? 60 : user.weightKg;
            new WorkoutDao(requireContext()).closeOpenSessions(new SessionManager(requireContext()).getUserId(), weight);
            load();
        });
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        updateReceiver = new WorkoutUpdateReceiver(intent -> load());
        IntentFilter filter = new IntentFilter(WorkoutForegroundService.ACTION_UPDATE);
        filter.addAction(WorkoutForegroundService.ACTION_FINISHED);
        Receivers.register(requireContext(), updateReceiver, filter, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        load();
    }

    @Override
    public void onStop() {
        if (updateReceiver != null && getContext() != null) {
            requireContext().unregisterReceiver(updateReceiver);
            updateReceiver = null;
        }
        super.onStop();
    }

    private void load() {
        if (binding == null || getContext() == null) {
            return;
        }
        long userId = new SessionManager(requireContext()).getUserId();
        User user = new UserDao(requireContext()).get(userId);
        WorkoutDao dao = new WorkoutDao(requireContext());
        if (user == null) {
            return;
        }
        double bmi = BmiUtils.bmi(user.heightCm, user.weightKg);
        binding.tvHello.setText("Xin chào, " + user.fullName);
        binding.tvBmi.setText(bmi <= 0
                ? "Hãy cập nhật chiều cao và cân nặng ở Hồ sơ"
                : "BMI " + bmi + " · " + BmiUtils.category(bmi) + " · " + Labels.goal(user.goal));
        double[] today = dao.todayTotals(userId);
        double steps = today[0];
        double calories = today[1];
        double km = today[2];
        if (WorkoutRuntime.active) {
            steps += WorkoutRuntime.steps;
            calories += WorkoutRuntime.liveCalories();
            km += WorkoutRuntime.distanceM / 1000.0;
        }
        binding.tvSteps.setText(String.format(Locale.US, "%.0f", steps));
        binding.tvCalories.setText(String.format(Locale.US, "%.0f", calories));
        binding.tvDistance.setText(String.format(Locale.US, "%.2f", km));
        binding.tvCompare.setText(compare(steps, user.targetSteps, "bước")
                + "   " + compare(calories, user.targetCalories, "kcal")
                + "   " + compare(km, user.targetDistanceKm, "km"));
        binding.cardLive.setVisibility(WorkoutRuntime.active ? View.VISIBLE : View.GONE);
        if (WorkoutRuntime.active) {
            binding.tvLive.setText((WorkoutRuntime.running ? "Đang tập" : "Tạm dừng")
                    + " · " + TimeUtil.clock((int) (WorkoutRuntime.elapsedMs / 1000))
                    + " · " + WorkoutRuntime.steps + " bước");
        }
        boolean dangling = !WorkoutRuntime.active && dao.hasOpenSession(userId);
        binding.cardDangling.setVisibility(dangling ? View.VISIBLE : View.GONE);
        suggestion = firstSuggestion(dao, user, bmi);
        binding.cardSuggest.setVisibility(suggestion == null ? View.GONE : View.VISIBLE);
        binding.tvSuggestEmpty.setVisibility(suggestion == null ? View.VISIBLE : View.GONE);
        if (suggestion != null) {
            binding.tvSuggestTitle.setText(suggestion.name);
            binding.tvSuggestMeta.setText(Labels.goal(suggestion.goal) + " · BMI "
                    + trim(suggestion.bmiMin) + "–" + trim(suggestion.bmiMax));
        }
        List<WorkoutSession> history = dao.recent(userId, 8);
        historyAdapter.submit(history);
        binding.tvHistoryEmpty.setVisibility(history.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private WorkoutPlan firstSuggestion(WorkoutDao dao, User user, double bmi) {
        List<WorkoutPlan> plans;
        if (bmi <= 0) {
            plans = dao.templatesForGoal(user.goal);
        } else {
            plans = dao.suggest(user.goal, bmi);
        }
        if (plans.isEmpty()) {
            plans = dao.visibleTemplates();
        }
        return plans.isEmpty() ? null : plans.get(0);
    }

    private void showSession(WorkoutSession session) {
        int points = new WorkoutDao(requireContext()).countRoute(session.id);
        String sync = session.synced ? "Đã đồng bộ" : "Chưa đồng bộ";
        Toast.makeText(requireContext(),
                session.steps + " bước, " + points + " điểm GPS, " + sync,
                Toast.LENGTH_LONG).show();
    }

    private String compare(double actual, double target, String unit) {
        if (target <= 0) {
            return unit;
        }
        return String.format(Locale.US, "%.0f/%.0f %s", actual, target, unit);
    }

    private String trim(double value) {
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
