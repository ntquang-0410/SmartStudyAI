package com.example.final_project;

import android.app.DatePickerDialog;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {

    private static final SimpleDateFormat DATE_FMT =
            new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    private final UserProfileRepository repo = new UserProfileRepository();

    private ShapeableImageView ivAvatar;
    private EditText etName;
    private TextView tvBirthDate;
    private RadioGroup rgLevel;
    private TextView tvGradeLabel;
    private TextView tvGradeValue;
    private EditText etSchool;
    private ProgressBar pbLoading;

    private UserProfile profile = new UserProfile();
    private Uri pendingAvatarUri;

    private final ActivityResultLauncher<PickVisualMediaRequest> pickAvatar =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri == null) return;
                pendingAvatarUri = uri;
                ivAvatar.setImageURI(uri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        View root = findViewById(R.id.root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        ivAvatar = findViewById(R.id.iv_avatar);
        etName = findViewById(R.id.et_name);
        tvBirthDate = findViewById(R.id.tv_birth_date);
        rgLevel = findViewById(R.id.rg_level);
        tvGradeLabel = findViewById(R.id.tv_grade_label);
        tvGradeValue = findViewById(R.id.tv_grade_value);
        etSchool = findViewById(R.id.et_school);
        pbLoading = findViewById(R.id.pb_loading);

        TextView tvEmail = findViewById(R.id.tv_email);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getEmail() != null) tvEmail.setText(user.getEmail());

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_save).setOnClickListener(v -> save());
        findViewById(R.id.fl_avatar_edit).setOnClickListener(v -> launchPicker());
        ivAvatar.setOnClickListener(v -> launchPicker());

        tvBirthDate.setOnClickListener(v -> pickBirthDate());
        tvGradeValue.setOnClickListener(v -> pickGrade());

        rgLevel.setOnCheckedChangeListener((group, checkedId) -> {
            profile.setEduLevel(levelFromCheckedId(checkedId));
            // Clear grade if switching between schemas where it no longer fits.
            if (profile.getGrade() != null) {
                int g = profile.getGrade();
                if (!profile.usesGrade() && !profile.isUniversity()) {
                    profile.setGrade(null);
                } else if (g < profile.gradeMin() || g > profile.gradeMax()) {
                    profile.setGrade(null);
                }
            }
            renderGradeRow();
        });

        load();
    }

    private void launchPicker() {
        pickAvatar.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    private void load() {
        pbLoading.setVisibility(View.VISIBLE);
        repo.load()
                .addOnSuccessListener(p -> {
                    pbLoading.setVisibility(View.GONE);
                    profile = p == null ? new UserProfile() : p;
                    bindProfile();
                })
                .addOnFailureListener(e -> {
                    pbLoading.setVisibility(View.GONE);
                    Toast.makeText(this, "Không tải được hồ sơ: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void bindProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        String name = profile.getDisplayName();
        if (TextUtils.isEmpty(name) && user != null) name = user.getDisplayName();
        etName.setText(name == null ? "" : name);

        tvBirthDate.setText(profile.getBirthDate() == null ? "" : profile.getBirthDate());
        etSchool.setText(profile.getSchool() == null ? "" : profile.getSchool());

        rgLevel.setOnCheckedChangeListener(null);
        int rid = checkedIdForLevel(profile.getEduLevel());
        if (rid != 0) rgLevel.check(rid);
        rgLevel.setOnCheckedChangeListener((g, id) -> {
            profile.setEduLevel(levelFromCheckedId(id));
            if (profile.getGrade() != null) {
                int v = profile.getGrade();
                if (!profile.usesGrade() && !profile.isUniversity()) profile.setGrade(null);
                else if (v < profile.gradeMin() || v > profile.gradeMax()) profile.setGrade(null);
            }
            renderGradeRow();
        });

        renderGradeRow();

        String url = profile.getPhotoUrl();
        if (!TextUtils.isEmpty(url)) {
            ImageLoader.load(ivAvatar, url, android.R.drawable.sym_def_app_icon);
        }
    }

    private void renderGradeRow() {
        boolean show = profile.usesGrade() || profile.isUniversity();
        tvGradeLabel.setVisibility(show ? View.VISIBLE : View.GONE);
        tvGradeValue.setVisibility(show ? View.VISIBLE : View.GONE);

        if (!show) {
            tvGradeValue.setText("");
            return;
        }
        if (profile.isUniversity()) {
            tvGradeLabel.setText("Năm đang học");
            tvGradeValue.setHint("Chọn năm (1–6)");
        } else {
            tvGradeLabel.setText("Lớp");
            tvGradeValue.setHint("Chọn lớp (" + profile.gradeMin() + "–" + profile.gradeMax() + ")");
        }
        Integer g = profile.getGrade();
        if (g == null) {
            tvGradeValue.setText("");
        } else if (profile.isUniversity()) {
            tvGradeValue.setText("Năm " + g);
        } else {
            tvGradeValue.setText("Lớp " + g);
        }
    }

    private void pickBirthDate() {
        Calendar c = Calendar.getInstance();
        try {
            if (!TextUtils.isEmpty(profile.getBirthDate())) {
                c.setTime(DATE_FMT.parse(profile.getBirthDate()));
            } else {
                c.add(Calendar.YEAR, -16);
            }
        } catch (Exception ignore) {
            c.add(Calendar.YEAR, -16);
        }
        DatePickerDialog dlg = new DatePickerDialog(this,
                (view, year, month, day) -> {
                    Calendar picked = Calendar.getInstance();
                    picked.set(year, month, day);
                    profile.setBirthDate(DATE_FMT.format(picked.getTime()));
                    tvBirthDate.setText(profile.getBirthDate());
                },
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        dlg.getDatePicker().setMaxDate(System.currentTimeMillis());
        dlg.show();
    }

    private void pickGrade() {
        if (!profile.usesGrade() && !profile.isUniversity()) return;

        int min = profile.gradeMin();
        int max = profile.gradeMax();
        int n = max - min + 1;
        String[] labels = new String[n];
        for (int i = 0; i < n; i++) {
            int v = min + i;
            labels[i] = profile.isUniversity() ? ("Năm " + v) : ("Lớp " + v);
        }
        int checked = -1;
        if (profile.getGrade() != null) {
            int idx = profile.getGrade() - min;
            if (idx >= 0 && idx < n) checked = idx;
        }
        new AlertDialog.Builder(this)
                .setTitle(profile.isUniversity() ? "Chọn năm đang học" : "Chọn lớp")
                .setSingleChoiceItems(labels, checked, (d, which) -> {
                    profile.setGrade(min + which);
                    renderGradeRow();
                    d.dismiss();
                })
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void save() {
        profile.setDisplayName(etName.getText().toString().trim());
        profile.setSchool(etSchool.getText().toString().trim());

        pbLoading.setVisibility(View.VISIBLE);
        if (pendingAvatarUri != null) {
            repo.uploadAvatar(pendingAvatarUri)
                    .addOnSuccessListener(url -> {
                        profile.setPhotoUrl(url);
                        pendingAvatarUri = null;
                        persist();
                    })
                    .addOnFailureListener(e -> {
                        pbLoading.setVisibility(View.GONE);
                        Toast.makeText(this, "Tải ảnh thất bại: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
        } else {
            persist();
        }
    }

    private void persist() {
        repo.save(profile)
                .addOnSuccessListener(v -> {
                    pbLoading.setVisibility(View.GONE);
                    Toast.makeText(this, "Đã lưu hồ sơ", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    pbLoading.setVisibility(View.GONE);
                    Toast.makeText(this, "Lưu thất bại: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private static String levelFromCheckedId(int id) {
        if (id == R.id.rb_primary) return UserProfile.LEVEL_PRIMARY;
        if (id == R.id.rb_secondary) return UserProfile.LEVEL_SECONDARY;
        if (id == R.id.rb_high) return UserProfile.LEVEL_HIGH;
        if (id == R.id.rb_university) return UserProfile.LEVEL_UNIVERSITY;
        if (id == R.id.rb_other) return UserProfile.LEVEL_OTHER;
        return null;
    }

    private static int checkedIdForLevel(String level) {
        if (UserProfile.LEVEL_PRIMARY.equals(level)) return R.id.rb_primary;
        if (UserProfile.LEVEL_SECONDARY.equals(level)) return R.id.rb_secondary;
        if (UserProfile.LEVEL_HIGH.equals(level)) return R.id.rb_high;
        if (UserProfile.LEVEL_UNIVERSITY.equals(level)) return R.id.rb_university;
        if (UserProfile.LEVEL_OTHER.equals(level)) return R.id.rb_other;
        return 0;
    }
}
