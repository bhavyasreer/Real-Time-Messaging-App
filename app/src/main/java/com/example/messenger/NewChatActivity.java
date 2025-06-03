package com.example.messenger;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.example.messenger.utils.ErrorHandler;

public class NewChatActivity extends AppCompatActivity {
    private static final String TAG = "NewChatActivity";
    private EditText searchEditText;
    private RecyclerView userListRecyclerView;
    private UserAdapter userAdapter;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private List<User> userList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_chat);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("New Chat");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Initialize views
        searchEditText = findViewById(R.id.searchEditText);
        userListRecyclerView = findViewById(R.id.userListRecyclerView);
        userListRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        userList = new ArrayList<>();
        userAdapter = new UserAdapter(this, userList);
        userListRecyclerView.setAdapter(userAdapter);

        // Setup search
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchUsers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Load initial users
        loadUsers();
    }

    private void loadUsers() {
        String currentUserId = mAuth.getCurrentUser().getUid();
        Log.d(TAG, "Loading users. Current user ID: " + currentUserId);
        
        db.collection("users")
                .whereNotEqualTo("uid", currentUserId)
                .orderBy("name", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error loading users: " + error.getMessage());
                        ErrorHandler.handleError(this, "Error loading users", error);
                        return;
                    }

                    userList.clear();
                    if (value != null) {
                        Log.d(TAG, "Found " + value.size() + " users");
                        for (QueryDocumentSnapshot doc : value) {
                            User user = doc.toObject(User.class);
                            Log.d(TAG, "User found: " + user.getName() + " (" + user.getEmail() + ")");
                            userList.add(user);
                        }
                        userAdapter.notifyDataSetChanged();
                    } else {
                        Log.d(TAG, "No users found in the collection");
                    }
                });
    }

    private void searchUsers(String query) {
        String currentUserId = mAuth.getCurrentUser().getUid();
        Log.d(TAG, "Searching users with query: " + query);
        
        db.collection("users")
                .whereNotEqualTo("uid", currentUserId)
                .orderBy("name", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    userList.clear();
                    Log.d(TAG, "Search found " + queryDocumentSnapshots.size() + " users");
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        User user = doc.toObject(User.class);
                        if (user.getName().toLowerCase().contains(query.toLowerCase())) {
                            Log.d(TAG, "Matching user found: " + user.getName() + " (" + user.getEmail() + ")");
                            userList.add(user);
                        }
                    }
                    userAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error searching users: " + e.getMessage());
                    ErrorHandler.handleError(this, "Error searching users", e);
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}