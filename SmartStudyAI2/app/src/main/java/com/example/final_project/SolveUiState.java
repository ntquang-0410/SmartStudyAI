package com.example.final_project;

/**
 * SolveUiState.java
 * ─────────────────────────────────────────────────────────────────────
 * Thay thế sealed class Kotlin bằng abstract class + 4 inner states.
 *
 * Trong Fragment, phân nhánh bằng instanceof:
 *
 *   viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
 *       if      (state instanceof SolveUiState.Idle)    renderIdle();
 *       else if (state instanceof SolveUiState.Loading) renderLoading();
 *       else if (state instanceof SolveUiState.Success) renderSuccess(((SolveUiState.Success) state).getAnswer());
 *       else if (state instanceof SolveUiState.Error)   renderError(((SolveUiState.Error) state).getMessage());
 *   });
 * ─────────────────────────────────────────────────────────────────────
 */
public abstract class SolveUiState {

    private SolveUiState() {}

    // ── States ────────────────────────────────────────────────────────────────

    /** Chưa làm gì — màn hình ban đầu. */
    public static final class Idle extends SolveUiState {
        public Idle() {}
    }

    /** Đang gọi Gemini API — hiển thị ProgressBar. */
    public static final class Loading extends SolveUiState {
        public Loading() {}
    }

    /** Gemini trả về lời giải thành công. */
    public static final class Success extends SolveUiState {
        private final String answer;

        public Success(String answer) {
            this.answer = answer;
        }

        public String getAnswer() {
            return answer;
        }
    }

    /** Có lỗi xảy ra — hiển thị Toast. */
    public static final class Error extends SolveUiState {
        private final String message;

        public Error(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}