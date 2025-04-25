package com.example.notesapp;

import android.graphics.Bitmap;

public class Drawing {
    private Bitmap bitmap;
    private String description;

    public Drawing(Bitmap bitmap, String description) {
        this.bitmap = bitmap;
        this.description = description;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
