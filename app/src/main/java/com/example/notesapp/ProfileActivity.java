package com.example.notesapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;

public class ProfileActivity extends AppCompatActivity {

    private ImageView backBtn, saveProfileBtn, profileImage, editProfileImage;
    private TextView profileUsername, profileEmail, notesCount, storageText;
    private MaterialButton editProfileBtn;
    private View defaultColorPreview, notesUsageBar;
    private SwitchCompat autoSaveSwitch, darkModeSwitch;
    private View defaultNoteColorContainer, changePasswordBtn, exportNotesBtn, deleteAccountBtn;
    private TextView appVersion;

    private SharedPreferences sharedPreferences;
    private SharedPreferences userPrefs;
    private SharedPreferences appPrefs;
    private String currentUserId;
    private String username;
    private String email;
    private boolean isEditMode = false;
    private int currentDefaultColor = 0xFF000000;

    // Activity result launcher for image picker
    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                        profileImage.setImageBitmap(bitmap);
                        saveProfileImage(bitmap);
                    } catch (IOException e) {
                        Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize preferences
        sharedPreferences = getSharedPreferences("NotesAppPrefs", MODE_PRIVATE);
        userPrefs = getSharedPreferences("UserData", MODE_PRIVATE);
        appPrefs = getSharedPreferences("AppSettings", MODE_PRIVATE);

        // Get user data
        currentUserId = sharedPreferences.getString("userId", null);
        username = sharedPreferences.getString("username", "User");
        email = userPrefs.getString(username + "_email", "user@example.com");

        if (currentUserId == null) {
            Toast.makeText(this, "User not found. Please login again.", Toast.LENGTH_SHORT).show();
            logoutUser();
            return;
        }

        // Initialize views
        initializeViews();

        // Set up listeners
        setupListeners();

        // Load user data
        loadUserData();

        // Calculate and display stats
        updateStats();
    }

    private void initializeViews() {
        // Top bar
        backBtn = findViewById(R.id.back_btn);
        saveProfileBtn = findViewById(R.id.save_profile_btn);

        // Profile header
        profileImage = findViewById(R.id.profile_image);
        editProfileImage = findViewById(R.id.edit_profile_image);
        profileUsername = findViewById(R.id.profile_username);
        profileEmail = findViewById(R.id.profile_email);
        editProfileBtn = findViewById(R.id.edit_profile_btn);

        // Stats
        notesCount = findViewById(R.id.notes_count);
        notesUsageBar = findViewById(R.id.notes_usage_bar);
        storageText = findViewById(R.id.storage_text);

        // Preferences
        defaultColorPreview = findViewById(R.id.default_color_preview);
        defaultNoteColorContainer = findViewById(R.id.default_note_color_container);
        autoSaveSwitch = findViewById(R.id.auto_save_switch);
        darkModeSwitch = findViewById(R.id.dark_mode_switch);

        // Account
        changePasswordBtn = findViewById(R.id.change_password_btn);
        exportNotesBtn = findViewById(R.id.export_notes_btn);
        deleteAccountBtn = findViewById(R.id.delete_account_btn);

        // App version
        appVersion = findViewById(R.id.app_version);
        appVersion.setText("Notes App v1.0.0");

        // Hide save button initially
        saveProfileBtn.setVisibility(View.GONE);
    }

    private void setupListeners() {
        // Back button
        backBtn.setOnClickListener(v -> onBackPressed());

        // Save profile button
        saveProfileBtn.setOnClickListener(v -> saveProfile());

        // Edit profile image
        editProfileImage.setOnClickListener(v -> pickImage());

        // Edit profile button
        editProfileBtn.setOnClickListener(v -> toggleEditMode());

        // Default note color
        defaultNoteColorContainer.setOnClickListener(v -> showColorPicker());

        // Switch listeners
        autoSaveSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                appPrefs.edit().putBoolean("autoSave", isChecked).apply());

        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            appPrefs.edit().putBoolean("darkMode", isChecked).apply();
            Toast.makeText(this, "App restart required for theme change", Toast.LENGTH_SHORT).show();
        });

        // Account options
        changePasswordBtn.setOnClickListener(v -> showChangePasswordDialog());
        exportNotesBtn.setOnClickListener(v -> exportNotes());
        deleteAccountBtn.setOnClickListener(v -> confirmDeleteAccount());
    }

    private void loadUserData() {
        // Set user details
        profileUsername.setText(username);
        profileEmail.setText(email);

        // Load profile image if exists
        String encodedImage = userPrefs.getString(currentUserId + "_profileImage", null);
        if (encodedImage != null) {
            byte[] decodedString = Base64.decode(encodedImage, Base64.DEFAULT);
            Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            profileImage.setImageBitmap(decodedBitmap);
        }

        // Load app preferences
        autoSaveSwitch.setChecked(appPrefs.getBoolean("autoSave", true));
        darkModeSwitch.setChecked(appPrefs.getBoolean("darkMode", true));

        // Load default note color
        currentDefaultColor = appPrefs.getInt("defaultNoteColor", 0xFF000000);
        defaultColorPreview.setBackgroundColor(currentDefaultColor);
    }

    private void updateStats() {
        ArrayList<Note> notes = loadNotes();

        // Count notes and calculate storage
        int totalNotes = notes.size();
        long totalStorageUsed = calculateNotesStorage(notes);

        notesCount.setText(String.valueOf(totalNotes));

        // Calculate percentage for progress bar
        long storageLimit = 100 * 1024 * 1024; // 100MB
        float usagePercentage = (float) totalStorageUsed / storageLimit;

        notesUsageBar.setScaleX(Math.min(usagePercentage, 1.0f));

        String usedMB = String.format("%.1f", totalStorageUsed / (1024.0 * 1024.0));
        storageText.setText("Notes: " + usedMB + " MB / 100 MB");
    }

    private long calculateNotesStorage(ArrayList<Note> notes) {
        long totalSize = 0;
        for (Note note : notes) {
            // Calculate approximate size of note content
            if (note.getContent() != null) {
                totalSize += note.getContent().length() * 2; // Approximate char size in bytes
            }
            if (note.getTitle() != null) {
                totalSize += note.getTitle().length() * 2;
            }
            // Add fixed overhead per note
            totalSize += 200; // Metadata overhead
        }
        return totalSize;
    }

    private void toggleEditMode() {
        isEditMode = !isEditMode;

        if (isEditMode) {
            // Enter edit mode
            saveProfileBtn.setVisibility(View.VISIBLE);
            editProfileBtn.setText("Cancel");

            // Make username and email editable
            showEditProfileDialog();
        } else {
            // Exit edit mode
            saveProfileBtn.setVisibility(View.GONE);
            editProfileBtn.setText("Edit Profile");

            // Reload user data to discard any unsaved changes
            loadUserData();
        }
    }

    private void showEditProfileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Profile");

        View view = getLayoutInflater().inflate(R.layout.dialog_edit_profile, null);
        EditText usernameEdit = view.findViewById(R.id.edit_username);
        EditText emailEdit = view.findViewById(R.id.edit_email);

        usernameEdit.setText(username);
        emailEdit.setText(email);

        builder.setView(view);
        builder.setPositiveButton("Save", (dialog, which) -> {
            String newUsername = usernameEdit.getText().toString().trim();
            String newEmail = emailEdit.getText().toString().trim();

            if (!newUsername.isEmpty() && !newEmail.isEmpty()) {
                // Update UI
                profileUsername.setText(newUsername);
                profileEmail.setText(newEmail);

                // Keep values for saving later
                username = newUsername;
                email = newEmail;
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void saveProfile() {
        // Save username and email
        SharedPreferences.Editor userEditor = userPrefs.edit();
        userEditor.putString(currentUserId + "_username", username);
        userEditor.putString(username + "_email", email);

        // Update login session
        SharedPreferences.Editor sessionEditor = sharedPreferences.edit();
        sessionEditor.putString("username", username);

        // Apply changes
        userEditor.apply();
        sessionEditor.apply();

        // Exit edit mode
        isEditMode = false;
        saveProfileBtn.setVisibility(View.GONE);
        editProfileBtn.setText("Edit Profile");

        Toast.makeText(this, "Profile saved successfully", Toast.LENGTH_SHORT).show();
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

    private void saveProfileImage(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
        byte[] imageBytes = baos.toByteArray();
        String encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);

        userPrefs.edit().putString(currentUserId + "_profileImage", encodedImage).apply();
    }

    private void showColorPicker() {
        final int[] colors = {
                0xFF000000, // Black
                0xFF2196F3, // Blue
                0xFF4CAF50, // Green
                0xFFFF9800, // Orange
                0xFFE91E63, // Pink
                0xFF9C27B0  // Purple
        };

        final String[] colorNames = {
                "Black", "Blue", "Green", "Orange", "Pink", "Purple"
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Default Note Color");
        builder.setSingleChoiceItems(colorNames, -1, (dialog, which) -> {
            currentDefaultColor = colors[which];
            defaultColorPreview.setBackgroundColor(currentDefaultColor);
            appPrefs.edit().putInt("defaultNoteColor", currentDefaultColor).apply();
            dialog.dismiss();
        });

        builder.show();
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Change Password");

        View view = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        EditText currentPassword = view.findViewById(R.id.current_password);
        EditText newPassword = view.findViewById(R.id.new_password);
        EditText confirmPassword = view.findViewById(R.id.confirm_password);

        builder.setView(view);
        builder.setPositiveButton("Save", null); // Set listener later to prevent auto-dismissal
        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        // Override the positive button's onclick listener
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            String currentPass = currentPassword.getText().toString();
            String newPass = newPassword.getText().toString();
            String confirmPass = confirmPassword.getText().toString();

            if (currentPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(ProfileActivity.this, "All fields are required", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPass.equals(confirmPass)) {
                Toast.makeText(ProfileActivity.this, "New passwords don't match", Toast.LENGTH_SHORT).show();
                return;
            }

            // Verify current password
            String savedPassword = userPrefs.getString(username + "_password", null);
            String hashedCurrentPassword = hashPassword(currentPass);

            if (savedPassword == null || !savedPassword.equals(hashedCurrentPassword)) {
                Toast.makeText(ProfileActivity.this, "Current password is incorrect", Toast.LENGTH_SHORT).show();
                return;
            }

            // Update password
            String hashedNewPassword = hashPassword(newPass);
            userPrefs.edit().putString(username + "_password", hashedNewPassword).apply();

            Toast.makeText(ProfileActivity.this, "Password changed successfully", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
    }

    private String hashPassword(String password) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Base64.encodeToString(encodedHash, Base64.NO_WRAP);
        } catch (java.security.NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void exportNotes() {
        ArrayList<Note> notes = loadNotes();

        if (notes.isEmpty()) {
            Toast.makeText(this, "No notes to export", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Create a JSON file
            Gson gson = new Gson();
            String jsonNotes = gson.toJson(notes);

            // Create directory in Documents folder
            File documentsDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOCUMENTS), "NotesApp");

            if (!documentsDir.exists()) {
                documentsDir.mkdirs();
            }

            // Create file
            String fileName = "notes_export_" + System.currentTimeMillis() + ".json";
            File exportFile = new File(documentsDir, fileName);

            // Write to file
            FileOutputStream fos = new FileOutputStream(exportFile);
            fos.write(jsonNotes.getBytes());
            fos.close();

            // Show success message with file path
            Toast.makeText(this, "Notes exported to " + exportFile.getAbsolutePath(),
                    Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Toast.makeText(this, "Failed to export notes: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void confirmDeleteAccount() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Account");
        builder.setMessage("Are you sure you want to delete your account? This action cannot be undone and all your notes will be permanently deleted.");
        builder.setPositiveButton("Delete", (dialog, which) -> {
            // Show a second confirmation dialog with password
            showDeleteAccountPasswordConfirmation();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showDeleteAccountPasswordConfirmation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm Password");

        View view = getLayoutInflater().inflate(R.layout.dialog_confirm_password, null);
        EditText passwordInput = view.findViewById(R.id.password_input);

        builder.setView(view);
        builder.setPositiveButton("Confirm", null); // Set listener later
        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            String password = passwordInput.getText().toString();

            if (password.isEmpty()) {
                Toast.makeText(ProfileActivity.this, "Please enter your password", Toast.LENGTH_SHORT).show();
                return;
            }

            // Verify password
            String savedPassword = userPrefs.getString(username + "_password", null);
            String hashedPassword = hashPassword(password);

            if (savedPassword == null || !savedPassword.equals(hashedPassword)) {
                Toast.makeText(ProfileActivity.this, "Incorrect password", Toast.LENGTH_SHORT).show();
                return;
            }

            // Delete user data
            deleteUserData();
            dialog.dismiss();
        });
    }

    private void deleteUserData() {
        // Delete user-related data
        SharedPreferences.Editor userEditor = userPrefs.edit();
        userEditor.remove(username + "_password");
        userEditor.remove(username + "_email");
        userEditor.remove(username + "_userId");
        userEditor.remove(currentUserId + "_username");
        userEditor.remove(currentUserId + "_profileImage");

        // Delete all notes
        ArrayList<Note> allNotes = loadAllNotes();
        ArrayList<Note> otherUsersNotes = new ArrayList<>();

        // Keep notes from other users
        for (Note note : allNotes) {
            if (!currentUserId.equals(note.getUserId())) {
                otherUsersNotes.add(note);
            }
        }

        // Save remaining notes
        saveAllNotes(otherUsersNotes);

        // Clear login session
        SharedPreferences.Editor sessionEditor = sharedPreferences.edit();
        sessionEditor.clear();
        sessionEditor.apply();

        // Apply changes
        userEditor.apply();

        Toast.makeText(this, "Account deleted successfully", Toast.LENGTH_SHORT).show();

        // Navigate to login screen
        Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void logoutUser() {
        // Clear login state
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isLoggedIn", false);
        editor.remove("username");
        editor.remove("userId");
        editor.apply();

        // Navigate back to login screen
        Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // Load notes for current user
    private ArrayList<Note> loadNotes() {
        SharedPreferences notesPrefs = getSharedPreferences("notes_prefs", MODE_PRIVATE);
        Gson gson = new Gson();
        String json = notesPrefs.getString("notes", null);
        Type type = new TypeToken<ArrayList<Note>>() {}.getType();

        if (json != null) {
            ArrayList<Note> allNotes = gson.fromJson(json, type);
            ArrayList<Note> userNotes = new ArrayList<>();

            for (Note note : allNotes) {
                if (currentUserId.equals(note.getUserId())) {
                    userNotes.add(note);
                }
            }

            return userNotes;
        }

        return new ArrayList<>();
    }

    // Load all notes regardless of user
    private ArrayList<Note> loadAllNotes() {
        SharedPreferences notesPrefs = getSharedPreferences("notes_prefs", MODE_PRIVATE);
        Gson gson = new Gson();
        String json = notesPrefs.getString("notes", null);
        Type type = new TypeToken<ArrayList<Note>>() {}.getType();

        if (json != null) {
            return gson.fromJson(json, type);
        }

        return new ArrayList<>();
    }

    // Save all notes
    private void saveAllNotes(ArrayList<Note> notes) {
        SharedPreferences notesPrefs = getSharedPreferences("notes_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = notesPrefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(notes);
        editor.putString("notes", json);
        editor.apply();
    }
}