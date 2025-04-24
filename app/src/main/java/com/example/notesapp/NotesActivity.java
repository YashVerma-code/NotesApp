// MainActivity.java
package com.example.notesapp;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;

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


public class NotesActivity extends AppCompatActivity {

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
    private ArrayList<Bitmap> imageList = new ArrayList<>();

    // List to store checkbox data (text and checked status)
    private ArrayList<ChecklistItem> checkboxItems = new ArrayList<>();

    private Note currentNote;

    private String userId;
    private ImageButton cameraButton;
    private ProgressBar progressBar;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private Uri imageUri;

    // Drawing feature constant
    private static final int DRAWING_REQUEST_CODE = 101;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);

        SharedPreferences userPrefs = getSharedPreferences("UserData", MODE_PRIVATE);
        userId = userPrefs.getString("username", "null"); // Use a default or handle null if needed

        mainLayout = findViewById(R.id.mainLayout);
        colorPickerLayout = findViewById(R.id.colorPickerLayout);
        colorIcon = findViewById(R.id.color_icon);
        cameraButton = findViewById(R.id.cameraButton);
        progressBar = findViewById(R.id.progressBar);
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
                    .url("https://d818-2402-3a80-752-9077-78f7-2826-5943-508e.ngrok-free.app/extract-text")
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
                            addImageToNote(imageBitmap);
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
                                addImageToNote(bitmap);
                            } catch (IOException e) {
                                e.printStackTrace();
                                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });

        // Register drawing activity launcher
        drawingLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        byte[] drawingBytes = result.getData().getByteArrayExtra("drawing_image");
                        if (drawingBytes != null) {
                            Bitmap drawingBitmap = BitmapFactory.decodeByteArray(drawingBytes, 0, drawingBytes.length);
                            addImageToNote(drawingBitmap);
                        }
                    }
                });
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

    private void addImageToNote(Bitmap bitmap) {
        if (!imageList.contains(bitmap)) {
            imageList.add(bitmap);
        }

        ImageView imageView = new ImageView(this);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                dpToPx(200), dpToPx(200));
        layoutParams.setMargins(dpToPx(4), 0, dpToPx(4), 0);
        imageView.setLayoutParams(layoutParams);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setImageBitmap(bitmap);

        imageView.setOnClickListener(v -> {
            showImageOptions(imageView, bitmap);
        });

        imageContent.addView(imageView);

        // Make sure the image area is visible (in case it was previously hidden)
        imageContainer.setVisibility(View.VISIBLE);
        if (imageContent.getParent().getParent() instanceof View) {
            View scrollViewParent = (View) imageContent.getParent().getParent();
            scrollViewParent.setVisibility(View.VISIBLE);
        }
    }

    private void showImageOptions(ImageView imageView, Bitmap bitmap) {
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
            imageContent.removeView(imageView);
            imageList.remove(bitmap);
            if (imageContent.getChildCount() == 0) {
                imageContainer.setVisibility(View.GONE);
                View scrollViewParent = (View) imageContent.getParent().getParent();
                scrollViewParent.setVisibility(View.GONE);
            }
            optionsDialog.dismiss();
        });

        optionsDialog.show();
    }

    private void showImageFullScreen(Bitmap bitmap) {
        Dialog fullScreenDialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        ImageView fullScreenImage = new ImageView(this);
        fullScreenImage.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        fullScreenImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        fullScreenImage.setImageBitmap(bitmap);

        fullScreenImage.setOnClickListener(v -> fullScreenDialog.dismiss());
        fullScreenDialog.setContentView(fullScreenImage);
        fullScreenDialog.show();
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}