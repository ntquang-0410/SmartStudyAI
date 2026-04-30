package com.example.final_project;

import android.Manifest;
import android.net.Uri;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QuetBaiFragment extends Fragment {

    private PreviewView viewFinder;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private SolveViewModel viewModel;

    private View bottomSheetResult;
    private TextView tvOcrTitle;
    private EditText edtOcrResult;
    private FloatingActionButton btnUploadImage;
    private FloatingActionButton btnCaptureImage;
    private MaterialButton btnSendAi;

    private Uri latestCapturedUri;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startCamera();
                } else {
                    Toast.makeText(requireContext(), "Cần cấp quyền camera để sử dụng", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) {
                    Toast.makeText(requireContext(), "Ban chua chon anh", Toast.LENGTH_SHORT).show();
                    return;
                }
                latestCapturedUri = uri;
                String extraText = edtOcrResult.getText() != null
                        ? edtOcrResult.getText().toString().trim()
                        : "";
                viewModel.solveFromUri(latestCapturedUri, extraText);
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_quet_bai, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewFinder = view.findViewById(R.id.view_finder);
        cameraExecutor = Executors.newSingleThreadExecutor();
        viewModel = new ViewModelProvider(this).get(SolveViewModel.class);

        bottomSheetResult = view.findViewById(R.id.bottom_sheet_result);
        tvOcrTitle = view.findViewById(R.id.tv_ocr_title);
        edtOcrResult = view.findViewById(R.id.edt_ocr_result);
        btnUploadImage = view.findViewById(R.id.btn_upload_image);
        btnCaptureImage = view.findViewById(R.id.btn_capture_image);
        btnSendAi = view.findViewById(R.id.btn_send_ai);

        btnUploadImage.setOnClickListener(v -> openGallery());
        btnCaptureImage.setOnClickListener(v -> captureAndSolve());
        btnSendAi.setOnClickListener(v -> retrySolveWithExtraText());
        observeUiState();

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void observeUiState() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), this::renderState);
    }

    private void renderState(SolveUiState state) {
        if (state instanceof SolveUiState.Idle) {
            renderIdle();
        } else if (state instanceof SolveUiState.Loading) {
            renderLoading();
        } else if (state instanceof SolveUiState.Success) {
            renderSuccess(((SolveUiState.Success) state).getAnswer());
        } else if (state instanceof SolveUiState.Error) {
            renderError(((SolveUiState.Error) state).getMessage());
        }
    }

    private void renderIdle() {
        btnUploadImage.setEnabled(true);
        btnCaptureImage.setEnabled(true);
        btnSendAi.setEnabled(latestCapturedUri != null);
    }

    private void renderLoading() {
        bottomSheetResult.setVisibility(View.VISIBLE);
        tvOcrTitle.setText("Dang xu ly voi Gemini");
        edtOcrResult.setText("Dang phan tich anh, vui long cho...");
        btnUploadImage.setEnabled(false);
        btnCaptureImage.setEnabled(false);
        btnSendAi.setEnabled(false);
    }

    private void renderSuccess(String answer) {
        bottomSheetResult.setVisibility(View.VISIBLE);
        tvOcrTitle.setText("Ket qua Gemini");
        edtOcrResult.setText(answer);
        btnUploadImage.setEnabled(true);
        btnCaptureImage.setEnabled(true);
        btnSendAi.setEnabled(true);
    }

    private void renderError(String message) {
        bottomSheetResult.setVisibility(View.VISIBLE);
        tvOcrTitle.setText("Gemini loi");
        edtOcrResult.setText(message);
        btnUploadImage.setEnabled(true);
        btnCaptureImage.setEnabled(true);
        btnSendAi.setEnabled(latestCapturedUri != null);
        Toast.makeText(requireContext(), "Loi: " + message, Toast.LENGTH_LONG).show();
    }

    private void openGallery() {
        pickImageLauncher.launch("image/*");
    }

    private void retrySolveWithExtraText() {
        if (latestCapturedUri == null) {
            Toast.makeText(requireContext(), "Hay chup anh truoc", Toast.LENGTH_SHORT).show();
            return;
        }
        String extraText = edtOcrResult.getText() != null
                ? edtOcrResult.getText().toString().trim()
                : "";
        viewModel.solveFromUri(latestCapturedUri, extraText);
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder().build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageCapture
                );

            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(requireContext(), "Lỗi khởi tạo camera", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void captureAndSolve() {
        if (imageCapture == null) {
            Toast.makeText(requireContext(), "Camera chua san sang", Toast.LENGTH_SHORT).show();
            return;
        }

        File photoFile = new File(
                requireContext().getCacheDir(),
                "scan_" + System.currentTimeMillis() + ".jpg"
        );

        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(
                outputOptions,
                cameraExecutor,
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        latestCapturedUri = Uri.fromFile(photoFile);
                        String extraText = edtOcrResult.getText() != null
                                ? edtOcrResult.getText().toString().trim()
                                : "";

                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() ->
                                viewModel.solveFromUri(latestCapturedUri, extraText));
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(), "Khong chup duoc anh", Toast.LENGTH_SHORT).show());
                    }
                }
        );
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}
