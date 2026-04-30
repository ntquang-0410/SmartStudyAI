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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import android.view.View;

public class RegisterActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        EditText etName = findViewById(R.id.etName);
        EditText etPhone = findViewById(R.id.etPhone);
        EditText etEmail = findViewById(R.id.etEmail);
        EditText etPassword = findViewById(R.id.etPassword);

        TextView tvNameError = findViewById(R.id.tvNameError);
        TextView tvPhoneError = findViewById(R.id.tvPhoneError);
        TextView tvEmailError = findViewById(R.id.tvEmailError);
        TextView tvPasswordError = findViewById(R.id.tvPasswordError);

        TextView tvSignInLink = findViewById(R.id.tvSignInLink);
        tvSignInLink.setOnClickListener(v -> finish());

        // Setup real-time validations
        etName.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String name = etName.getText().toString().trim();
                if (!name.isEmpty()) {
                    etName.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_check_green, 0);
                    tvNameError.setVisibility(View.GONE);
                } else {
                    etName.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
                    tvNameError.setText("Vui lòng nhập họ tên");
                    tvNameError.setVisibility(View.VISIBLE);
                }
            }
        });

        etPhone.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String phone = etPhone.getText().toString().trim();
                if (phone.matches("^[0-9]{9,11}$")) {
                    etPhone.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_check_green, 0);
                    tvPhoneError.setVisibility(View.GONE);
                } else if (!phone.isEmpty()) {
                    etPhone.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
                    tvPhoneError.setText("Số điện thoại không hợp lệ (9-11 số)");
                    tvPhoneError.setVisibility(View.VISIBLE);
                } else {
                    tvPhoneError.setText("Vui lòng nhập số điện thoại");
                    tvPhoneError.setVisibility(View.VISIBLE);
                }
            }
        });

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
                } else {
                    tvEmailError.setText("Vui lòng nhập email");
                    tvEmailError.setVisibility(View.VISIBLE);
                }
            }
        });

        etPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String pass = etPassword.getText().toString().trim();
                if (pass.isEmpty()) {
                    tvPasswordError.setText("Vui lòng nhập mật khẩu");
                    tvPasswordError.setVisibility(View.VISIBLE);
                } else if (pass.length() < 8) {
                    tvPasswordError.setText("Mật khẩu phải từ 8 ký tự");
                    tvPasswordError.setVisibility(View.VISIBLE);
                } else if (!pass.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^])[A-Za-z\\d@$!%*?&#^]{8,}$")) {
                    tvPasswordError.setText("Mật khẩu chưa đủ mạnh. (Cần ít nhất 1 chữ hoa, 1 chữ thường, 1 số và 1 ký tự đặc biệt)");
                    tvPasswordError.setVisibility(View.VISIBLE);
                } else {
                    tvPasswordError.setVisibility(View.GONE);
                }
            }
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

        Button btnSignUp = findViewById(R.id.btnSignUp);
        btnSignUp.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String name = etName.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();

            boolean isValid = true;

            if (name.isEmpty()) {
                tvNameError.setText("Vui lòng nhập họ tên");
                tvNameError.setVisibility(View.VISIBLE);
                isValid = false;
            } else tvNameError.setVisibility(View.GONE);

            if (phone.isEmpty()) {
                tvPhoneError.setText("Vui lòng nhập số điện thoại");
                tvPhoneError.setVisibility(View.VISIBLE);
                isValid = false;
            } else if (!phone.matches("^[0-9]{9,11}$")) {
                tvPhoneError.setText("Số điện thoại không hợp lệ (9-11 số)");
                tvPhoneError.setVisibility(View.VISIBLE);
                isValid = false;
            } else tvPhoneError.setVisibility(View.GONE);

            if (email.isEmpty()) {
                tvEmailError.setText("Vui lòng nhập email");
                tvEmailError.setVisibility(View.VISIBLE);
                isValid = false;
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tvEmailError.setText("Email không đúng định dạng");
                tvEmailError.setVisibility(View.VISIBLE);
                isValid = false;
            } else tvEmailError.setVisibility(View.GONE);

            if (password.isEmpty()) {
                tvPasswordError.setText("Vui lòng nhập mật khẩu");
                tvPasswordError.setVisibility(View.VISIBLE);
                isValid = false;
            } else if (password.length() < 8) {
                tvPasswordError.setText("Mật khẩu phải từ 8 ký tự");
                tvPasswordError.setVisibility(View.VISIBLE);
                isValid = false;
            } else if (!password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^])[A-Za-z\\d@$!%*?&#^]{8,}$")) {
                tvPasswordError.setText("Mật khẩu chưa đủ mạnh. (Cần ít nhất 1 chữ hoa, 1 chữ thường, 1 số và 1 ký tự đặc biệt)");
                tvPasswordError.setVisibility(View.VISIBLE);
                isValid = false;
            } else tvPasswordError.setVisibility(View.GONE);

            if (!isValid) return;

            mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            User newUser = new User(name, email, phone);
                            mDatabase.child("users").child(user.getUid()).setValue(newUser)
                                .addOnCompleteListener(dbTask -> {
                                    if (dbTask.isSuccessful()) {
                                        Toast.makeText(RegisterActivity.this, "Đăng ký thành công", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                                        finishAffinity();
                                    } else {
                                        Toast.makeText(RegisterActivity.this, "Lưu thông tin thất bại", Toast.LENGTH_SHORT).show();
                                    }
                                });
                        }
                    } else {
                        Toast.makeText(this, "Đăng ký thất bại: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }
}
