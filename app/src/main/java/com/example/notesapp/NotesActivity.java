package com.example.notesapp;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;

import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

// Add these imports at the top with your other imports
import android.app.AlertDialog;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;

// Inside NotesActivity class, add these variables

public class NotesActivity extends AppCompatActivity {
    private SensorManager sensorManager;
    private ShakeDetector shakeDetector;
    private ConstraintLayout mainLayout;
    private LinearLayout colorPickerLayout;
    private ImageView colorIcon;
    private View plusIcon;
    private LinearLayout imageContent;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Intent> drawingLauncher;
    private FrameLayout imageContainer;
    private LinearLayout checkboxContainer;
    private ImageView backBtn;
    // Title and content and color variable
    private EditText titleEditText;
    private EditText contentEditText;
    private int currentBackgroundColor;

    // Image Variable
    private ArrayList<Map<Bitmap,Boolean>> imageList = new ArrayList<>();

    // Drawing List
    private List<Drawing> drawingList = new ArrayList<>(); // Your list of drawings

    private int drawingCount = 0;

    // List to store checkbox data (text and checked status)
    private ArrayList<ChecklistItem> checkboxItems = new ArrayList<>();

    private Note currentNote;

    private String userId;
    private ImageButton cameraButton;
    private ProgressBar progressBar;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private Uri imageUri;
    private DrawingView canvasView;

