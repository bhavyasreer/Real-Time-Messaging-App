package com.example.messenger;

import android.app.Application;
import android.util.Log;
import android.widget.Toast;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.messenger.utils.ErrorHandler;

public class MessengerApplication extends Application {
    private static final String TAG = "MessengerApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            // Enable error messages for debugging
            ErrorHandler.setShowErrors(true);
            
            // Initialize Firebase
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this);
                Log.d(TAG, "Firebase initialized successfully");
            } else {
                Log.d(TAG, "Firebase already initialized");
            }
            
            // Enable Firestore offline persistence
            try {
                FirebaseFirestore.getInstance().setFirestoreSettings(
                    new com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                        .setPersistenceEnabled(true)
                        .build()
                );
                Log.d(TAG, "Firestore settings configured successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error configuring Firestore settings: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            Log.e(TAG, "Fatal error initializing Firebase: " + e.getMessage(), e);
            // We can't show Toast here as the context isn't ready
        }
    }
} 