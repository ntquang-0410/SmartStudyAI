package com.example.final_project;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.view.MotionEvent;
import android.text.method.PasswordTransformationMethod;
import android.text.method.HideReturnsTransformationMethod;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import android.view.View;

public class LoginActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        EditText etEmail = findViewById(R.id.etEmail);
        EditText etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        TextView tvEmailError = findViewById(R.id.tvEmailError);
        TextView tvPasswordError = findViewById(R.id.tvPasswordError);

        TextView tvSignUpLink = findViewById(R.id.tvSignUpLink);
        tvSignUpLink.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });

        // Toggle password visibility
        etPassword.setOnTouchListener((v, event) -> {
            final int DRAWABLE_RIGHT = 2;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (etPassword.getCompoundDrawables()[DRAWABLE_RIGHT] != null) {
                    if (event.getRawX() >= (etPassword.getRight() - etPassword.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width() - etPassword.getPaddingEnd())) {
                        int selection = etPassword.getSelectionEnd();
                        if (etPassword.getTransformationMethod() == PasswordTransformationMethod.getInstance()) {
                            etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                            etPassword.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_eye_off, 0);
                        } else {
                            etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                            etPassword.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_eye, 0);
                        }
                        etPassword.setSelection(selection);
                        return true;
                    }
                }
            }
            return false;
        });

        // Real-time validation on focus lost
        etEmail.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String email = etEmail.getText().toString().trim();
                if (!email.isEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    etEmail.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_check_green, 0);
                    tvEmailError.setVisibility(View.GONE);
                } else if (!email.isEmpty()) {
                    etEmail.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
                    tvEmailError.setText("Email không đúng định dạng");
                    tvEmailError.setVisibility(View.VISIBLE);
                }
            }
        });

        etPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String pass = etPassword.getText().toString().trim();
                if (!pass.isEmpty() && pass.length() < 8) {
                    tvPasswordError.setText("Mật khẩu phải từ 8 ký tự, có Hoa, thường, số, ký tự đặc biệt");
                    tvPasswordError.setVisibility(View.VISIBLE);
                } else if (!pass.isEmpty() && !pass.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^])[A-Za-z\\d@$!%*?&#^]{8,}$")) {
                    tvPasswordError.setText("Mật khẩu chưa đủ mạnh. Mật khẩu phải chứa ít nhất 1 chữ hoa, 1 chữ thường, 1 số và 1 ký tự đặc biệt.");
                    tvPasswordError.setVisibility(View.VISIBLE);
                } else {
                    tvPasswordError.setVisibility(View.GONE);
                }
            }
        });

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            boolean isValid = true;

            if (email.isEmpty()) {
                tvEmailError.setText("Vui lòng nhập email");
                tvEmailError.setVisibility(View.VISIBLE);
                isValid = false;
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tvEmailError.setText("Email không đúng định dạng");
                tvEmailError.setVisibility(View.VISIBLE);
                isValid = false;
            } else {
                tvEmailError.setVisibility(View.GONE);
            }

            if (password.isEmpty()) {
                tvPasswordError.setText("Vui lòng nhập mật khẩu");
                tvPasswordError.setVisibility(View.VISIBLE);
                isValid = false;
            } else {
                tvPasswordError.setVisibility(View.GONE);
            }

            if (!isValid) return;

            mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finishAffinity();
                    } else {
                        Toast.makeText(LoginActivity.this, "Sai email hoặc mật khẩu!", Toast.LENGTH_SHORT).show();
                    }
                });
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finishAffinity();
        }
    }
}
