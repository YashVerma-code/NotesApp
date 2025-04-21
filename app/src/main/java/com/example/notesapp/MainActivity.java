package com.example.notesapp;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private ConstraintLayout mainLayout;
    private LinearLayout colorPickerLayout;
    private ImageView colorIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // View initializations
        mainLayout = findViewById(R.id.mainLayout);
        colorPickerLayout = findViewById(R.id.colorPickerLayout);
        colorIcon = findViewById(R.id.color_icon);

        // Insets for edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        View colorRed = findViewById(R.id.colorRed);
        View colorYellow = findViewById(R.id.colorYellow);
        View colorGreen = findViewById(R.id.colorGreen);
        View colorBlue =findViewById(R.id.colorBlue);
        View colorBlack =findViewById(R.id.colorBlack);
        View colorViolet =findViewById(R.id.colorViolet);
        // Toggle color picker visibility
        colorIcon.setOnClickListener(view -> {
            if (colorPickerLayout.getVisibility() == View.VISIBLE) {
                colorPickerLayout.setVisibility(View.GONE);
            } else {
                colorPickerLayout.setVisibility(View.VISIBLE);
            }
        });

        colorRed.setOnClickListener(view -> {
            mainLayout.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.red));
            colorPickerLayout.setVisibility(View.GONE);
        });

        colorYellow.setOnClickListener(view -> {
            mainLayout.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.yellow));
            colorPickerLayout.setVisibility(View.GONE);
        });

        colorGreen.setOnClickListener(view -> {
            mainLayout.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.green));
            colorPickerLayout.setVisibility(View.GONE);
        });
        colorBlue.setOnClickListener(view -> {
            mainLayout.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.blue));
            colorPickerLayout.setVisibility(View.GONE);
        });
        colorBlack.setOnClickListener(view -> {
            mainLayout.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.black));
            colorPickerLayout.setVisibility(View.GONE);
        });
        colorViolet.setOnClickListener(view -> {
            mainLayout.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.violet));
            colorPickerLayout.setVisibility(View.GONE);
        });
    }
}
