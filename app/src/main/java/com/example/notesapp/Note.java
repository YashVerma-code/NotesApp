package com.example.notesapp;

import android.graphics.Bitmap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

public class Note implements Serializable {
    private String title;
    private String content;
    private Date dateCreated;
    private int backgroundColor;

    // New fields
    private ArrayList<ChecklistItem> checkboxes;
    private transient ArrayList<Bitmap> imageList; // Bitmaps are not serializable

    private String userId; // New user ID field

    public Note(String title, String content, int backgroundColor,String userId) {
        this.title = title;
        this.content = content;
        this.backgroundColor = backgroundColor;
        this.dateCreated = new Date();
        this.checkboxes = new ArrayList<>();
        this.imageList = new ArrayList<>();
        this.userId = userId;
    }

    // Title
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    // Content
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    // Date
    public Date getDateCreated() { return dateCreated; }

    // Background Color
    public int getBackgroundColor() { return backgroundColor; }
    public void setBackgroundColor(int backgroundColor) { this.backgroundColor = backgroundColor; }

    // Checkboxes
    public ArrayList<ChecklistItem> getCheckboxes() { return checkboxes; }
    public void setCheckboxes(ArrayList<ChecklistItem> checkboxes) { this.checkboxes = checkboxes; }

    // Images
    public ArrayList<Bitmap> getImageList() { return imageList; }
    public void setImageList(ArrayList<Bitmap> imageList) { this.imageList = imageList; }

    // User ID
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}
