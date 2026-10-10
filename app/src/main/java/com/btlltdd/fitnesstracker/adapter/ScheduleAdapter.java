package com.btlltdd.fitnesstracker.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.btlltdd.fitnesstracker.databinding.ItemScheduleBinding;
import com.btlltdd.fitnesstracker.model.WorkoutSchedule;
import com.btlltdd.fitnesstracker.util.Labels;
import com.btlltdd.fitnesstracker.util.TimeUtil;

import java.util.ArrayList;
import java.util.List;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.Holder> {
    public interface Listener {
        void onToggle(WorkoutSchedule schedule, boolean enabled);
        void onDelete(WorkoutSchedule schedule);
    }

    private final Listener listener;
    private List<WorkoutSchedule> items = new ArrayList<>();

    public ScheduleAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<WorkoutSchedule> data) {
        items = data;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemScheduleBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        WorkoutSchedule item = items.get(position);
        holder.binding.tvName.setText(item.title);
        holder.binding.tvMeta.setText(TimeUtil.display(item.scheduleTime) + " · " + Labels.repeat(item.repeatType));
        holder.binding.switchOn.setOnCheckedChangeListener(null);
        holder.binding.switchOn.setChecked(item.enabled);
        holder.binding.switchOn.setOnCheckedChangeListener((button, checked) -> listener.onToggle(item, checked));
        holder.binding.btnDelete.setOnClickListener(v -> listener.onDelete(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemScheduleBinding binding;

        Holder(ItemScheduleBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
