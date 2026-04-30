package com.example.final_project;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        ImageView ivLogo = findViewById(R.id.iv_logo);
        TextView tvAppName = findViewById(R.id.tv_app_name);
        TextView tvSlogan = findViewById(R.id.tv_slogan);

        // Load animations
        Animation fadeInScale = AnimationUtils.loadAnimation(this, R.anim.fade_in_scale);
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);

        ivLogo.startAnimation(fadeInScale);
        tvAppName.startAnimation(slideUp);
        tvSlogan.startAnimation(slideUp);

        // Navigate to MainActivity after 2500ms
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish();
        }, 2500);
    }
}
