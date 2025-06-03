package com.example.messenger.utils;

import android.content.Context;
import android.util.Log;

public class ErrorHandler {
    private static final String TAG = "ErrorHandler";

    public static void handleError(Context context, String message, Exception e) {
        // Only log the error for debugging, no user-facing messages
        Log.e(TAG, message + (e != null ? ": " + e.getMessage() : ""), e);
    }

    public static void handleError(Context context, String message) {
        // Only log the error for debugging, no user-facing messages
        Log.e(TAG, message);
    }

    // These methods are kept for compatibility but are no longer used
    public static void setShowErrors(boolean show) {
        // No-op, errors are never shown
    }

    public static boolean isShowErrors() {
        return false; // Always return false as we never show errors
    }
} 