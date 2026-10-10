package com.btlltdd.fitnesstracker.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.btlltdd.fitnesstracker.databinding.ItemAdminExerciseBinding;
import com.btlltdd.fitnesstracker.model.Exercise;
import com.btlltdd.fitnesstracker.util.Labels;

import java.util.ArrayList;
import java.util.List;

public class AdminExerciseAdapter extends RecyclerView.Adapter<AdminExerciseAdapter.Holder> {
    public interface Listener {
        void onEdit(Exercise exercise);
        void onHide(Exercise exercise);
        void onDelete(Exercise exercise);
    }

    private final Listener listener;
    private List<Exercise> items = new ArrayList<>();

    public AdminExerciseAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<Exercise> data) {
        items = data;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemAdminExerciseBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Exercise item = items.get(position);
        holder.binding.tvName.setText(item.name);
        String meta = Labels.muscle(item.muscleGroup) + " · " + Labels.difficulty(item.difficulty)
                + " · MET " + item.met;
        if (item.hidden) {
            meta += " · Đang ẩn";
        }
        holder.binding.tvMeta.setText(meta);
        holder.binding.btnHide.setText(item.hidden ? "Hiện" : "Ẩn");
        holder.binding.btnEdit.setOnClickListener(v -> listener.onEdit(item));
        holder.binding.btnHide.setOnClickListener(v -> listener.onHide(item));
        holder.binding.btnDelete.setOnClickListener(v -> listener.onDelete(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemAdminExerciseBinding binding;

        Holder(ItemAdminExerciseBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
