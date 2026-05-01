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

import com.example.final_project.databinding.ActivityRegisterBinding;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private AuthViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        binding.btnRegister.setOnClickListener(v -> attemptRegister());
        binding.tvGoLogin.setOnClickListener(v -> finish());

        viewModel.getUiState().observe(this, this::render);
    }

    private void attemptRegister() {
        String name = binding.etName.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString();
        String confirm = binding.etConfirm.getText().toString();

        if (TextUtils.isEmpty(name)) {
            binding.etName.setError("Vui lòng nhập họ tên");
            binding.etName.requestFocus();
            return;
        }
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
        if (!password.equals(confirm)) {
            binding.etConfirm.setError("Mật khẩu không trùng khớp");
            binding.etConfirm.requestFocus();
            return;
        }
        viewModel.register(name, email, password);
    }

    private void render(AuthUiState state) {
        boolean loading = state instanceof AuthUiState.Loading;
        binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnRegister.setEnabled(!loading);

        if (state instanceof AuthUiState.Authenticated) {
            viewModel.consume();
            Toast.makeText(this, "Đăng ký thành công", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else if (state instanceof AuthUiState.Error) {
            viewModel.consume();
            Toast.makeText(this, ((AuthUiState.Error) state).getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
