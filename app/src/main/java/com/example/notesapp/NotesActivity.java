package com.example.notesapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

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
    private ImageButton cameraButton;
    private EditText contentEditText;
    private ProgressBar progressBar;

    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notes);

        mainLayout = findViewById(R.id.mainLayout);
        colorPickerLayout = findViewById(R.id.colorPickerLayout);
        colorIcon = findViewById(R.id.color_icon);
        cameraButton = findViewById(R.id.cameraButton);
        contentEditText = findViewById(R.id.contentEditText);
        progressBar = findViewById(R.id.progressBar);

        // Insets for edge-to-edge
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
            mainLayout.setBackgroundColor(ContextCompat.getColor(NotesActivity.this, R.color.red));
            colorPickerLayout.setVisibility(View.GONE);
        });

        colorYellow.setOnClickListener(view -> {
            mainLayout.setBackgroundColor(ContextCompat.getColor(NotesActivity.this, R.color.yellow));
            colorPickerLayout.setVisibility(View.GONE);
        });

        colorGreen.setOnClickListener(view -> {
            mainLayout.setBackgroundColor(ContextCompat.getColor(NotesActivity.this, R.color.green));
            colorPickerLayout.setVisibility(View.GONE);
        });

        colorBlue.setOnClickListener(view -> {
            mainLayout.setBackgroundColor(ContextCompat.getColor(NotesActivity.this, R.color.blue));
            colorPickerLayout.setVisibility(View.GONE);
        });

        colorBlack.setOnClickListener(view -> {
            mainLayout.setBackgroundColor(ContextCompat.getColor(NotesActivity.this, R.color.black));
            colorPickerLayout.setVisibility(View.GONE);
        });

        colorViolet.setOnClickListener(view -> {
            mainLayout.setBackgroundColor(ContextCompat.getColor(NotesActivity.this, R.color.violet));
            colorPickerLayout.setVisibility(View.GONE);
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
                    .url("https://3c7b-45-127-199-155.ngrok-free.app/extract-text")
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
}