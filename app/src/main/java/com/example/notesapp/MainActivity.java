package com.example.notesapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private ConstraintLayout mainLayout;
    private LinearLayout colorPickerLayout;
    private ImageView colorIcon;
    private EditText titleEditText, contentEditText;
    private Note currentNote;
    private int currentBackgroundColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // View initializations
        mainLayout = findViewById(R.id.mainLayout);
        colorPickerLayout = findViewById(R.id.colorPickerLayout);
        colorIcon = findViewById(R.id.color_icon);
        titleEditText = findViewById(R.id.titleEditText);
        contentEditText = findViewById(R.id.contentEditText);

        // Set default background color
        currentBackgroundColor = ContextCompat.getColor(this, R.color.black);

        // Check if we're editing an existing note
        if (getIntent().hasExtra("note")) {
            currentNote = (Note) getIntent().getSerializableExtra("note");
            if (currentNote != null) {
                titleEditText.setText(currentNote.getTitle());
                contentEditText.setText(currentNote.getContent());
                currentBackgroundColor = currentNote.getBackgroundColor();
                mainLayout.setBackgroundColor(currentBackgroundColor);
            }
        }

        // Insets for edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Color selector setup
        View colorRed = findViewById(R.id.colorRed);
        View colorYellow = findViewById(R.id.colorYellow);
        View colorGreen = findViewById(R.id.colorGreen);
        View colorBlue = findViewById(R.id.colorBlue);
        View colorBlack = findViewById(R.id.colorBlack);
        View colorViolet = findViewById(R.id.colorViolet);

        // Toggle color picker visibility
        colorIcon.setOnClickListener(view -> {
            if (colorPickerLayout.getVisibility() == View.VISIBLE) {
                colorPickerLayout.setVisibility(View.GONE);
            } else {
                colorPickerLayout.setVisibility(View.VISIBLE);
            }
        });

        colorRed.setOnClickListener(view -> {
            currentBackgroundColor = ContextCompat.getColor(MainActivity.this, R.color.red);
            mainLayout.setBackgroundColor(currentBackgroundColor);
            colorPickerLayout.setVisibility(View.GONE);
        });

        colorYellow.setOnClickListener(view -> {
            currentBackgroundColor = ContextCompat.getColor(MainActivity.this, R.color.yellow);
            mainLayout.setBackgroundColor(currentBackgroundColor);
            colorPickerLayout.setVisibility(View.GONE);
        });

        colorGreen.setOnClickListener(view -> {
            currentBackgroundColor = ContextCompat.getColor(MainActivity.this, R.color.green);
            mainLayout.setBackgroundColor(currentBackgroundColor);
            colorPickerLayout.setVisibility(View.GONE);
        });

        colorBlue.setOnClickListener(view -> {
            currentBackgroundColor = ContextCompat.getColor(MainActivity.this, R.color.blue);
            mainLayout.setBackgroundColor(currentBackgroundColor);
            colorPickerLayout.setVisibility(View.GONE);
        });

        colorBlack.setOnClickListener(view -> {
            currentBackgroundColor = ContextCompat.getColor(MainActivity.this, R.color.black);
            mainLayout.setBackgroundColor(currentBackgroundColor);
            colorPickerLayout.setVisibility(View.GONE);
        });

        colorViolet.setOnClickListener(view -> {
            currentBackgroundColor = ContextCompat.getColor(MainActivity.this, R.color.violet);
            mainLayout.setBackgroundColor(currentBackgroundColor);
            colorPickerLayout.setVisibility(View.GONE);
        });

        // Add back button functionality
        findViewById(R.id.editedTextView).setOnClickListener(view -> {
            saveNote();
            navigateToHome();
        });

        // Handle back press with the new API
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                saveNote();
                finish();
            }
        });
    }

    private void saveNote() {
        String title = titleEditText.getText().toString().trim();
        String content = contentEditText.getText().toString().trim();

        if (title.isEmpty() && content.isEmpty()) {
            // Don't save empty notes
            return;
        }

        // Default title if empty
        if (title.isEmpty()) {
            title = "Untitled Note";
        }

        ArrayList<Note> notesList = loadNotes();

        if (currentNote != null) {
            // Update existing note
            for (int i = 0; i < notesList.size(); i++) {
                Note note = notesList.get(i);
                if (note != null && note.getDateCreated() != null &&
                        currentNote.getDateCreated() != null &&
                        note.getDateCreated().equals(currentNote.getDateCreated())) {
                    note.setTitle(title);
                    note.setContent(content);
                    note.setBackgroundColor(currentBackgroundColor);
                    break;
                }
            }
        } else {
            // Create new note
            Note newNote = new Note(title, content, currentBackgroundColor);
            notesList.add(0, newNote); // Add to beginning of list
        }

        // Save the updated notes list
        saveNotes(notesList);
        Toast.makeText(this, "Note saved", Toast.LENGTH_SHORT).show();
    }

    private void navigateToHome() {
        Intent intent = new Intent(MainActivity.this, Home.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    // Save notes to SharedPreferences
    private void saveNotes(ArrayList<Note> notesList) {
        SharedPreferences sharedPreferences = getSharedPreferences("notes_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(notesList);
        editor.putString("notes", json);
        editor.apply();
    }

    // Load notes from SharedPreferences
    private ArrayList<Note> loadNotes() {
        SharedPreferences sharedPreferences = getSharedPreferences("notes_prefs", MODE_PRIVATE);
        Gson gson = new Gson();
        String json = sharedPreferences.getString("notes", null);
        Type type = new TypeToken<ArrayList<Note>>() {}.getType();

        if (json != null) {
            return gson.fromJson(json, type);
        } else {
            return new ArrayList<>();
        }
    }
}