package com.example.messenger;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.UUID;
import com.example.messenger.utils.ErrorHandler;
import com.google.android.material.snackbar.Snackbar;
import com.example.messenger.utils.TextDrawableHelper;

public class ProfileSettingsActivity extends AppCompatActivity {
    private static final String TAG = "ProfileSettingsActivity";
    private ImageView profileImageView;
    private TextInputEditText nameInput;
    private MaterialButton saveButton;
    private CircularProgressIndicator progressIndicator;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private boolean isSaving = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_settings);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        // Initialize views
        profileImageView = findViewById(R.id.profileImageView);
        nameInput = findViewById(R.id.nameInput);
        saveButton = findViewById(R.id.saveButton);
        progressIndicator = findViewById(R.id.progressIndicator);

        // Load current user data
        loadUserData();

        // Setup click listeners
        saveButton.setOnClickListener(v -> saveProfile());
    }

    private void loadUserData() {
        String userId = mAuth.getCurrentUser().getUid();
        showLoading(true);
        
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                User user = documentSnapshot.toObject(User.class);
                if (user != null) {
                    nameInput.setText(user.getName());
                    profileImageView.setImageDrawable(
                        TextDrawableHelper.create(this, user.getName())
                    );
                }
                showLoading(false);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading profile", e);
                ErrorHandler.handleError(this, "Error loading profile", e);
                showLoading(false);
            });
    }

    private void saveProfile() {
        if (isSaving) {
            return; // Prevent multiple save attempts
        }

        String userId = mAuth.getCurrentUser().getUid();
        String newName = nameInput.getText().toString().trim();

        if (newName.isEmpty()) {
            ErrorHandler.handleError(this, "Please enter a name");
            return;
        }

        isSaving = true;
        showLoading(true);

        Log.d(TAG, "No profile picture, updating profile with name only...");
        updateUserProfile(userId, newName);
    }

    private void updateUserProfile(String userId, String name) {
        Log.d(TAG, "Updating user profile...");
        User user = new User(userId, mAuth.getCurrentUser().getEmail(), name);

        db.collection("users").document(userId)
            .set(user)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Profile updated successfully");
                runOnUiThread(() -> {
                    progressIndicator.setVisibility(View.GONE);
                    saveButton.setEnabled(true);
                    saveButton.setText("Save Changes");
                    new androidx.appcompat.app.AlertDialog.Builder(ProfileSettingsActivity.this)
                        .setTitle("Success")
                        .setMessage("Profile updated successfully")
                        .setCancelable(false)
                        .setPositiveButton("OK", (dialog, which) -> {
                            dialog.dismiss();
                            finish();
                        })
                        .show();
                });
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error updating profile", e);
                ErrorHandler.handleError(this, "Error updating profile", e);
                handleSaveComplete();
            });
    }

    private void handleSaveComplete() {
        isSaving = false;
        showLoading(false);
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            progressIndicator.setVisibility(View.VISIBLE);
            saveButton.setEnabled(false);
        } else {
            progressIndicator.setVisibility(View.GONE);
            saveButton.setEnabled(true);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (!isSaving) {
            onBackPressed();
            return true;
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handleSaveComplete();
    }
} 