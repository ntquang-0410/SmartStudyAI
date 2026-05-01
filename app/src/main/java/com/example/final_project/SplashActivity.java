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
import androidx.core.splashscreen.SplashScreen;

import com.google.firebase.auth.FirebaseAuth;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
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

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Class<?> next = FirebaseAuth.getInstance().getCurrentUser() != null
                    ? MainActivity.class
                    : LoginActivity.class;
            startActivity(new Intent(SplashActivity.this, next));
            finish();
        }, 1200);
    }
}
