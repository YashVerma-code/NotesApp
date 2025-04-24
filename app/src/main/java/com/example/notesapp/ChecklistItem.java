package com.example.notesapp;

public class ChecklistItem {
    private String itemText;
    private boolean isChecked;

    public ChecklistItem(String itemText, boolean isChecked) {
        this.itemText = itemText;
        this.isChecked = isChecked;
    }

    public String getItemText() {
        return itemText;
    }

    public void setItemText(String itemText) {
        this.itemText = itemText;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }
}
