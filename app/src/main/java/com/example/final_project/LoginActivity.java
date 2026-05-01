package com.example.final_project;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.final_project.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private AuthViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        binding.btnLogin.setOnClickListener(v -> attemptLogin());
        binding.tvGoRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
        binding.tvForgot.setOnClickListener(v -> attemptPasswordReset());

        viewModel.getUiState().observe(this, this::render);
    }

    private void attemptLogin() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString();

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.setError("Email không hợp lệ");
            binding.etEmail.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            binding.etPassword.setError("Mật khẩu tối thiểu 6 ký tự");
            binding.etPassword.requestFocus();
            return;
        }
        viewModel.login(email, password);
    }

    private void attemptPasswordReset() {
        String email = binding.etEmail.getText().toString().trim();
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.setError("Nhập email để đặt lại mật khẩu");
            binding.etEmail.requestFocus();
            return;
        }
        viewModel.sendPasswordReset(email);
    }

    private void render(AuthUiState state) {
        boolean loading = state instanceof AuthUiState.Loading;
        binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnLogin.setEnabled(!loading);

        if (state instanceof AuthUiState.Authenticated) {
            viewModel.consume();
            Toast.makeText(this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else if (state instanceof AuthUiState.ResetEmailSent) {
            viewModel.consume();
            Toast.makeText(this, "Email đặt lại mật khẩu đã được gửi", Toast.LENGTH_LONG).show();
        } else if (state instanceof AuthUiState.Error) {
            viewModel.consume();
            Toast.makeText(this, ((AuthUiState.Error) state).getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