    // Drawing feature constant
    private ImageView deleteBtn;

// Inside onCreate(), after other button initializations, add:

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);

        userId = getIntent().getStringExtra("userId");
        if (userId == null) {
            SharedPreferences sharedPreferences = getSharedPreferences("NotesAppPrefs", MODE_PRIVATE);
            userId = sharedPreferences.getString("userId", null);
            if (userId == null) {
                Toast.makeText(this, "User not found. Please login again.", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        }

        mainLayout = findViewById(R.id.mainLayout);
        colorPickerLayout = findViewById(R.id.colorPickerLayout);
        colorIcon = findViewById(R.id.color_icon);
        cameraButton = findViewById(R.id.cameraButton);
        progressBar = findViewById(R.id.progressBar);
        titleEditText = findViewById(R.id.titleEditText);
        contentEditText = findViewById(R.id.contentEditText);
        deleteBtn = findViewById(R.id.delete_btn);
        deleteBtn.setOnClickListener(view->{
            showDeleteConfirmationDialog();
        });
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
                Log.d("NotesApp", "Editing note with color: " + currentBackgroundColor);
            }
        }

        imageContent = findViewById(R.id.imageContent);
        imageContainer = findViewById(R.id.imageContainer);
        checkboxContainer = findViewById(R.id.checkbox_container);
        backBtn = findViewById(R.id.back_btn);

        titleEditText = findViewById(R.id.titleEditText);
        contentEditText = findViewById(R.id.contentEditText);

        backBtn.setOnClickListener(view -> {
            saveNote();
            startActivity(new Intent(NotesActivity.this, Home.class));
            finish();
        });

        // Initialize the horizontal scroll view with empty content
        if (imageContent.getChildCount() > 0) {
            imageContainer.setVisibility(View.VISIBLE);
            imageContent.removeAllViews();
        } else {
            imageContainer.setVisibility(View.GONE);
        }

        ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri selectedImage = result.getData().getData();
                        extractTextFromImage(selectedImage);
                    }
                }
        );

        cameraButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        // Toggle color picker visibility
        plusIcon = findViewById(R.id.plus_icon);
        plusIcon.setOnClickListener(view -> showBottomSheet());

        findViewById(R.id.colorRed).setOnClickListener(view -> setColor(R.color.red));
        findViewById(R.id.colorYellow).setOnClickListener(view -> setColor(R.color.yellow));
        findViewById(R.id.colorGreen).setOnClickListener(view -> setColor(R.color.green));
        findViewById(R.id.colorBlue).setOnClickListener(view -> setColor(R.color.blue));
        findViewById(R.id.colorBlack).setOnClickListener(view -> setColor(R.color.black));
        findViewById(R.id.colorViolet).setOnClickListener(view -> setColor(R.color.violet));

        colorIcon.setOnClickListener(view -> {
            colorPickerLayout.setVisibility(
                    colorPickerLayout.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        });

        // Add save button functionality
        ImageView saveBtn = findViewById(R.id.save_btn);
        saveBtn.setOnClickListener(view -> {
            saveNote();
            Toast.makeText(NotesActivity.this, "Note saved", Toast.LENGTH_SHORT).show();
            finish();
        });

        // Add edited textview click listener
        findViewById(R.id.editedTextView).setOnClickListener(view -> {
            saveNote();
            navigateToHome();
        });

        setupActivityResultLaunchers();

        // Handle back press with the new API
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                saveNote();
                finish();
            }
        });
        setupShakeDetector();
    }

    private void showDeleteConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Note");
        builder.setMessage("Are you sure you want to delete this note?");

        builder.setPositiveButton("Delete", (dialog, which) -> {
            deleteNote();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.dismiss();
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void deleteNote() {
        // Only attempt to delete if this is an existing note
        if (currentNote != null) {
            ArrayList<Note> notesList = loadNotes();
            boolean removed = false;

            for (int i = 0; i < notesList.size(); i++) {
                Note note = notesList.get(i);
                if (note != null && note.getDateCreated() != null &&
                        currentNote.getDateCreated() != null &&
                        note.getDateCreated().equals(currentNote.getDateCreated())) {
                    notesList.remove(i);
                    removed = true;
                    break;
                }
            }

            if (removed) {
                // Save the updated notes list
                saveNotes(notesList);
                Toast.makeText(NotesActivity.this, "Note deleted", Toast.LENGTH_SHORT).show();

                // Return to the Home activity
                navigateToHome();
            } else {
                Toast.makeText(NotesActivity.this, "Failed to delete note", Toast.LENGTH_SHORT).show();
            }
        } else {
            // This is a new unsaved note
            Toast.makeText(NotesActivity.this, "Note discarded", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    private void setupShakeDetector() {
        // Get the sensor manager
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        // Initialize the shake detector
        shakeDetector = new ShakeDetector(new ShakeDetector.OnShakeListener() {
            @Override
            public void onShake() {
                // Show confirmation dialog on main thread
                runOnUiThread(() -> showClearNoteConfirmationDialog());
            }
        });
    }

    // Add this method to show the clear confirmation dialog
    private void showClearNoteConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Clear Note Content");
        builder.setMessage("Are you sure you want to clear this note's content?");

        builder.setPositiveButton("Clear", (dialog, which) -> {
            // Clear the note content but keep the note
            clearNoteContent();
            Toast.makeText(NotesActivity.this, "Note content cleared", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.dismiss();
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // Add this method to clear the note content
    private void clearNoteContent() {
        // Clear EditText fields
        contentEditText.setText("");

        // If there's a current note, update it with empty content
        if (currentNote != null) {
            ArrayList<Note> notesList = loadNotes();

            for (int i = 0; i < notesList.size(); i++) {
                Note note = notesList.get(i);
                if (note != null && note.getDateCreated() != null &&
                        currentNote.getDateCreated() != null &&
                        note.getDateCreated().equals(currentNote.getDateCreated())) {
                    note.setContent("");
                    currentNote.setContent("");
                    break;
                }
            }

            // Save the updated notes list
            saveNotes(notesList);
        }

        // Clear any images or checkboxes if they exist
        if (imageContent != null) {
            imageContent.removeAllViews();
            imageContainer.setVisibility(View.GONE);
        }

        if (checkboxContainer != null) {
            checkboxContainer.removeAllViews();
        }

        // Clear image and drawing lists
        imageList.clear();
        drawingList.clear();
        drawingCount = 0;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register the sensor listener
        if (sensorManager != null) {
            sensorManager.registerListener(shakeDetector,
                    sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        // Unregister the sensor listener to save battery
        if (sensorManager != null) {
            sensorManager.unregisterListener(shakeDetector);
        }
        super.onPause();
    }

    private byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[16384];
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }

    private void extractTextFromImage(Uri imageUri) {
        showProgressBar(true); // Show loading spinner

        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            byte[] imageBytes = getBytes(inputStream);

            OkHttpClient client = new OkHttpClient();

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("image", "image.jpg",
                            RequestBody.create(MediaType.parse("image/jpeg"), imageBytes))
                    .build();

            Request request = new Request.Builder()
                    .url("https://b794-2402-3a80-749-3cc0-f9a3-9b75-fb36-9328.ngrok-free.app/extract-text")
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        showProgressBar(false);
                        Toast.makeText(NotesActivity.this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseText = response.body().string();
                    runOnUiThread(() -> {
                        showProgressBar(false);
                        try {
                            JSONObject jsonObject = new JSONObject(responseText);
                            String extractedText = jsonObject.getString("text");
                            String existingText = contentEditText.getText().toString();
                            contentEditText.setText(existingText + "\n" + extractedText);
                        } catch (JSONException e) {
                            Toast.makeText(NotesActivity.this, "Invalid response format", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });

        } catch (Exception e) {
            showProgressBar(false);
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showProgressBar(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        contentEditText.setEnabled(!show);
    }

    private void setColor(int colorResId) {
        currentBackgroundColor = ContextCompat.getColor(NotesActivity.this, colorResId);
        mainLayout.setBackgroundColor(currentBackgroundColor);
        colorPickerLayout.setVisibility(View.GONE);

        Log.d("NotesApp", "Color set to: " + currentBackgroundColor);

        int color = ContextCompat.getColor(NotesActivity.this, colorResId);
        currentBackgroundColor = color;
    }

    private void setupActivityResultLaunchers() {
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null && data.getExtras() != null) {
                            Bitmap imageBitmap = (Bitmap) data.getExtras().get("data");
                            addImageToNote(imageBitmap,false);
                        }
                    }
                });

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null && data.getData() != null) {
                            Uri imageUri = data.getData();
                            try {
                                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                                addImageToNote(bitmap,false);
                            } catch (IOException e) {
                                e.printStackTrace();
                                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });

        // Register drawing activity launcher
        // Register drawing activity launcher
        // Modify the drawingLauncher result handling in setupActivityResultLaunchers()
        drawingLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        byte[] drawingBytes = result.getData().getByteArrayExtra("drawing_image");
                        String newDescription = result.getData().getStringExtra("description");
                        String originalDescription = result.getData().getStringExtra("original_description");

                        if (drawingBytes != null) {
                            Bitmap drawingBitmap = BitmapFactory.decodeByteArray(drawingBytes, 0, drawingBytes.length);

                            // Find if we're editing an existing drawing
                            boolean isUpdatingExisting = false;
                            int existingIndex = -1;

                            // First try to find by original description
                            if (originalDescription != null) {
                                for (int i = 0; i < drawingList.size(); i++) {
                                    if (drawingList.get(i).getDescription().equals(originalDescription)) {
                                        existingIndex = i;
                                        isUpdatingExisting = true;
                                        break;
                                    }
                                }
                            }

                            if (isUpdatingExisting) {
                                // Update existing drawing bitmap and description
                                Drawing existingDrawing = drawingList.get(existingIndex);
                                existingDrawing.setBitmap(drawingBitmap);

                                // Update description if changed
                                if (newDescription != null && !originalDescription.equals(newDescription)) {
                                    existingDrawing.setDescription(newDescription);
                                    updateDrawingHyperlinkInContent(originalDescription, newDescription);
                                }

                                // Update the image in imageContent (horizontal scrollview)
                                // Find and update the corresponding ImageView
                                for (int i = 0; i < imageContent.getChildCount(); i++) {
                                    View child = imageContent.getChildAt(i);
                                    ImageView imageView = child.findViewById(R.id.itemImage);
                                    BitmapDrawable drawable = (BitmapDrawable) imageView.getDrawable();
                                    if (drawable != null) {
                                        Bitmap currentBitmap = drawable.getBitmap();
                                        // Check if this is the image we need to update
                                        // You can use the position/index instead of bitmap comparison
                                        if (i == existingIndex) {
                                            imageView.setImageBitmap(drawingBitmap);
                                            break;
                                        }
                                    }
                                }
                            } else {
                                // This is a new drawing
                                String finalDescription = (newDescription != null && !newDescription.isEmpty()) ?
                                        newDescription : "Drawing " + (drawingCount + 1);

                                // Create and add new drawing
                                Drawing newDrawing = new Drawing(drawingBitmap, finalDescription);
                                drawingList.add(newDrawing);
                                addImageToNote(drawingBitmap, true);
                            }
                        }
                    }
                });


    }

    private void updateDrawingHyperlinkInContent(String oldDescription, String newDescription) {
        // Get the current content with all existing spans
        Editable content = contentEditText.getText();
        String contentStr = content.toString();

        // Look for the old link format without brackets
        int startIndex = contentStr.indexOf("\n" + oldDescription + "\n");

        if (startIndex != -1) {
            startIndex += 1; // Skip the first newline
            int endIndex = startIndex + oldDescription.length();

            // Replace the text without removing the span
            content.replace(startIndex, endIndex, newDescription);

            // Update the clickable span
            ClickableSpan[] spans = content.getSpans(startIndex, startIndex + newDescription.length(), ClickableSpan.class);
            if (spans.length > 0) {
                // Remove old span
                content.removeSpan(spans[0]);

                // Create new clickable span with updated description
                ClickableSpan clickableSpan = new ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        // Find the drawing by new name
                        for (Drawing drawing : drawingList) {
                            if (drawing.getDescription().equals(newDescription)) {
                                openDrawingInCanvas(drawing.getBitmap());
                                break;
                            }
                        }
                    }

                    @Override
                    public void updateDrawState(TextPaint ds) {
                        super.updateDrawState(ds);
                        // Change link color to white
                        ds.setColor(Color.WHITE);
                        ds.setUnderlineText(true);
                    }
                };

                // Apply the new span
                content.setSpan(clickableSpan, startIndex, startIndex + newDescription.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                // Update color span to white
                content.setSpan(new ForegroundColorSpan(Color.WHITE),
                        startIndex, startIndex + newDescription.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    private void showBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(NotesActivity.this);
        View bottomSheetView = LayoutInflater.from(getApplicationContext())
                .inflate(R.layout.bottom_sheet_layout, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        TextView takePhoto = bottomSheetView.findViewById(R.id.takePhoto);
        TextView addImage = bottomSheetView.findViewById(R.id.addImage);
        TextView drawing = bottomSheetView.findViewById(R.id.drawing);
        TextView addList = bottomSheetView.findViewById(R.id.listOption);

        takePhoto.setOnClickListener(view -> {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                cameraLauncher.launch(takePictureIntent);
            }
            bottomSheetDialog.dismiss();
        });

        addImage.setOnClickListener(view -> {
            Intent pickPhoto = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(pickPhoto);
            bottomSheetDialog.dismiss();
        });

        drawing.setOnClickListener(view -> {
            Intent drawingIntent = new Intent(NotesActivity.this, DrawingActivity.class);
            drawingLauncher.launch(drawingIntent);
            bottomSheetDialog.dismiss();
        });

        addList.setOnClickListener(view -> {
            addNewCheckboxItem();
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }

    private void addNewCheckboxItem() {
        // Inflate the checkbox item layout
        View checkboxItemView = getLayoutInflater().inflate(R.layout.list_item_checkbox, null);
        CheckBox checkBox = checkboxItemView.findViewById(R.id.checkbox);
        EditText itemEditText = checkboxItemView.findViewById(R.id.item_edit_text);
        ImageButton removeButton = checkboxItemView.findViewById(R.id.remove_button);

        // Add listener to detect state changes
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Store checkbox data
            ChecklistItem checkboxItem = new ChecklistItem(itemEditText.getText().toString(), isChecked);
            checkboxItems.add(checkboxItem);

            // If checkbox is checked, strike-through the text
            if (isChecked) {
                itemEditText.setPaintFlags(itemEditText.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                itemEditText.setPaintFlags(itemEditText.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
            }
        });

        // Set placeholder text and request focus
        itemEditText.setHint("List item");
        itemEditText.requestFocus();

        // Add the remove button functionality
        removeButton.setOnClickListener(v -> {
            checkboxContainer.removeView(checkboxItemView);
        });

        // Add key listener to handle Enter key press
        itemEditText.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                // Create a new checkbox item below the current one
                int currentIndex = checkboxContainer.indexOfChild(checkboxItemView);
                addNewCheckboxItemAt(currentIndex + 1);
                return true;
            }
            // Remove from checkboxItems list
            checkboxItems.removeIf(item -> item.getItemText().equals(itemEditText.getText().toString()));
            return false;
        });

        // Add the view to the container
        checkboxContainer.addView(checkboxItemView);
    }

    private void saveNote() {
        String title = titleEditText.getText().toString().trim();
        String content = contentEditText.getText().toString().trim();

        if (title.isEmpty() && content.isEmpty()) {
            // Don't save empty notes
            Log.d("NotesApp", "Not saving empty note");
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
                    Log.d("NotesApp", "Updated existing note with color: " + currentBackgroundColor);
                    break;
                }
            }
        } else {
            // Create new note
            Note newNote = new Note(title, content, currentBackgroundColor, userId);
            notesList.add(0, newNote); // Add to beginning of list
            Log.d("NotesApp", "Created new note with color: " + currentBackgroundColor);
        }

        // Save the updated notes list
        saveNotes(notesList);
        Toast.makeText(this, "Note saved", Toast.LENGTH_SHORT).show();
    }

    private void navigateToHome() {
        Intent intent = new Intent(NotesActivity.this, Home.class);
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
        Log.d("NotesApp", "Saved " + notesList.size() + " notes to preferences");
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

    private void addNewCheckboxItemAt(int position) {
        // Inflate the checkbox item layout
        View checkboxItemView = getLayoutInflater().inflate(R.layout.list_item_checkbox, null);
        CheckBox checkBox = checkboxItemView.findViewById(R.id.checkbox);
        EditText itemEditText = checkboxItemView.findViewById(R.id.item_edit_text);
        ImageButton removeButton = checkboxItemView.findViewById(R.id.remove_button);

        // Add listener to detect state changes
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // If checkbox is checked
            if (isChecked) {
                // Change the checkbox tint to black when checked
                checkBox.setButtonTintList(android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(NotesActivity.this, android.R.color.black)));

                // You can also cross out or style the text if needed
                itemEditText.setPaintFlags(itemEditText.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                // Change back to white when unchecked
                checkBox.setButtonTintList(android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(NotesActivity.this, android.R.color.white)));

                // Remove strikethrough if applied
                itemEditText.setPaintFlags(itemEditText.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
            }
        });

        // Set placeholder text and request focus
        itemEditText.setHint("List item");
        itemEditText.requestFocus();

        // Add the remove button functionality
        removeButton.setOnClickListener(v -> {
            checkboxContainer.removeView(checkboxItemView);
        });

        // Add key listener to handle Enter key press
        itemEditText.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                // Create a new checkbox item below the current one
                int currentIndex = checkboxContainer.indexOfChild(checkboxItemView);
                addNewCheckboxItemAt(currentIndex + 1);
                return true;
            }
            return false;
        });

        // Add the view to the container at the specified position
        checkboxContainer.addView(checkboxItemView, position);
    }

    private void addTextWithHyperlink(String text) {
        SpannableString spannableString = new SpannableString(text);

        // Define the text "Drawing 1" as clickable
        int startIndex = text.indexOf("Drawing 1");
        int endIndex = startIndex + "Drawing 1".length();

        if (startIndex != -1) {
            // Set the clickable part of the text
            spannableString.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View widget) {
                    // When the "Drawing 1" link is clicked, open the drawing
                    openDrawingInCanvas(getDrawingByIndex(drawingCount).getBitmap()); // Assuming 1 corresponds to Drawing 1
                }
            }, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            // Optional: Change the color or style of the link to look like a hyperlink
            spannableString.setSpan(new ForegroundColorSpan(Color.BLUE), startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannableString.setSpan(new UnderlineSpan(), startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        // Set the text to a TextView
        contentEditText.setText(spannableString);
        contentEditText.setMovementMethod(LinkMovementMethod.getInstance());  // Enables clickable links
    }

    private void openDrawingInCanvas(Bitmap bitmap) {
        // Find the drawing description based on bitmap
        String description = null;

        for (Drawing drawing : drawingList) {
            // Note: sameAs can be computationally expensive
            if (drawing.getBitmap().sameAs(bitmap)) {
                description = drawing.getDescription();
                break;
            }
        }

        // If we couldn't find a matching drawing, create a new one
        if (description == null) {
            drawingCount++;
            description = "Drawing " + drawingCount;
        }

        Intent intent = new Intent(NotesActivity.this, DrawingActivity.class);
        intent.putExtra("isDrawing", true);

        // Compress bitmap to byte array for transfer
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();

        intent.putExtra("drawing_bytes", byteArray);
        intent.putExtra("description", description);
        intent.putExtra("original_description", description); // Add this to track which drawing is being edited

        drawingLauncher.launch(intent);
    }


    private Drawing getDrawingByIndex(int drawingIndex) {
        if (drawingIndex >= 0 && drawingIndex < drawingList.size()) {
            return drawingList.get(drawingIndex);
        }
        return null;  // Return null if the index is invalid
    }


    private boolean imageExists(Bitmap bitmap)
    {
        boolean exists = false;

        // Loop through the imageList and check if the Bitmap is already present
        for (Map<Bitmap, Boolean> map : imageList) {
            if (map.containsKey(bitmap)) {
                exists = true;
                break;
            }
        }
        return exists;
    }

    private void addImageToNote(Bitmap bitmap, boolean isDrawing) {
        // Make container visible if needed
        imageContainer.setVisibility(View.VISIBLE);

        // Inflate the custom image item layout
        View imageItemView = LayoutInflater.from(this).inflate(R.layout.image_item, imageContent, false);
        ImageView imageView = imageItemView.findViewById(R.id.itemImage);
        View drawingIndicator = imageItemView.findViewById(R.id.drawingIndicator);

        // Set the image bitmap
        imageView.setImageBitmap(bitmap);

        // Show drawing indicator if it's a drawing
        if (isDrawing) {
            drawingIndicator.setVisibility(View.VISIBLE);
        } else {
            drawingIndicator.setVisibility(View.GONE);
        }

        // Apply fade-in animation
        imageItemView.setAlpha(0f);
        imageItemView.animate().alpha(1f).setDuration(300).start();

        final boolean finalIsDrawing = isDrawing;

        // Set proper click listener for this image
        imageItemView.setOnClickListener(v -> {
            if (finalIsDrawing) {
                // Retrieve bitmap directly from ImageView
                BitmapDrawable drawable = (BitmapDrawable) imageView.getDrawable();
                if (drawable != null) {
                    Bitmap clickedBitmap = drawable.getBitmap();
                    openDrawingInCanvas(clickedBitmap);
                }
            } else {
                showImageOptions(imageItemView, bitmap);
            }
        });

        // Add to main container
        imageContent.addView(imageItemView);

        // Add to tracking lists
        Map<Bitmap, Boolean> newMap = new HashMap<>();
        newMap.put(bitmap, isDrawing);
        imageList.add(newMap);

        // If it's a drawing, add to drawing list and create hyperlink
        if (isDrawing) {
            // Create a default description for new drawings
            String description = "";

            // Check if this drawing already exists in drawingList (when returned from DrawingActivity)
            boolean found = false;
            for (Drawing drawing : drawingList) {
                if (drawing.getBitmap().sameAs(bitmap)) {
                    description = drawing.getDescription();
                    found = true;
                    break;
                }
            }

            // If not found, create a new drawing with a default name
            if (!found) {
                drawingCount++;
                description = "Drawing " + drawingCount;
                Drawing newDrawing = new Drawing(bitmap, description);
                drawingList.add(newDrawing);
            }

            // Add hyperlink to content using the actual description
            insertDrawingHyperlinkToContent(description);
        }
    }



    private void insertDrawingHyperlinkToContent(String drawingName) {
        // Get the current content with all existing spans
        Editable currentContent = contentEditText.getText();
        int currentPosition = contentEditText.getSelectionStart();
        if (currentPosition == -1) currentPosition = currentContent.length();

        // Create the link text without brackets
        String linkText = "\n" + drawingName + "\n";

        // Insert the text without disturbing existing spans
        contentEditText.getText().insert(currentPosition, linkText);

        // Now add the span to just the new link text
        int linkStart = currentPosition + 1; // +1 to skip the first newline
        int linkEnd = linkStart + drawingName.length();

        // Store a reference to the drawing
        final String finalDrawingName = drawingName;

        // Create the clickable span with white text
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                // Find the drawing by name
                for (int i = 0; i < drawingList.size(); i++) {
                    if (drawingList.get(i).getDescription().equals(finalDrawingName)) {
                        openDrawingInCanvas(drawingList.get(i).getBitmap());
                        break;
                    }
                }
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                // Change link color to white
                ds.setColor(Color.WHITE);
                ds.setUnderlineText(true);
            }
        };

        // Apply the spans to just the new text
        contentEditText.getText().setSpan(clickableSpan, linkStart, linkEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        contentEditText.getText().setSpan(new ForegroundColorSpan(Color.WHITE), linkStart, linkEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Make sure links are clickable
        contentEditText.setMovementMethod(LinkMovementMethod.getInstance());

        // Position cursor after the inserted text
        contentEditText.setSelection(linkEnd + 1);
    }



    private void showImageOptions(View imageItemView, Bitmap bitmap) {
        // Create a modern bottom sheet with Material Design styling
        BottomSheetDialog optionsDialog = new BottomSheetDialog(NotesActivity.this);
        View optionsView = LayoutInflater.from(getApplicationContext())
                .inflate(R.layout.image_options_layout, null);
        optionsDialog.setContentView(optionsView);

        TextView viewFullScreen = optionsView.findViewById(R.id.viewFullScreen);
        TextView removeImage = optionsView.findViewById(R.id.removeImage);

        viewFullScreen.setOnClickListener(view -> {
            showImageFullScreen(bitmap);
            optionsDialog.dismiss();
        });

        removeImage.setOnClickListener(view -> {
            // Fade out animation before removal
            imageItemView.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction(() -> {
                        imageContent.removeView(imageItemView);

                        // Remove from imageList
                        for (int i = 0; i < imageList.size(); i++) {
                            Map<Bitmap, Boolean> item = imageList.get(i);
                            if (item.containsKey(bitmap)) {
                                imageList.remove(i);
                                break;
                            }
                        }

                        // Hide container if no more images
                        if (imageContent.getChildCount() == 0) {
                            imageContainer.setVisibility(View.GONE);
                        }
                    })
                    .start();

            optionsDialog.dismiss();
        });

        optionsDialog.show();
    }

    private void showImageFullScreen(Bitmap bitmap) {
        Dialog fullScreenDialog = new Dialog(this, R.style.FullScreenImageDialogTheme);

        View fullScreenView = LayoutInflater.from(this).inflate(R.layout.fullscreen_image, null);
        ImageView fullScreenImage = fullScreenView.findViewById(R.id.fullscreen_image);
        ImageView closeButton = fullScreenView.findViewById(R.id.close_button);

        fullScreenImage.setImageBitmap(bitmap);

        closeButton.setOnClickListener(v -> {
            // Add a nice fade out animation
            fullScreenView.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction(() -> fullScreenDialog.dismiss())
                    .start();
        });

        fullScreenDialog.setContentView(fullScreenView);

        // Start with fade in animation
        fullScreenView.setAlpha(0f);
        fullScreenDialog.show();
        fullScreenView.animate().alpha(1f).setDuration(300).start();
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
