package com.example.notesapp;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {

    private ArrayList<Note> notesList;
    private Context context;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private OnNoteDeleteListener onNoteDeleteListener;

    public interface OnNoteDeleteListener {
        void onDeleteNote(Note note);
    }

    public NotesAdapter(Context context, ArrayList<Note> notesList) {
        this.context = context;
        this.notesList = notesList;
    }

    public void setOnNoteDeleteListener(OnNoteDeleteListener listener) {
        this.onNoteDeleteListener = listener;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.note_card_layout, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = notesList.get(position);

        holder.cardTitle.setText(note.getTitle());
        holder.cardContent.setText(note.getContent());
        holder.cardDate.setText(dateFormat.format(note.getDateCreated()));
        holder.container.setBackgroundColor(note.getBackgroundColor());

        // Set up click listener for the note card
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, NotesActivity.class);
            intent.putExtra("note", note);
            context.startActivity(intent);
        });

        // Remove the long press listener - we'll use ItemTouchHelper for drag functionality instead
    }

    @Override
    public int getItemCount() {
        return notesList.size();
    }

    public void updateNotes(ArrayList<Note> newNotes) {
        this.notesList = newNotes;
        notifyDataSetChanged();
    }

    // Method to get note at specific position
    public Note getNoteAt(int position) {
        if (position >= 0 && position < notesList.size()) {
            return notesList.get(position);
        }
        return null;
    }

    // Method to move a note within the list
    public void moveNote(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(notesList, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(notesList, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
    }

    // Method to remove a note
    public void removeNote(int position) {
        if (position >= 0 && position < notesList.size()) {
            notesList.remove(position);
            notifyItemRemoved(position);
        }
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView cardTitle, cardContent, cardDate;
        LinearLayout container;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            cardTitle = itemView.findViewById(R.id.cardTitle);
            cardContent = itemView.findViewById(R.id.cardContent);
            cardDate = itemView.findViewById(R.id.cardDate);
            container = itemView.findViewById(R.id.noteCardContainer);
        }
    }
}