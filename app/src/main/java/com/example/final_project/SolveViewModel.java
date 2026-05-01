package com.example.final_project;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.final_project.GeminiResult;
import com.example.final_project.GeminiVisionService;

import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SolveViewModel.java
 * ─────────────────────────────────────────────────────────────────────
 * Tách logic AI ra khỏi Fragment/Activity.
 * Dùng ExecutorService (background) + LiveData (main thread) thay cho
 * Kotlin coroutines + StateFlow.
 *
 * Fragment chỉ cần:
 *   viewModel.getUiState().observe(getViewLifecycleOwner(), state -> { ... });
 * ─────────────────────────────────────────────────────────────────────
 */
public class SolveViewModel extends AndroidViewModel {

    // ── Fields ────────────────────────────────────────────────────────────────

    private final GeminiVisionService geminiService;

    /** Single-thread executor — tránh gọi API song song. */
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<SolveUiState> _uiState =
            new MutableLiveData<>(new SolveUiState.Idle());

    // ── Constructor ───────────────────────────────────────────────────────────

    public SolveViewModel(@NonNull Application application) {
        super(application);
        geminiService = new GeminiVisionService(application);
    }

    // ── Public LiveData ───────────────────────────────────────────────────────

    /** Fragment observe LiveData này để cập nhật UI. */
    public LiveData<SolveUiState> getUiState() {
        return _uiState;
    }

    // ── Public actions ────────────────────────────────────────────────────────

    /**
     * Gọi khi người dùng chụp / chọn ảnh từ gallery.
     *
     * @param uri       Uri của ảnh
     * @param extraText Câu hỏi bổ sung (có thể để rỗng "")
     */
    public void solveFromUri(Uri uri, String extraText) {
        _uiState.setValue(new SolveUiState.Loading());

        executor.execute(() -> {
            Bitmap bitmap = decodeUri(uri);
            if (bitmap == null) {
                _uiState.postValue(new SolveUiState.Error("Không đọc được ảnh"));
                return;
            }
            GeminiResult result = geminiService.solveFromBitmap(bitmap, extraText);
            postResult(result);
        });
    }

    private Bitmap decodeUri(Uri uri) {
        try (InputStream in = getApplication().getContentResolver().openInputStream(uri)) {
            return in == null ? null : BitmapFactory.decodeStream(in);
        } catch (Exception e) {
            Log.e("SolveViewModel", "decodeUri failed", e);
            return null;
        }
    }

    /**
     * Gọi khi ảnh đến từ CameraX preview (Bitmap trực tiếp).
     */
    public void solveFromBitmap(Bitmap bitmap, String extraText) {
        _uiState.setValue(new SolveUiState.Loading());

        executor.execute(() -> {
            GeminiResult result = geminiService.solveFromBitmap(bitmap, extraText);
            postResult(result);
        });
    }

    /** Reset về trạng thái ban đầu (người dùng bấm "Hỏi câu khác"). */
    public void reset() {
        _uiState.setValue(new SolveUiState.Idle());
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    /** Chuyển GeminiResult → SolveUiState rồi post về main thread. */
    private void postResult(GeminiResult result) {
        SolveUiState next;
        if (result instanceof GeminiResult.Success) {
            next = new SolveUiState.Success(((GeminiResult.Success) result).getAnswer());
        } else {
            next = new SolveUiState.Error(((GeminiResult.Error) result).getMessage());
        }
        // postValue an toàn khi gọi từ background thread
        _uiState.postValue(next);
    }

    // ── Cleanup ───────────────────────────────────────────────────────────────

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdownNow();
    }
}