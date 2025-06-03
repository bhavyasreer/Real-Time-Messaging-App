package com.example.messenger.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public class TextDrawableHelper {
    public static Drawable create(Context context, String name) {
        int size = 120; // px, adjust as needed
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Draw circle background
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(getColorForName(name));
        canvas.drawCircle(size / 2, size / 2, size / 2, paint);

        // Draw first letter
        paint.setColor(Color.WHITE);
        paint.setTextSize(size / 2);
        paint.setTextAlign(Paint.Align.CENTER);
        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        float x = size / 2;
        float y = size / 2 - (fontMetrics.ascent + fontMetrics.descent) / 2;
        String firstLetter = name != null && !name.isEmpty() ? name.substring(0, 1).toUpperCase() : "?";
        canvas.drawText(firstLetter, x, y, paint);

        return new BitmapDrawable(context.getResources(), bitmap);
    }

    // Simple color generator based on name hash
    private static int getColorForName(String name) {
        int[] colors = {
            Color.parseColor("#F44336"), Color.parseColor("#E91E63"), Color.parseColor("#9C27B0"),
            Color.parseColor("#3F51B5"), Color.parseColor("#03A9F4"), Color.parseColor("#009688"),
            Color.parseColor("#4CAF50"), Color.parseColor("#FF9800"), Color.parseColor("#795548")
        };
        int hash = name != null ? Math.abs(name.hashCode()) : 0;
        return colors[hash % colors.length];
    }
} 