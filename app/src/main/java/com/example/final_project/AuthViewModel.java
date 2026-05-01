package com.example.final_project;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/**
 * ViewModel cho Login / Register / ForgotPassword.
 * View chỉ observe getUiState() và gọi login/register/sendPasswordReset.
 */
public class AuthViewModel extends ViewModel {

    private final AuthRepository repo;

    private final MutableLiveData<AuthUiState> _uiState =
            new MutableLiveData<>(new AuthUiState.Idle());

    public AuthViewModel() {
        this(new AuthRepository());
    }

    AuthViewModel(@NonNull AuthRepository repo) {
        this.repo = repo;
    }

    public LiveData<AuthUiState> getUiState() {
        return _uiState;
    }

    public void login(@NonNull String email, @NonNull String password) {
        _uiState.setValue(new AuthUiState.Loading());
        repo.login(email, password)
                .addOnSuccessListener(r -> _uiState.setValue(new AuthUiState.Authenticated()))
                .addOnFailureListener(e -> _uiState.setValue(
                        new AuthUiState.Error(message(e, "Đăng nhập thất bại"))));
    }

    public void register(@NonNull String name,
                         @NonNull String email,
                         @NonNull String password) {
        _uiState.setValue(new AuthUiState.Loading());
        repo.register(name, email, password)
                .addOnSuccessListener(v -> _uiState.setValue(new AuthUiState.Authenticated()))
                .addOnFailureListener(e -> _uiState.setValue(
                        new AuthUiState.Error(message(e, "Đăng ký thất bại"))));
    }

    public void sendPasswordReset(@NonNull String email) {
        _uiState.setValue(new AuthUiState.Loading());
        repo.sendPasswordReset(email)
                .addOnSuccessListener(v -> _uiState.setValue(new AuthUiState.ResetEmailSent()))
                .addOnFailureListener(e -> _uiState.setValue(
                        new AuthUiState.Error(message(e, "Không gửi được email"))));
    }

    /** Gọi sau khi View đã consume một state đặc biệt (Error / ResetEmailSent / Authenticated). */
    public void consume() {
        _uiState.setValue(new AuthUiState.Idle());
    }

    private static String message(Exception e, String fallback) {
        return (e != null && e.getMessage() != null) ? e.getMessage() : fallback;
    }
}
