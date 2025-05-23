package com.example.notesapp;

import android.content.Intent;
import java.util.Collections;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.google.android.material.navigation.NavigationView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class Home extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, NoteItemTouchHelper.OnNoteActionListener {

    private DrawerLayout drawerLayout;
    private RecyclerView notesRecyclerView;
    private NotesAdapter notesAdapter;
    private ArrayList<Note> notesList;
    private ArrayList<Note> filteredNotesList;
    private EditText searchEditText;
    private String currentUserId;
    private ItemTouchHelper itemTouchHelper;

    private TextView welcomeText;
    private Button logoutButton;
    private ImageButton addNoteButton;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        sharedPreferences = getSharedPreferences("NotesAppPrefs", MODE_PRIVATE);
        String username = sharedPreferences.getString("username", "User");
        currentUserId = sharedPreferences.getString("userId", null);

        if (currentUserId == null) {
            Toast.makeText(this, "User not found. Please login again.", Toast.LENGTH_SHORT).show();
            logoutUser();
            return;
        }

        // Initialize drawer layout
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Set up the hamburger menu button to open/close drawer
        findViewById(R.id.menuButton).setOnClickListener(view -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        // Initialize RecyclerView
        notesRecyclerView = findViewById(R.id.notesRecyclerView);
        notesRecyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));

        // Initialize notes list and adapter
        notesList = loadNotes();
        filteredNotesList = new ArrayList<>(notesList);
        notesAdapter = new NotesAdapter(this, filteredNotesList);
        notesRecyclerView.setAdapter(notesAdapter);

        // Set up ItemTouchHelper for drag-and-drop functionality
        setupDragAndDrop();

        // Set up search functionality
        searchEditText = findViewById(R.id.searchNotes);
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterNotes(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Set up add note button
        findViewById(R.id.addNoteButton).setOnClickListener(view -> {
            Intent intent = new Intent(Home.this, NotesActivity.class);
            intent.putExtra("userId", currentUserId);
            startActivity(intent);
        });

        // System insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        logoutButton = findViewById(R.id.logoutButton);
        addNoteButton = findViewById(R.id.addNoteButton);
        ImageButton profileButton = findViewById(R.id.profileButton); // Ensure this ID exists in your layout
        profileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Home.this, ProfileActivity.class);
                intent.putExtra("userId", currentUserId); // Optional: Pass user ID if needed
                startActivity(intent);
            }
        });


        addNoteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Home.this, NotesActivity.class);
                startActivity(intent);
            }
        });

        // Set up logout button click listener
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logoutUser();
            }
        });

        // Handle back press with the new API
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    finish();
                }
            }
        });
    }

    private void setupDragAndDrop() {
        // Calculate the delete threshold based on screen height (20% of screen height)
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int deleteThreshold = displayMetrics.heightPixels / 5;

        // Create and attach the ItemTouchHelper
        NoteItemTouchHelper callback = new NoteItemTouchHelper(notesAdapter, this, deleteThreshold);
        itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(notesRecyclerView);
    }

    // Implementation of NoteItemTouchHelper.OnNoteActionListener
    @Override
    public void onNoteDeleted(Note note) {
        // Delete note directly without confirmation dialog
        deleteNote(note);
    }

    @Override
    public void onNoteMoved(int fromPosition, int toPosition) {
        // Update the filtered list first
        notesAdapter.moveNote(fromPosition, toPosition);

        // Find the corresponding notes in the original list
        Note noteFrom = filteredNotesList.get(fromPosition);
        Note noteTo = filteredNotesList.get(toPosition);

        int fromOriginalPos = -1;
        int toOriginalPos = -1;

        for (int i = 0; i < notesList.size(); i++) {
            Note currentNote = notesList.get(i);
            if (currentNote.getDateCreated() != null) {
                if (currentNote.getDateCreated().equals(noteFrom.getDateCreated())) {
                    fromOriginalPos = i;
                }
                if (currentNote.getDateCreated().equals(noteTo.getDateCreated())) {
                    toOriginalPos = i;
                }
            }
        }

        // Update the original list if both positions are found
        if (fromOriginalPos != -1 && toOriginalPos != -1) {
            if (fromOriginalPos < toOriginalPos) {
                for (int i = fromOriginalPos; i < toOriginalPos; i++) {
                    Collections.swap(notesList, i, i + 1);
                }
            } else {
                for (int i = fromOriginalPos; i > toOriginalPos; i--) {
                    Collections.swap(notesList, i, i - 1);
                }
            }

            // Save the updated notes list
            saveNotes(notesList);
        }
    }

    private void logoutUser() {
        // Clear login state
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isLoggedIn", false);
        editor.remove("username");
        editor.remove("userId");
        editor.apply();

        // Navigate back to login screen
        Intent intent = new Intent(Home.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload notes when returning to this activity
        notesList = loadNotes();
        filterNotes(searchEditText.getText().toString());
    }

    private void filterNotes(String query) {
        if (query.isEmpty()) {
            filteredNotesList = new ArrayList<>(notesList);
        } else {
            String searchText = query.toLowerCase();
            filteredNotesList = notesList.stream()
                    .filter(note -> note.getTitle().toLowerCase().contains(searchText) ||
                            note.getContent().toLowerCase().contains(searchText))
                    .collect(Collectors.toCollection(ArrayList::new));
        }
        notesAdapter.updateNotes(filteredNotesList);
    }

    private void deleteNote(Note note) {
        // Find the note in the filtered list
        int filteredPosition = -1;
        for (int i = 0; i < filteredNotesList.size(); i++) {
            if (filteredNotesList.get(i).getDateCreated().equals(note.getDateCreated())) {
                filteredPosition = i;
                break;
            }
        }

        if (filteredPosition != -1) {
            filteredNotesList.remove(filteredPosition);
            notesAdapter.notifyItemRemoved(filteredPosition);
        }

        // Find and remove the note from the original list
        for (int i = 0; i < notesList.size(); i++) {
            Note currentNote = notesList.get(i);
            if (currentNote.getDateCreated() != null &&
                    note.getDateCreated() != null &&
                    currentNote.getDateCreated().equals(note.getDateCreated()) &&
                    currentNote.getUserId() != null &&
                    currentNote.getUserId().equals(currentUserId)) {
                notesList.remove(i);
                break;
            }
        }

        // Save the updated notes list
        saveNotes(notesList);
        Toast.makeText(this, "Note deleted", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation item clicks
        int id = item.getItemId();

        if (id == R.id.nav_notes) {
            // Already on notes view
            Toast.makeText(this, "Notes", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_archive) {
            // Navigate to archive
            Toast.makeText(this, "Archive", Toast.LENGTH_SHORT).show();
            // Add your navigation code here
        } else if (id == R.id.nav_deleted) {
            // Navigate to deleted files
            Toast.makeText(this, "Deleted Files", Toast.LENGTH_SHORT).show();
            // Add your navigation code here
        } else if (id == R.id.nav_profile) {
            // Navigate to profile
            Toast.makeText(this, "Profile", Toast.LENGTH_SHORT).show();
            // Add your navigation code here
        } else if (id == R.id.nav_settings) {
            // Navigate to settings
            Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show();
            // Add your navigation code here
        }

        // Close drawer after item click
        if (drawerLayout != null) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
        return true;
    }

    // Load saved notes from SharedPreferences
    private ArrayList<Note> loadNotes() {
        SharedPreferences sharedPreferences = getSharedPreferences("notes_prefs", MODE_PRIVATE);
        SharedPreferences userData = getSharedPreferences("UserData", MODE_PRIVATE);
        String userId = userData.getString("userId", "null"); // Retrieve the userId
        if (userId == null) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
            return new ArrayList<>();
        }
        Gson gson = new Gson();
        String json = sharedPreferences.getString("notes", null);
        Type type = new TypeToken<ArrayList<Note>>() {}.getType();

        if (json != null) {
            ArrayList<Note> loadedNotes = gson.fromJson(json, type);
            ArrayList<Note> userNotes = new ArrayList<Note>();
            for (Note note : loadedNotes) {
                if (note.getUserId() != null && note.getUserId().equals(currentUserId)) {
                    userNotes.add(note);
                }
            }
            Log.d("NotesApp", "Loaded " + loadedNotes.size() + " notes");
            return userNotes;
        } else {
            Log.d("NotesApp", "No notes found, returning empty list");
            return new ArrayList<>();
        }
    }

    // Save notes to SharedPreferences
    private void saveNotes(ArrayList<Note> notesList) {
        SharedPreferences sharedPreferences = getSharedPreferences("notes_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(notesList);
        editor.putString("notes", json);
        editor.apply();
        Log.d("NotesApp", "Saved " + notesList.size() + " notes to preferences");
    }
}