package com.btlltdd.fitnesstracker.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.btlltdd.fitnesstracker.database.UserDao;
import com.btlltdd.fitnesstracker.databinding.ActivityLoginBinding;
import com.btlltdd.fitnesstracker.util.SessionManager;

public class LoginActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SessionManager session = new SessionManager(this);
        if (session.isLoggedIn()) {
            openMain();
            return;
        }
        ActivityLoginBinding binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.btnLogin.setOnClickListener(v -> {
            UserDao.Result result = new UserDao(this).login(
                    text(binding.edtUsername),
                    text(binding.edtPassword));
            if (result.user == null) {
                Toast.makeText(this, result.error, Toast.LENGTH_SHORT).show();
                return;
            }
            session.save(result.user);
            openMain();
        });
        binding.btnRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
        binding.btnReset.setOnClickListener(v ->
                startActivity(new Intent(this, ResetPasswordActivity.class)));
    }

    private void openMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private String text(android.widget.EditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString();
    }
}
