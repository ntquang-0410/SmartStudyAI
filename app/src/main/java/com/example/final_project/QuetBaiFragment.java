package com.example.final_project;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QuetBaiFragment extends Fragment {

    // ── Camera ────────────────────────────────────────────────────────────────
    private PreviewView viewFinder;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;

    // ── ViewModel ─────────────────────────────────────────────────────────────
    private SolveViewModel viewModel;

    // ── Camera UI (existing) ──────────────────────────────────────────────────
    private View bottomSheetResult;
    private TextView tvOcrTitle;
    private FloatingActionButton btnUploadImage;
    private FloatingActionButton btnCaptureImage;
    private MaterialButton btnSendAi;

    // ── Result panel (new) ────────────────────────────────────────────────────
    private View layoutResult;
    private WebView webViewAnswer;
    private ImageView ivThumbnail;
    private Button btnCopy;
    private Button btnShare;
    private MaterialButton btnCameraNew;

    // ── State ─────────────────────────────────────────────────────────────────
    private Uri latestCapturedUri;
    private String rawAnswer;
    private String savedAnswerKey;

    // ── History ───────────────────────────────────────────────────────────────
    private final ScanHistoryRepository historyRepo = new ScanHistoryRepository();

    // ── Launchers ─────────────────────────────────────────────────────────────

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) startCamera();
                else Toast.makeText(requireContext(), "Cần cấp quyền camera để sử dụng", Toast.LENGTH_SHORT).show();
            });

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) {
                    Toast.makeText(requireContext(), "Bạn chưa chọn ảnh", Toast.LENGTH_SHORT).show();
                    return;
                }
                latestCapturedUri = uri;
                viewModel.solveFromUri(latestCapturedUri, "");
            });

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_quet_bai, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        cameraExecutor = Executors.newSingleThreadExecutor();
        viewModel = new ViewModelProvider(this).get(SolveViewModel.class);

        // Camera UI
        viewFinder        = view.findViewById(R.id.view_finder);
        bottomSheetResult = view.findViewById(R.id.bottom_sheet_result);
        tvOcrTitle        = view.findViewById(R.id.tv_ocr_title);
        btnUploadImage    = view.findViewById(R.id.btn_upload_image);
        btnCaptureImage   = view.findViewById(R.id.btn_capture_image);
        btnSendAi         = view.findViewById(R.id.btn_send_ai);

        // Result panel
        layoutResult   = view.findViewById(R.id.layoutResult);
        webViewAnswer  = view.findViewById(R.id.webViewAnswer);
        ivThumbnail    = view.findViewById(R.id.ivThumbnail);
        btnCopy        = view.findViewById(R.id.btnCopy);
        btnShare       = view.findViewById(R.id.btnShare);
        btnCameraNew   = view.findViewById(R.id.btnCameraNew);

        setupWebView();

        btnUploadImage.setOnClickListener(v -> openGallery());
        btnCaptureImage.setOnClickListener(v -> captureAndSolve());
        btnSendAi.setOnClickListener(v -> retrySolve());
        btnCopy.setOnClickListener(v -> copyToClipboard());
        btnShare.setOnClickListener(v -> shareAnswer());
        btnCameraNew.setOnClickListener(v -> viewModel.reset());

        observeUiState();

        if (allPermissionsGranted()) startCamera();
        else requestPermissionLauncher.launch(Manifest.permission.CAMERA);
    }

    @Override
    public void onDestroyView() {
        if (webViewAnswer != null) {
            webViewAnswer.stopLoading();
            webViewAnswer.destroy();
        }
        super.onDestroyView();
        if (cameraExecutor != null) cameraExecutor.shutdown();
    }

    // ── WebView setup ─────────────────────────────────────────────────────────

    private void setupWebView() {
        WebSettings ws = webViewAnswer.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setSupportZoom(false);
        ws.setBuiltInZoomControls(false);
        ws.setDisplayZoomControls(false);
        webViewAnswer.setScrollBarStyle(WebView.SCROLLBARS_INSIDE_OVERLAY);
        webViewAnswer.setBackgroundColor(0xFFFFFFFF);
    }

    // ── State rendering ───────────────────────────────────────────────────────

    private void observeUiState() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), this::renderState);
    }

    private void renderState(SolveUiState state) {
        if      (state instanceof SolveUiState.Idle)    renderIdle();
        else if (state instanceof SolveUiState.Loading) renderLoading();
        else if (state instanceof SolveUiState.Success) renderSuccess(((SolveUiState.Success) state).getAnswer());
        else if (state instanceof SolveUiState.Error)   renderError(((SolveUiState.Error) state).getMessage());
    }

    private void renderIdle() {
        layoutResult.setVisibility(View.GONE);
        bottomSheetResult.setVisibility(View.GONE);
        btnUploadImage.setEnabled(true);
        btnCaptureImage.setEnabled(true);
        btnSendAi.setEnabled(latestCapturedUri != null);
    }

    private void renderLoading() {
        layoutResult.setVisibility(View.GONE);
        bottomSheetResult.setVisibility(View.VISIBLE);
        tvOcrTitle.setText("AI đang phân tích bài...");
        btnUploadImage.setEnabled(false);
        btnCaptureImage.setEnabled(false);
        btnSendAi.setEnabled(false);
    }

    private void renderSuccess(String answer) {
        rawAnswer = answer;
        bottomSheetResult.setVisibility(View.GONE);
        layoutResult.setVisibility(View.VISIBLE);

        if (latestCapturedUri != null) {
            ivThumbnail.setImageURI(latestCapturedUri);
        }

        webViewAnswer.loadDataWithBaseURL(
                "https://cdn.jsdelivr.net",
                MarkdownToHtml.toHtml(answer),
                "text/html",
                "UTF-8",
                null
        );

        saveToHistoryIfNeeded(answer);
    }

    private void renderError(String message) {
        layoutResult.setVisibility(View.GONE);
        bottomSheetResult.setVisibility(View.GONE);
        btnUploadImage.setEnabled(true);
        btnCaptureImage.setEnabled(true);
        btnSendAi.setEnabled(latestCapturedUri != null);
        Toast.makeText(requireContext(), "Lỗi: " + message, Toast.LENGTH_LONG).show();
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void openGallery() {
        pickImageLauncher.launch("image/*");
    }

    private void retrySolve() {
        if (latestCapturedUri == null) {
            Toast.makeText(requireContext(), "Hãy chụp ảnh trước", Toast.LENGTH_SHORT).show();
            return;
        }
        viewModel.solveFromUri(latestCapturedUri, "");
    }

    private void copyToClipboard() {
        if (rawAnswer == null || rawAnswer.isEmpty()) return;
        ClipboardManager clipboard =
                (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("Lời giải", rawAnswer));
        Toast.makeText(requireContext(), "Đã chép vào clipboard", Toast.LENGTH_SHORT).show();
    }

    private void shareAnswer() {
        if (rawAnswer == null || rawAnswer.isEmpty()) return;
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, rawAnswer);
        startActivity(Intent.createChooser(intent, "Chia sẻ lời giải"));
    }

    // ── Camera ────────────────────────────────────────────────────────────────

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(requireContext());

        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());
                imageCapture = new ImageCapture.Builder().build();
                provider.unbindAll();
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture);
            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(requireContext(), "Lỗi khởi tạo camera", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void captureAndSolve() {
        if (imageCapture == null) {
            Toast.makeText(requireContext(), "Camera chưa sẵn sàng", Toast.LENGTH_SHORT).show();
            return;
        }
        File photoFile = new File(
                requireContext().getCacheDir(),
                "scan_" + System.currentTimeMillis() + ".jpg"
        );
        ImageCapture.OutputFileOptions opts =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(opts, cameraExecutor,
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults r) {
                        latestCapturedUri = Uri.fromFile(photoFile);
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() ->
                                viewModel.solveFromUri(latestCapturedUri, ""));
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException e) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(), "Không chụp được ảnh", Toast.LENGTH_SHORT).show());
                    }
                });
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    // ── History persistence ───────────────────────────────────────────────────

    private void saveToHistoryIfNeeded(String answer) {
        if (latestCapturedUri == null || answer == null || answer.isEmpty()) return;

        // Idempotency: don't re-save the same answer if observer fires twice
        // (e.g., on config change with cached LiveData).
        String key = latestCapturedUri + "|" + answer.hashCode();
        if (key.equals(savedAnswerKey)) return;
        savedAnswerKey = key;

        if (historyRepo.currentUid() == null) return; // not signed in, skip silently

        Uri uri = latestCapturedUri;
        cameraExecutor.execute(() -> {
            byte[] bytes = readBytes(uri);
            if (bytes == null) return;
            historyRepo.save(bytes, answer)
                    .addOnSuccessListener(item -> {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(),
                                "Đã lưu vào lịch sử", Toast.LENGTH_SHORT).show();
                        if (item.getId() != null) {
                            SubjectClassifier.classifyAsync(answer, subject ->
                                    historyRepo.updateSubject(item.getId(), subject));
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(),
                                "Lưu lịch sử thất bại: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
        });
    }

    @Nullable
    private byte[] readBytes(Uri uri) {
        try (InputStream in = requireContext().getContentResolver().openInputStream(uri)) {
            if (in == null) return null;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8 * 1024];
            int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            return out.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }
}
