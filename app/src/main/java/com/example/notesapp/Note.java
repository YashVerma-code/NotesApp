package com.example.notesapp;

import java.io.Serializable;
import java.util.Date;

public class Note implements Serializable {
    private String title;
    private String content;
    private Date dateCreated;
    private int backgroundColor;

    public Note(String title, String content, int backgroundColor) {
        this.title = title;
        this.content = content;
        this.backgroundColor = backgroundColor;
        this.dateCreated = new Date();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }
}