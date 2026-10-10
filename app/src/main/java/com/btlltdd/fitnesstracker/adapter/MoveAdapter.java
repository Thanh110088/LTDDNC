package com.btlltdd.fitnesstracker.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.btlltdd.fitnesstracker.databinding.ItemMoveBinding;
import com.btlltdd.fitnesstracker.model.PlanExercise;

import java.util.ArrayList;
import java.util.List;

public class MoveAdapter extends RecyclerView.Adapter<MoveAdapter.Holder> {
    public interface Listener {
        void onDelete(PlanExercise item);
    }

    private final Listener listener;
    private List<PlanExercise> items = new ArrayList<>();

    public MoveAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<PlanExercise> data) {
        items = data;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemMoveBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        PlanExercise item = items.get(position);
        holder.binding.tvName.setText(item.exerciseName);
        String meta = item.sets + " hiệp × " + item.reps + " lần";
        if (item.durationSec > 0) {
            meta += " · " + item.durationSec + " giây";
        }
        if (item.weekday > 0) {
            meta += " · Ngày " + item.weekday;
        }
        holder.binding.tvMeta.setText(meta);
        holder.binding.btnDelete.setOnClickListener(v -> listener.onDelete(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemMoveBinding binding;

        Holder(ItemMoveBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
