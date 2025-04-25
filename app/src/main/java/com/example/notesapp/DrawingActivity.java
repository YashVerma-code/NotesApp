package com.example.notesapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;

import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Stack;

public class DrawingActivity extends AppCompatActivity {

    private DrawingView drawingView;
    private ImageButton pencilBtn, eraserBtn, undoBtn, redoBtn, clearBtn, saveBtn, cancelBtn;
    private ImageButton colorBlackBtn, colorRedBtn, colorBlueBtn, colorGreenBtn, colorYellowBtn;
    private SeekBar brushSizeSeekBar;
    private LinearLayout colorPalette;
    private EditText drawingDescription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drawing);

        // Initialize drawing view
        drawingView = findViewById(R.id.drawing_view);
        drawingDescription = findViewById(R.id.drawingDescription);

        // Get intent data
        Intent intent = getIntent();
        boolean isDrawing = intent.getBooleanExtra("isDrawing", false);
        String description = intent.getStringExtra("description");

        drawingDescription.setText(description);

        // Check if we have bitmap bytes (better for passing through intent)
        byte[] drawingBytes = intent.getByteArrayExtra("drawing_bytes");
        if (isDrawing && drawingBytes != null) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(drawingBytes, 0, drawingBytes.length);
            Drawing drawing = new Drawing(bitmap, description);
            drawingView.setDrawing(drawing);
        }
        // If no bytes, try to get bitmap directly (older method)
        else if (isDrawing) {
            Bitmap bitmap = intent.getParcelableExtra("bitmap");
            if (bitmap != null) {
                Drawing drawing = new Drawing(bitmap, description);
                drawingView.setDrawing(drawing);
            }
        }


        // Initialize all buttons
        pencilBtn = findViewById(R.id.pencil_btn);
        eraserBtn = findViewById(R.id.eraser_btn);
        undoBtn = findViewById(R.id.undo_btn);
        redoBtn = findViewById(R.id.redo_btn);
        clearBtn = findViewById(R.id.clear_btn);
        saveBtn = findViewById(R.id.save_btn);


        // Initialize color buttons
        colorBlackBtn = findViewById(R.id.color_black);
        colorRedBtn = findViewById(R.id.color_red);
        colorBlueBtn = findViewById(R.id.color_blue);
        colorGreenBtn = findViewById(R.id.color_green);
        colorYellowBtn = findViewById(R.id.color_yellow);

        // Initialize brush size seekbar
        brushSizeSeekBar = findViewById(R.id.brush_size_seekbar);
        colorPalette = findViewById(R.id.color_palette);

        // Set default color to black
        drawingView.setColor(Color.BLACK);

        // Set onClick listeners for buttons
        pencilBtn.setOnClickListener(v -> {
            drawingView.setTool(DrawingView.Tool.PENCIL);
            pencilBtn.setBackgroundColor(Color.LTGRAY);
            eraserBtn.setBackgroundColor(Color.TRANSPARENT);
            colorPalette.setVisibility(View.VISIBLE);
        });

        eraserBtn.setOnClickListener(v -> {
            drawingView.setTool(DrawingView.Tool.ERASER);
            eraserBtn.setBackgroundColor(Color.LTGRAY);
            pencilBtn.setBackgroundColor(Color.TRANSPARENT);
            colorPalette.setVisibility(View.GONE);
        });

        undoBtn.setOnClickListener(v -> drawingView.undo());
        redoBtn.setOnClickListener(v -> drawingView.redo());
        clearBtn.setOnClickListener(v -> drawingView.clearCanvas());

        // In DrawingActivity.java - update saveBtn.setOnClickListener
        saveBtn.setOnClickListener(v -> {
            Bitmap newBitmap = drawingView.getBitmap();
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            newBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] byteArray = stream.toByteArray();

            Intent resultIntent = new Intent();
            resultIntent.putExtra("drawing_image", byteArray);
            resultIntent.putExtra("description", drawingDescription.getText().toString().trim()); // New description

            // Pass back the original description to identify which drawing we're updating
            String originalDescription = getIntent().getStringExtra("original_description");
            if (originalDescription != null) {
                resultIntent.putExtra("original_description", originalDescription);
            }

            setResult(RESULT_OK, resultIntent);
            finish();
        });



        // Color buttons
        colorBlackBtn.setOnClickListener(v -> drawingView.setColor(Color.BLACK));
        colorRedBtn.setOnClickListener(v -> drawingView.setColor(Color.RED));
        colorBlueBtn.setOnClickListener(v -> drawingView.setColor(Color.BLUE));
        colorGreenBtn.setOnClickListener(v -> drawingView.setColor(Color.GREEN));
        colorYellowBtn.setOnClickListener(v -> drawingView.setColor(Color.YELLOW));

        // Brush size seekbar
        brushSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                drawingView.setBrushSize(progress + 5); // Min size of 5
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }
}