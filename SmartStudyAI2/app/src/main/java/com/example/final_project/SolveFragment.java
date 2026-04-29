package com.example.final_project;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.final_project.databinding.FragmentSolveBinding;

import java.io.File;
import java.io.IOException;

/**
 * SolveFragment.java
 * ─────────────────────────────────────────────────────────────────────
 * Xử lý toàn bộ UI: chọn ảnh, hiển thị preview, gọi ViewModel, render kết quả.
 * Dùng ViewBinding — bật trong build.gradle: buildFeatures { viewBinding true }
 *
 * Layout tối thiểu (fragment_solve.xml):
 *   ImageView    id="ivPreview"
 *   EditText     id="etExtraText"
 *   Button       id="btnCamera"
 *   Button       id="btnGallery"
 *   Button       id="btnSolve"
 *   ProgressBar  id="progressBar"
 *   ScrollView   id="scrollAnswer"  (chứa TextView id="tvAnswer")
 *   Button       id="btnReset"
 *   LinearLayout id="groupInput"
 * ─────────────────────────────────────────────────────────────────────
 */
public class SolveFragment extends Fragment {

    // ── Fields ────────────────────────────────────────────────────────────────

    private FragmentSolveBinding binding;
    private SolveViewModel viewModel;

    /** Uri ảnh tạm lưu khi dùng camera. */
    private Uri cameraImageUri;

    /** Uri ảnh hiện tại (camera hoặc gallery) — dùng khi bấm Giải. */
    private Uri selectedImageUri;

    // ── Activity Result Launchers ─────────────────────────────────────────────

    /** Chọn ảnh từ Gallery */
    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    new ActivityResultCallback<ActivityResult>() {
                        @Override
                        public void onActivityResult(ActivityResult result) {
                            if (result.getResultCode() == Activity.RESULT_OK
                                    && result.getData() != null
                                    && result.getData().getData() != null) {

                                selectedImageUri = result.getData().getData();
                                binding.ivPreview.setImageURI(selectedImageUri);
                                binding.btnSolve.setEnabled(true);
                            }
                        }
                    });

    /** Chụp ảnh từ Camera */
    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.TakePicture(),
                    new ActivityResultCallback<Boolean>() {
                        @Override
                        public void onActivityResult(Boolean success) {
                            if (Boolean.TRUE.equals(success) && cameraImageUri != null) {
                                selectedImageUri = cameraImageUri;
                                binding.ivPreview.setImageURI(selectedImageUri);
                                binding.btnSolve.setEnabled(true);
                            }
                        }
                    });

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSolveBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(SolveViewModel.class);

        setupButtons();
        observeState();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private void setupButtons() {
        binding.btnGallery.setOnClickListener(v -> openGallery());
        binding.btnCamera.setOnClickListener(v  -> openCamera());
        binding.btnSolve.setOnClickListener(v   -> solve());
        binding.btnReset.setOnClickListener(v   -> viewModel.reset());

        // Disable nút Giải cho đến khi có ảnh
        binding.btnSolve.setEnabled(false);
    }

    private void observeState() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> renderState(state));
    }

    // ── State Rendering ───────────────────────────────────────────────────────

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
        binding.progressBar.setVisibility(View.GONE);
        binding.scrollAnswer.setVisibility(View.GONE);
        binding.btnReset.setVisibility(View.GONE);
        binding.groupInput.setVisibility(View.VISIBLE);
        binding.btnSolve.setEnabled(selectedImageUri != null);
    }

    private void renderLoading() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.scrollAnswer.setVisibility(View.GONE);
        binding.btnSolve.setEnabled(false);
    }

    private void renderSuccess(String answer) {
        binding.progressBar.setVisibility(View.GONE);
        binding.groupInput.setVisibility(View.GONE);
        binding.scrollAnswer.setVisibility(View.VISIBLE);
        binding.tvAnswer.setText(answer);
        binding.btnReset.setVisibility(View.VISIBLE);
    }

    private void renderError(String message) {
        binding.progressBar.setVisibility(View.GONE);
        binding.btnSolve.setEnabled(true);
        Toast.makeText(requireContext(), "Lỗi: " + message, Toast.LENGTH_LONG).show();
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private void openCamera() {
        try {
            File imageFile = File.createTempFile("qanda_", ".jpg",
                    requireContext().getCacheDir());

            cameraImageUri = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".provider", // khớp AndroidManifest
                    imageFile
            );

            cameraLauncher.launch(cameraImageUri);
        } catch (IOException e) {
            Toast.makeText(requireContext(),
                    "Không tạo được file ảnh tạm", Toast.LENGTH_SHORT).show();
        }
    }

    private void solve() {
        if (selectedImageUri == null) return;

        String extraText = binding.etExtraText.getText().toString().trim();
        viewModel.solveFromUri(selectedImageUri, extraText);
    }
}