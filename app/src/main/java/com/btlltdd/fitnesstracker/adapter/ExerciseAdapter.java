package com.btlltdd.fitnesstracker.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.btlltdd.fitnesstracker.databinding.ItemExerciseBinding;
import com.btlltdd.fitnesstracker.model.Exercise;
import com.btlltdd.fitnesstracker.util.Labels;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ExerciseAdapter extends RecyclerView.Adapter<ExerciseAdapter.Holder> {
    public interface Listener {
        void onOpen(Exercise exercise);
        void onBookmark(Exercise exercise);
    }

    private final Listener listener;
    private List<Exercise> items = new ArrayList<>();

    public ExerciseAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<Exercise> data) {
        items = data;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemExerciseBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Exercise item = items.get(position);
        String letter = item.name.isEmpty() ? "?" : item.name.substring(0, 1).toUpperCase(Locale.US);
        holder.binding.tvAvatar.setText(letter);
        holder.binding.tvName.setText(item.name);
        holder.binding.tvMeta.setText(Labels.muscle(item.muscleGroup) + " · "
                + Labels.difficulty(item.difficulty) + " · " + Labels.equipment(item.equipment));
        holder.binding.btnSave.setText(item.bookmarked ? "Đã lưu" : "Lưu");
        holder.binding.getRoot().setOnClickListener(v -> listener.onOpen(item));
        holder.binding.btnSave.setOnClickListener(v -> listener.onBookmark(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemExerciseBinding binding;

        Holder(ItemExerciseBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
