package com.btlltdd.fitnesstracker.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.btlltdd.fitnesstracker.databinding.ItemPlanBinding;
import com.btlltdd.fitnesstracker.model.WorkoutPlan;
import com.btlltdd.fitnesstracker.util.Labels;

import java.util.ArrayList;
import java.util.List;

public class PlanAdapter extends RecyclerView.Adapter<PlanAdapter.Holder> {
    public static final int MINE = 1;
    public static final int SUGGEST = 2;
    public static final int ADMIN = 3;

    public interface Listener {
        void onPrimary(WorkoutPlan plan);
        void onSecondary(WorkoutPlan plan);
        void onDelete(WorkoutPlan plan);
    }

    private final Listener listener;
    private List<WorkoutPlan> items = new ArrayList<>();
    private int mode = MINE;

    public PlanAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<WorkoutPlan> data, int mode) {
        this.items = data;
        this.mode = mode;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemPlanBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        WorkoutPlan plan = items.get(position);
        holder.binding.tvName.setText(plan.name);
        String meta = Labels.goal(plan.goal);
        if (plan.template) {
            meta += " · BMI " + trim(plan.bmiMin) + "–" + trim(plan.bmiMax);
        }
        if (plan.hidden) {
            meta += " · Đang ẩn";
        }
        holder.binding.tvMeta.setText(meta);
        holder.binding.tvDesc.setText(plan.description);
        if (mode == SUGGEST) {
            holder.binding.btnPrimary.setText("Tập");
            holder.binding.btnSecondary.setText("Dùng mẫu");
            holder.binding.btnDelete.setVisibility(View.GONE);
        } else if (mode == ADMIN) {
            holder.binding.btnPrimary.setText("Sửa");
            holder.binding.btnSecondary.setText(plan.hidden ? "Hiện" : "Ẩn");
            holder.binding.btnDelete.setVisibility(View.VISIBLE);
        } else {
            holder.binding.btnPrimary.setText("Tập");
            holder.binding.btnSecondary.setText("Sửa");
            holder.binding.btnDelete.setVisibility(View.VISIBLE);
        }
        holder.binding.btnPrimary.setOnClickListener(v -> listener.onPrimary(plan));
        holder.binding.btnSecondary.setOnClickListener(v -> listener.onSecondary(plan));
        holder.binding.btnDelete.setOnClickListener(v -> listener.onDelete(plan));
    }

    private String trim(double value) {
        if (value == (long) value) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemPlanBinding binding;

        Holder(ItemPlanBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
