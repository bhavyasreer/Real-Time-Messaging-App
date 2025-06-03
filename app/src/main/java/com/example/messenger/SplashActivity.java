package com.example.messenger;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.View;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView logo = findViewById(R.id.logoImageView);
        TextView appName = findViewById(R.id.appNameTextView);
        TextView from = findViewById(R.id.fromTextView);

        // Load animations
        Animation logoAnim = AnimationUtils.loadAnimation(this, R.anim.splash_logo_anim);
        Animation nameAnim = AnimationUtils.loadAnimation(this, R.anim.splash_name_anim);
        Animation fromAnim = AnimationUtils.loadAnimation(this, R.anim.splash_from_anim);

        // Start logo animation
        logo.startAnimation(logoAnim);

        // Start app name animation after logo animation finishes
        logoAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}
            @Override
            public void onAnimationEnd(Animation animation) {
                appName.setVisibility(View.VISIBLE); // Make app name visible
                appName.startAnimation(nameAnim);
            }
            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        // Start 'from' animation after app name animation finishes
        nameAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}
            @Override
            public void onAnimationEnd(Animation animation) {
                from.setVisibility(View.VISIBLE); // Make 'from' visible
                from.startAnimation(fromAnim);
            }
            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        // Initially hide app name and from text so they only appear after animation
        appName.setVisibility(View.INVISIBLE);
        from.setVisibility(View.INVISIBLE);

        new Handler().postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, ChatListActivity.class));
            finish();
        }, 2500); // Increased delay slightly for animations
    }
} 