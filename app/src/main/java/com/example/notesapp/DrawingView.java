package com.example.notesapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.Stack;

public class DrawingView extends View {

    // Enum for different drawing tools
    public enum Tool {
        PENCIL,
        ERASER
    }

    private Path path;
    private Paint drawPaint, canvasPaint;
    private int paintColor = Color.BLACK;
    private Canvas drawCanvas;
    private Bitmap canvasBitmap;
    private float brushSize = 10;
    private Tool currentTool = Tool.PENCIL;

    // For undo/redo functionality
    private Stack<Path> paths = new Stack<>();
    private Stack<Paint> paints = new Stack<>();
    private Stack<Path> redoPaths = new Stack<>();
    private Stack<Paint> redoPaints = new Stack<>();

    private Drawing currentDrawing;

    public DrawingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setupDrawing();
    }

    public DrawingView(Context context) {
        super(context);
    }


    public void setDrawing(Drawing drawing) {
        // Set the drawing (Bitmap and description)
        this.currentDrawing = drawing;
        invalidate();  // Redraw the canvas
    }


    private void setupDrawing() {
        path = new Path();
        drawPaint = new Paint();

        drawPaint.setColor(paintColor);
        drawPaint.setAntiAlias(true);
        drawPaint.setStrokeWidth(brushSize);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);

        canvasPaint = new Paint(Paint.DITHER_FLAG);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        drawCanvas = new Canvas(canvasBitmap);
        drawCanvas.drawColor(Color.WHITE); // White background
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas) {
        // First draw the base white canvas
        canvas.drawBitmap(canvasBitmap, 0, 0, null);

        // Draw the current path the user is creating
        canvas.drawPath(path, drawPaint);

        // If there is a current drawing loaded from before, overlay it on the canvas
        if (currentDrawing != null) {
            // Copy the drawing's bitmap to our canvas
            Bitmap drawingBitmap = currentDrawing.getBitmap();

            // Scale the bitmap if needed to fit the canvas
            if (drawingBitmap.getWidth() != getWidth() || drawingBitmap.getHeight() != getHeight()) {
                drawingBitmap = Bitmap.createScaledBitmap(
                        drawingBitmap,
                        getWidth(),
                        getHeight(),
                        true
                );
            }

            // Draw the bitmap onto our canvas
            drawCanvas.drawBitmap(drawingBitmap, 0, 0, null);

            // Clear the currentDrawing reference so we don't keep redrawing it
            // It's now part of our canvas
            currentDrawing = null;

            // Invalidate to show the changes
            invalidate();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float touchX = event.getX();
        float touchY = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                path.moveTo(touchX, touchY);
                break;
            case MotionEvent.ACTION_MOVE:
                path.lineTo(touchX, touchY);
                break;
            case MotionEvent.ACTION_UP:
                drawCanvas.drawPath(path, drawPaint);

                // Save for undo
                paths.push(new Path(path));
                Paint newPaint = new Paint(drawPaint);
                paints.push(newPaint);

                // Clear redo stack when new drawing happens
                redoPaths.clear();
                redoPaints.clear();

                path.reset();
                break;
            default:
                return false;
        }

        invalidate();
        return true;
    }

    public void setColor(int color) {
        paintColor = color;
        drawPaint.setColor(paintColor);
    }

    public void setBrushSize(float size) {
        brushSize = size;
        drawPaint.setStrokeWidth(brushSize);
    }

    public void setTool(Tool tool) {
        currentTool = tool;

        if (tool == Tool.ERASER) {
            drawPaint.setColor(Color.WHITE);
        } else {
            drawPaint.setColor(paintColor);
        }
    }

    public void undo() {
        if (!paths.isEmpty()) {
            redoPaths.push(paths.pop());
            redoPaints.push(paints.pop());
            redrawCanvas();
        }
    }

    public void redo() {
        if (!redoPaths.isEmpty()) {
            paths.push(redoPaths.pop());
            paints.push(redoPaints.pop());
            redrawCanvas();
        }
    }

    private void redrawCanvas() {
        drawCanvas.drawColor(Color.WHITE); // Clear and redraw

        for (int i = 0; i < paths.size(); i++) {
            drawCanvas.drawPath(paths.get(i), paints.get(i));
        }

        invalidate();
    }

    public void clearCanvas() {
        paths.clear();
        paints.clear();
        redoPaths.clear();
        redoPaints.clear();
        drawCanvas.drawColor(Color.WHITE);
        invalidate();
    }

    public Bitmap getBitmap() {
        return Bitmap.createBitmap(canvasBitmap);
    }
}