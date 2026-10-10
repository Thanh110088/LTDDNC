package com.btlltdd.fitnesstracker.activity;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.btlltdd.fitnesstracker.database.UserDao;
import com.btlltdd.fitnesstracker.databinding.ActivityRegisterBinding;

public class RegisterActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityRegisterBinding binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.btnRegister.setOnClickListener(v -> {
            String error = new UserDao(this).register(
                    text(binding.edtUsername),
                    text(binding.edtPassword),
                    text(binding.edtFullname));
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, "Đã tạo tài khoản. Hãy đăng nhập.", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private String text(android.widget.EditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString();
    }
}
