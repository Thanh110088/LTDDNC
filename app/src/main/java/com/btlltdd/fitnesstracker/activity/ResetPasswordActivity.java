package com.btlltdd.fitnesstracker.activity;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.btlltdd.fitnesstracker.database.UserDao;
import com.btlltdd.fitnesstracker.databinding.ActivityResetPasswordBinding;

public class ResetPasswordActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityResetPasswordBinding binding = ActivityResetPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.btnSave.setOnClickListener(v -> {
            String error = new UserDao(this).resetPassword(text(binding.edtUsername), text(binding.edtPassword));
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, "Đã đổi mật khẩu", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private String text(android.widget.EditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString();
    }
}
