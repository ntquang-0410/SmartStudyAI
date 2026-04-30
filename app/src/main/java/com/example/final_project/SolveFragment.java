package com.example.final_project;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
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
 * SolveFragment – Input form + result screen with LaTeX rendering via KaTeX.
 *
 * States:
 *   Idle    → groupInput visible, layoutResult gone
 *   Loading → groupInput visible (image + spinner), layoutResult gone
 *   Success → groupInput gone,  layoutResult visible (WebView + actions)
 *   Error   → groupInput visible (toast shown), layoutResult gone
 */
public class SolveFragment extends Fragment {

    private FragmentSolveBinding binding;
    private SolveViewModel viewModel;

    /** Current selected image (camera or gallery). */
    private Uri selectedImageUri;
    /** Temp URI for camera capture. */
    private Uri cameraImageUri;
    /** Raw answer text kept for copy/share. */
    private String rawAnswer;

    // ── Activity Result Launchers ─────────────────────────────────────────────

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
        setupWebView();
        setupButtons();
        observeState();
    }

    @Override
    public void onDestroyView() {
        if (binding != null) {
            binding.webViewAnswer.stopLoading();
            binding.webViewAnswer.destroy();
        }
        super.onDestroyView();
        binding = null;
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private void setupWebView() {
        WebSettings ws = binding.webViewAnswer.getSettings();
        ws.setJavaScriptEnabled(true);   // required for KaTeX
        ws.setDomStorageEnabled(true);
        ws.setSupportZoom(false);
        ws.setBuiltInZoomControls(false);
        ws.setDisplayZoomControls(false);
        ws.setLoadsImagesAutomatically(true);
        binding.webViewAnswer.setScrollBarStyle(WebView.SCROLLBARS_INSIDE_OVERLAY);
        binding.webViewAnswer.setBackgroundColor(0xFFFFFFFF);
    }

    private void setupButtons() {
        binding.btnGallery.setOnClickListener(v -> openGallery());
        binding.btnCamera.setOnClickListener(v  -> openCamera());
        binding.btnSolve.setOnClickListener(v   -> solve());
        binding.btnReset.setOnClickListener(v   -> viewModel.reset());
        binding.btnCopy.setOnClickListener(v    -> copyToClipboard());
        binding.btnShare.setOnClickListener(v   -> shareAnswer());
        binding.btnSolve.setEnabled(false);
    }

    private void observeState() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> renderState(state));
    }

    // ── State rendering ───────────────────────────────────────────────────────

    private void renderState(SolveUiState state) {
        if      (state instanceof SolveUiState.Idle)    renderIdle();
        else if (state instanceof SolveUiState.Loading) renderLoading();
        else if (state instanceof SolveUiState.Success) renderSuccess(((SolveUiState.Success) state).getAnswer());
        else if (state instanceof SolveUiState.Error)   renderError(((SolveUiState.Error) state).getMessage());
    }

    private void renderIdle() {
        binding.layoutLoading.setVisibility(View.GONE);
        binding.layoutResult.setVisibility(View.GONE);
        binding.groupInput.setVisibility(View.VISIBLE);
        binding.btnSolve.setEnabled(selectedImageUri != null);
    }

    private void renderLoading() {
        binding.layoutResult.setVisibility(View.GONE);
        binding.groupInput.setVisibility(View.VISIBLE);
        binding.layoutLoading.setVisibility(View.VISIBLE);
        binding.btnSolve.setEnabled(false);
    }

    private void renderSuccess(String answer) {
        rawAnswer = answer;
        binding.layoutLoading.setVisibility(View.GONE);
        binding.groupInput.setVisibility(View.GONE);
        binding.layoutResult.setVisibility(View.VISIBLE);

        // Show problem thumbnail in result header
        if (selectedImageUri != null) {
            binding.ivThumbnail.setImageURI(selectedImageUri);
        }

        // Convert markdown+LaTeX → HTML and load into WebView
        String html = MarkdownToHtml.toHtml(answer);
        binding.webViewAnswer.loadDataWithBaseURL(
                "https://cdn.jsdelivr.net",
                html,
                "text/html",
                "UTF-8",
                null
        );
    }

    private void renderError(String message) {
        binding.layoutResult.setVisibility(View.GONE);
        binding.groupInput.setVisibility(View.VISIBLE);
        binding.layoutLoading.setVisibility(View.GONE);
        binding.btnSolve.setEnabled(true);
        Toast.makeText(requireContext(), "Lỗi: " + message, Toast.LENGTH_LONG).show();
    }

    // ── Image selection ───────────────────────────────────────────────────────

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private void openCamera() {
        try {
            File imageFile = File.createTempFile("qanda_", ".jpg", requireContext().getCacheDir());
            cameraImageUri = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".provider",
                    imageFile
            );
            cameraLauncher.launch(cameraImageUri);
        } catch (IOException e) {
            Toast.makeText(requireContext(), "Không tạo được file ảnh tạm", Toast.LENGTH_SHORT).show();
        }
    }

    private void solve() {
        if (selectedImageUri == null) return;
        String extraText = binding.etExtraText.getText() != null
                ? binding.etExtraText.getText().toString().trim()
                : "";
        viewModel.solveFromUri(selectedImageUri, extraText);
    }

    // ── Actions ───────────────────────────────────────────────────────────────

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
}
