package com.example.messenger;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.messenger.utils.ErrorHandler;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private TextInputEditText emailInput, passwordInput, nameInput;
    private TextInputLayout emailLayout, passwordLayout, nameLayout;
    private MaterialButton loginButton, registerButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private View rootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        rootView = findViewById(R.id.rootView);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        nameInput = findViewById(R.id.nameInput);
        emailLayout = findViewById(R.id.emailLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        nameLayout = findViewById(R.id.nameLayout);
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);

        // Add input validation
        setupInputValidation();

        // Check if user is already logged in
        if (mAuth.getCurrentUser() != null) {
            Log.d(TAG, "User already logged in, redirecting to ChatListActivity");
            startActivity(new Intent(LoginActivity.this, ChatListActivity.class));
            finish();
            return;
        }

        // Set click listeners with animations
        loginButton.setOnClickListener(v -> {
            if (validateInputs()) {
                loginButton.setEnabled(false);
                registerButton.setEnabled(false);
                loginUser();
            }
        });

        registerButton.setOnClickListener(v -> {
            if (validateInputs()) {
                loginButton.setEnabled(false);
                registerButton.setEnabled(false);
                registerUser();
            }
        });

        // Start entrance animation
        startEntranceAnimation();
    }

    private void setupInputValidation() {
        emailInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && !isValidEmail(emailInput.getText().toString())) {
                emailLayout.setError("Please enter a valid email address");
            } else {
                emailLayout.setError(null);
            }
        });

        passwordInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && passwordInput.getText().length() < 6) {
                passwordLayout.setError("Password must be at least 6 characters");
            } else {
                passwordLayout.setError(null);
            }
        });

        nameInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && TextUtils.isEmpty(nameInput.getText())) {
                nameLayout.setError("Please enter your name");
            } else {
                nameLayout.setError(null);
            }
        });
    }

    private boolean validateInputs() {
        boolean isValid = true;

        if (!isValidEmail(emailInput.getText().toString())) {
            emailLayout.setError("Please enter a valid email address");
            isValid = false;
        }

        if (passwordInput.getText().length() < 6) {
            passwordLayout.setError("Password must be at least 6 characters");
            isValid = false;
        }

        if (TextUtils.isEmpty(nameInput.getText())) {
            nameLayout.setError("Please enter your name");
            isValid = false;
        }

        return isValid;
    }

    private boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void startEntranceAnimation() {
        Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in);
        rootView.startAnimation(slideIn);
    }

    private void loginUser() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Login successful for user: " + email);
                        startActivity(new Intent(LoginActivity.this, ChatListActivity.class));
                        overridePendingTransition(R.anim.slide_in, R.anim.slide_out);
                        finish();
                    } else {
                        Log.e(TAG, "Login failed: " + task.getException().getMessage());
                        ErrorHandler.handleError(LoginActivity.this, "Authentication failed", task.getException());
                        loginButton.setEnabled(true);
                        registerButton.setEnabled(true);
                    }
                });
    }

    private void registerUser() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String name = nameInput.getText().toString().trim();

        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    String uid = mAuth.getCurrentUser().getUid();
                    User user = new User(uid, email, name);

                    db.collection("users").document(uid)
                        .set(user)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(LoginActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, ChatListActivity.class));
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(LoginActivity.this, "Error saving user data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            loginButton.setEnabled(true);
                            registerButton.setEnabled(true);
                        });
                } else {
                    String errorMsg = "Registration failed: ";
                    if (task.getException() != null) {
                        errorMsg += task.getException().getMessage();
                    }
                    Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    loginButton.setEnabled(true);
                    registerButton.setEnabled(true);
                }
            });
    }
} 