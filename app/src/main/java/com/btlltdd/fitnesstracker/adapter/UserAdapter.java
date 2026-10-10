package com.btlltdd.fitnesstracker.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.btlltdd.fitnesstracker.databinding.ItemUserBinding;
import com.btlltdd.fitnesstracker.model.User;
import com.btlltdd.fitnesstracker.util.Labels;

import java.util.ArrayList;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.Holder> {
    public interface Listener {
        void onToggle(User user, boolean active);
    }

    private final Listener listener;
    private final long currentUserId;
    private List<User> items = new ArrayList<>();

    public UserAdapter(long currentUserId, Listener listener) {
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    public void submit(List<User> data) {
        items = data;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemUserBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        User user = items.get(position);
        holder.binding.tvName.setText(user.fullName);
        holder.binding.tvMeta.setText(user.username + " · " + Labels.role(user));
        holder.binding.switchActive.setOnCheckedChangeListener(null);
        holder.binding.switchActive.setChecked(user.active);
        holder.binding.switchActive.setEnabled(user.id != currentUserId);
        holder.binding.switchActive.setOnCheckedChangeListener((button, checked) -> listener.onToggle(user, checked));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemUserBinding binding;

        Holder(ItemUserBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
