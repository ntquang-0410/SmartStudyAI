package com.example.final_project;

/**
 * UI state for LoginActivity / RegisterActivity.
 * Use instanceof to dispatch in observers.
 */
public abstract class AuthUiState {

    private AuthUiState() {}

    public static final class Idle extends AuthUiState {}

    public static final class Loading extends AuthUiState {}

    /** Login or register completed — caller should navigate to MainActivity. */
    public static final class Authenticated extends AuthUiState {}

    /** Password reset email sent successfully. */
    public static final class ResetEmailSent extends AuthUiState {}

    public static final class Error extends AuthUiState {
        private final String message;
        public Error(String message) { this.message = message; }
        public String getMessage() { return message; }
    }
}
