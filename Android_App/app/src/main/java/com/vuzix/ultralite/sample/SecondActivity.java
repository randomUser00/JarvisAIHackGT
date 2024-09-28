package com.vuzix.ultralite.sample;

import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.vuzix.ultralite.sample.R.id;

public class SecondActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_second);

        // Find the shine view
        View shineView = findViewById(R.id.shine);

        // Load the left-right animation
        Animation shineAnimation = AnimationUtils.loadAnimation(this, R.anim.left_right);

        // Start the shine effect animation
        shineView.startAnimation(shineAnimation);
    }
}