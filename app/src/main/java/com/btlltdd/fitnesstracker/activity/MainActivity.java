package com.btlltdd.fitnesstracker.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.btlltdd.fitnesstracker.R;
import com.btlltdd.fitnesstracker.databinding.ActivityMainBinding;
import com.btlltdd.fitnesstracker.fragment.AdminFragment;
import com.btlltdd.fitnesstracker.fragment.ExerciseFragment;
import com.btlltdd.fitnesstracker.fragment.HomeFragment;
import com.btlltdd.fitnesstracker.fragment.ProfileFragment;
import com.btlltdd.fitnesstracker.fragment.WorkoutFragment;
import com.btlltdd.fitnesstracker.network.SyncManager;
import com.btlltdd.fitnesstracker.receiver.NetworkSyncReceiver;
import com.btlltdd.fitnesstracker.util.SessionManager;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!new SessionManager(this).isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.bottomNav.getMenu().findItem(R.id.nav_admin).setVisible(new SessionManager(this).isAdmin());
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_exercise) {
                show(new ExerciseFragment());
            } else if (id == R.id.nav_workout) {
                show(new WorkoutFragment());
            } else if (id == R.id.nav_profile) {
                show(new ProfileFragment());
            } else if (id == R.id.nav_admin) {
                show(new AdminFragment());
            } else {
                show(new HomeFragment());
            }
            return true;
        });
        if (savedInstanceState == null) {
            binding.bottomNav.setSelectedItemId(R.id.nav_home);
        }
        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 31);
        }
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (binding.bottomNav.getSelectedItemId() != R.id.nav_home) {
                    binding.bottomNav.setSelectedItemId(R.id.nav_home);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (new SessionManager(this).isLoggedIn() && NetworkSyncReceiver.isOnline(this)) {
            SyncManager.sync(this, null);
        }
    }

    private void show(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
