package com.btlltdd.fitnesstracker.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.btlltdd.fitnesstracker.databinding.ItemHistoryBinding;
import com.btlltdd.fitnesstracker.model.WorkoutSession;
import com.btlltdd.fitnesstracker.util.TimeUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.Holder> {
    public interface Listener {
        void onOpen(WorkoutSession session);
    }

    private final Listener listener;
    private List<WorkoutSession> items = new ArrayList<>();

    public HistoryAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<WorkoutSession> data) {
        items = data;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemHistoryBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        WorkoutSession item = items.get(position);
        holder.binding.tvName.setText(item.title == null || item.title.isEmpty() ? "Buổi tập" : item.title);
        holder.binding.tvMeta.setText(TimeUtil.display(item.startTime)
                + " · " + TimeUtil.clock(item.durationSec)
                + " · " + item.steps + " bước · "
                + String.format(Locale.US, "%.2f km · %.0f kcal", item.distanceM / 1000.0, item.calories));
        if ("finished".equals(item.status)) {
            holder.binding.tvSync.setText(item.synced ? "Đã đồng bộ" : "Chưa đồng bộ");
        } else {
            holder.binding.tvSync.setText("Đang dở");
        }
        holder.binding.getRoot().setOnClickListener(v -> listener.onOpen(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemHistoryBinding binding;

        Holder(ItemHistoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
