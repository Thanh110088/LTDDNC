package com.btlltdd.fitnesstracker.fragment;

import android.content.Intent;
import android.net.Uri;
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
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.btlltdd.fitnesstracker.adapter.ExerciseAdapter;
import com.btlltdd.fitnesstracker.database.ExerciseDao;
import com.btlltdd.fitnesstracker.databinding.FragmentExerciseBinding;
import com.btlltdd.fitnesstracker.model.Exercise;
import com.btlltdd.fitnesstracker.util.Labels;
import com.btlltdd.fitnesstracker.util.SessionManager;

import java.util.List;
import java.util.Locale;

public class ExerciseFragment extends Fragment {
    private FragmentExerciseBinding binding;
    private ExerciseAdapter adapter;
    private boolean ready;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentExerciseBinding.inflate(inflater, container, false);
        adapter = new ExerciseAdapter(new ExerciseAdapter.Listener() {
            @Override
            public void onOpen(Exercise exercise) {
                showDetail(exercise);
            }

            @Override
            public void onBookmark(Exercise exercise) {
                toggle(exercise);
            }
        });
        binding.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recycler.setAdapter(adapter);
        fill(binding.spMuscle, Labels.muscles(true));
        fill(binding.spDifficulty, Labels.levels(true));
        fill(binding.spEquipment, Labels.equipment(true));
        android.widget.AdapterView.OnItemSelectedListener reload = new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                load();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        };
        binding.spMuscle.setOnItemSelectedListener(reload);
        binding.spDifficulty.setOnItemSelectedListener(reload);
        binding.spEquipment.setOnItemSelectedListener(reload);
        binding.edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                load();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        ready = true;
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        if (!ready || binding == null || getContext() == null) {
            return;
        }
        long userId = new SessionManager(requireContext()).getUserId();
        List<Exercise> items = new ExerciseDao(requireContext()).search(
                userId,
                binding.edtSearch.getText().toString(),
                Labels.key(binding.spMuscle),
                Labels.key(binding.spDifficulty),
                Labels.key(binding.spEquipment),
                false);
        adapter.submit(items);
        binding.tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showDetail(Exercise exercise) {
        String message = Labels.muscle(exercise.muscleGroup) + " · " + Labels.difficulty(exercise.difficulty)
                + "\n" + Labels.equipment(exercise.equipment)
                + "\nMET " + String.format(Locale.US, "%.1f", exercise.met)
                + "\n\n" + (exercise.description.isEmpty() ? "Chưa có mô tả." : exercise.description);
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                .setTitle(exercise.name)
                .setMessage(message)
                .setPositiveButton("Đóng", null)
                .setNegativeButton(exercise.bookmarked ? "Bỏ lưu" : "Yêu thích", (dialog, which) -> toggle(exercise));
        if (!exercise.videoUrl.isEmpty()) {
            builder.setNeutralButton("Xem video", (dialog, which) -> {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(exercise.videoUrl)));
                } catch (Exception exception) {
                    Toast.makeText(requireContext(), "Không mở được video", Toast.LENGTH_SHORT).show();
                }
            });
        }
        builder.show();
    }

    private void toggle(Exercise exercise) {
        boolean saved = new ExerciseDao(requireContext())
                .toggleBookmark(new SessionManager(requireContext()).getUserId(), exercise.id);
        Toast.makeText(requireContext(), saved ? "Đã đánh dấu yêu thích" : "Đã bỏ yêu thích", Toast.LENGTH_SHORT).show();
        load();
    }

    private void fill(android.widget.Spinner spinner, List<Labels.Item> items) {
        ArrayAdapter<Labels.Item> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    @Override
    public void onDestroyView() {
        ready = false;
        binding = null;
        super.onDestroyView();
    }
}
