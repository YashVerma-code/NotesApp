package com.example.notesapp;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

public class NoteItemTouchHelper extends ItemTouchHelper.Callback {

    private final NotesAdapter adapter;
    private final OnNoteActionListener listener;

    // Threshold for deletion when dragging down (in pixels)
    private final int deleteThreshold;

    public interface OnNoteActionListener {
        void onNoteDeleted(Note note);
        void onNoteMoved(int fromPosition, int toPosition);
    }

    public NoteItemTouchHelper(NotesAdapter adapter, OnNoteActionListener listener, int deleteThreshold) {
        this.adapter = adapter;
        this.listener = listener;
        this.deleteThreshold = deleteThreshold;
    }

    @Override
    public boolean isLongPressDragEnabled() {
        // Enable drag on long press
        return true;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        // Disable swipe to focus on drag functionality
        return false;
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        int dragFlags;

        if (recyclerView.getLayoutManager() instanceof StaggeredGridLayoutManager) {
            // For grid layout, allow dragging in all directions
            dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
        } else {
            // For linear layout, allow dragging up and down
            dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        }

        // No swipe flags
        return makeMovementFlags(dragFlags, 0);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                          @NonNull RecyclerView.ViewHolder target) {
        // Get positions
        int fromPosition = viewHolder.getAdapterPosition();
        int toPosition = target.getAdapterPosition();

        // Notify the adapter to move the item
        if (listener != null) {
            listener.onNoteMoved(fromPosition, toPosition);
        }

        return true;
    }

    // Track the initial Y position for determining downward drag distance
    private float initialY = 0;

    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
        super.onSelectedChanged(viewHolder, actionState);

        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && viewHolder != null) {
            // Record the initial Y position when drag starts
            initialY = viewHolder.itemView.getY();
        }
    }

    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);

        // Check if the item was dragged down more than the threshold
        float currentY = viewHolder.itemView.getY();
        float dragDistance = currentY - initialY;

        if (dragDistance > deleteThreshold) {
            // Get the note at the current position
            int position = viewHolder.getAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                Note noteToDelete = adapter.getNoteAt(position);
                if (noteToDelete != null && listener != null) {
                    listener.onNoteDeleted(noteToDelete);
                }
            }
        }
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        // Not used as swiping is disabled
    }
}