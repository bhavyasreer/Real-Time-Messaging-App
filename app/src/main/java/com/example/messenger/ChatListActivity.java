package com.example.messenger;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.app.AlertDialog;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.CheckBox;
import com.google.android.material.button.MaterialButton;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.example.messenger.utils.ErrorHandler;
import com.example.messenger.utils.TextDrawableHelper;
import android.widget.PopupMenu;
import java.util.HashSet;
import java.util.Set;
import android.os.Handler;
import com.google.firebase.firestore.DocumentChange;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;

public class ChatListActivity extends AppCompatActivity {
    private RecyclerView chatListRecyclerView;
    private ChatListAdapter chatAdapter;
    private List<Chat> chatList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private TextInputEditText searchEditText;
    private TabLayout chatTabs;
    private String currentTab = "All";
    private List<User> allUsers = new ArrayList<>(); // For group creation
    private com.google.android.material.button.MaterialButton createGroupButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Check if user is logged in
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Set user as online
        setUserOnlineStatus(true);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Initialize views
        chatListRecyclerView = findViewById(R.id.chatListRecyclerView);
        searchEditText = findViewById(R.id.searchEditText);
        FloatingActionButton newChatFab = findViewById(R.id.newChatFab);
        chatTabs = findViewById(R.id.chatTabs);
        createGroupButton = findViewById(R.id.createGroupButton);

        // Setup RecyclerView
        chatList = new ArrayList<>();
        chatAdapter = new ChatListAdapter(this, chatList);
        chatListRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatListRecyclerView.setAdapter(chatAdapter);

        // Setup search functionality
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                chatAdapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Load all users for group creation
        loadAllUsers();

        // Setup FAB click listener for new chat
        newChatFab.setOnClickListener(v -> {
            Intent intent = new Intent(ChatListActivity.this, NewChatActivity.class);
            startActivity(intent);
        });

        // Long press FAB to create group
        newChatFab.setOnLongClickListener(v -> {
            showCreateGroupDialog();
            return true;
        });

        // Example: create a parent chat document for testing
        List<String> participants = Arrays.asList("YI9nvmGie8XfwBZHcxAQOmDuoE62", "20QQbm6kqtYbSQuL8JI5B4jUh5I1");
        createParentChatDocument("85f0iU7PM9DIH3AEtfHs", participants, "hi", com.google.firebase.Timestamp.now());

        // Load chats
        loadChats();

        // Setup tab layout
        chatTabs.addTab(chatTabs.newTab().setText("All"));
        chatTabs.addTab(chatTabs.newTab().setText("Groups"));
        chatTabs.addTab(chatTabs.newTab().setText("Favourites"));
        chatTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getText().toString();
                filterChatsByTab();
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        createGroupButton.setOnClickListener(v -> showCreateGroupDialog());
        createGroupButton.setVisibility(View.GONE); // Hide by default
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadChats();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Set user as offline when app is closed
        setUserOnlineStatus(false);
    }

    private void loadChats() {
        String currentUserId = mAuth.getCurrentUser().getUid();
        db.collection("chats")
            .whereArrayContains("participants", currentUserId)
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)
            .addSnapshotListener((value, error) -> {
                if (error != null) {
                    ErrorHandler.handleError(this, "Error loading chats", error);
                    return;
                }

                if (value != null) {
                    // Process all document changes in the snapshot
                    for (DocumentChange dc : value.getDocumentChanges()) {
                        Chat chat = dc.getDocument().toObject(Chat.class);
                        chat.setId(dc.getDocument().getId());

                        // Get unread count for the current user
                        Long unreadCount = dc.getDocument().getLong("unreadCounts." + currentUserId);
                        if (unreadCount != null) {
                            chat.getUnreadCounts().put(currentUserId, unreadCount);
                        }

                        // For individual chats, get the other user's name from the userNames map
                        if (!chat.isGroup()) {
                            String otherUserId = null;
                            for (String participant : chat.getParticipants()) {
                                if (!participant.equals(currentUserId)) {
                                    otherUserId = participant;
                                    break;
                                }
                            }
                            // Try to get the name from the userNames map in the chat document
                            java.util.Map<String, String> userNames = (java.util.Map<String, String>) dc.getDocument().get("userNames");
                            if (userNames != null && otherUserId != null && userNames.get(otherUserId) != null) {
                                chat.setOtherUserName(userNames.get(otherUserId));
                            } else {
                                chat.setOtherUserName("?");
                            }
                        }

                        switch (dc.getType()) {
                            case ADDED:
                                // Add new chat to the list at the correct index
                                // Check if the chat already exists in the list to avoid duplicates
                                int existingIndex = findChatIndex(chat.getId());
                                if (existingIndex == -1) {
                                    chatList.add(dc.getNewIndex(), chat);
                                    // Update filtered list based on current tab and search query
                                    filterChatsByTab();
                                } else {
                                     // If chat exists, it might be a move or just an update without index change
                                     // For now, we'll treat it as a modification to ensure data is fresh
                                     // A more sophisticated approach could handle moves explicitly
                                      chatList.set(existingIndex, chat);
                                      filterChatsByTab(); // Re-filter to update UI
                                }
                                break;
                            case MODIFIED:
                                // Update the existing chat in the list at the correct index
                                int oldIndex = findChatIndex(chat.getId());
                                if (oldIndex != -1) {
                                     // Remove from old position
                                     chatList.remove(oldIndex);
                                     // Add to new position
                                     chatList.add(dc.getNewIndex(), chat);
                                    // Update filtered list based on current tab and search query
                                     filterChatsByTab();
                                } else {
                                     // This case should ideally not happen if ADDED is handled correctly,
                                     // but as a fallback, add if not found.
                                     chatList.add(dc.getNewIndex(), chat);
                                     filterChatsByTab(); // Re-filter to update UI
                                }
                                break;
                            case REMOVED:
                                // Remove the chat from the list
                                int removeIndex = findChatIndex(chat.getId());
                                if (removeIndex != -1) {
                                    chatList.remove(removeIndex);
                                     // Update filtered list based on current tab and search query
                                     filterChatsByTab();
                                }
                                break;
                        }
                    }

                     // After processing all changes, update the adapter on the main thread
                    runOnUiThread(() -> chatAdapter.setChats(new ArrayList<>(chatList))); // Provide a new list instance
                } else {
                     // Handle empty chat list initially or if all chats are removed
                     runOnUiThread(() -> {
                        chatList.clear();
                        filterChatsByTab(); // This will also call setChats internally with an empty list
                     });
                }
            });
    }

    // Helper method to find chat index by ID
    private int findChatIndex(String chatId) {
        for (int i = 0; i < chatList.size(); i++) {
            if (chatList.get(i).getId() != null && chatList.get(i).getId().equals(chatId)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chat_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_profile) {
            // Show popup menu for profile actions
            View profileIcon = findViewById(R.id.action_profile);
            if (profileIcon == null) {
                // fallback: show profile info dialog as before
                showCurrentUserProfileMenu();
                return true;
            }
            // Check if menu resource exists before inflating
            int menuResId = getResources().getIdentifier("profile_menu", "menu", getPackageName());
            if (menuResId == 0) {
                Toast.makeText(this, "Profile menu not found!", Toast.LENGTH_SHORT).show();
                return true;
            }
            PopupMenu popup = new PopupMenu(this, profileIcon);
            popup.getMenuInflater().inflate(menuResId, popup.getMenu());
            popup.setOnMenuItemClickListener(menuItem -> {
                int id = menuItem.getItemId();
                if (id == R.id.menu_edit_profile) {
                    startActivity(new Intent(this, ProfileSettingsActivity.class));
                    return true;
                } else if (id == R.id.menu_logout) {
                    mAuth.signOut();
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.menu_reset_data) {
                    resetData();
                    return true;
                }
                return false;
            });
            popup.show();
            return true;
        } else if (item.getItemId() == R.id.action_logout) {
            mAuth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void resetData() {
        // First, delete all existing data
        db.collection("chats").get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                doc.getReference().delete();
            }
        });

        db.collection("users").get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                doc.getReference().delete();
            }
        });

        // Wait a moment to ensure deletion completes
        new Handler().postDelayed(this::createTestUsers, 1000);
    }

    private void createTestUsers() {
        // Create test user 1
        Map<String, Object> user1 = new HashMap<>();
        user1.put("name", "John Doe");
        user1.put("email", "john@example.com");
        user1.put("uid", "user1");

        // Create test user 2
        Map<String, Object> user2 = new HashMap<>();
        user2.put("name", "Jane Smith");
        user2.put("email", "jane@example.com");
        user2.put("uid", "user2");

        // Add users to Firestore
        db.collection("users").document("user1").set(user1);
        db.collection("users").document("user2").set(user2);

        // Create a test chat between the users
        Map<String, Object> chat = new HashMap<>();
        chat.put("participants", Arrays.asList("user1", "user2"));
        chat.put("lastMessageText", "Hello! This is a test message.");
        chat.put("lastMessageTime", com.google.firebase.Timestamp.now());

        db.collection("chats").add(chat);

        Toast.makeText(this, "Test data has been reset and created", Toast.LENGTH_SHORT).show();
    }

    // Utility to create a user if not exists
    public void createUserIfNotExists(String uid, String name) {
        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (!doc.exists()) {
                Map<String, Object> user = new HashMap<>();
                user.put("name", name);
                db.collection("users").document(uid).set(user);
            }
        });
    }

    // Utility to create a chat if not exists between two users
    public void createChatIfNotExists(String user1Id, String user2Id) {
        // Always store participants sorted for consistency
        List<String> participants = Arrays.asList(user1Id, user2Id);
        db.collection("chats")
            .whereEqualTo("participants", participants)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                if (querySnapshot.isEmpty()) {
                    Map<String, Object> chat = new HashMap<>();
                    chat.put("participants", participants);
                    chat.put("lastMessageText", "");
                    chat.put("lastMessageTime", null);
                    db.collection("chats").add(chat);
                }
            });
    }

    // Utility to create a parent chat document with required fields
    public void createParentChatDocument(String chatId, List<String> participants, String lastMessageText, com.google.firebase.Timestamp lastMessageTime) {
        Map<String, Object> chat = new HashMap<>();
        chat.put("participants", participants);
        chat.put("lastMessageText", lastMessageText);
        chat.put("lastMessageTime", lastMessageTime);
        db.collection("chats").document(chatId).set(chat)
            .addOnSuccessListener(aVoid -> {
                // Success message is optional, you can remove this if you don't want any messages
                if (ErrorHandler.isShowErrors()) {
                    Toast.makeText(this, "Parent chat document created!", Toast.LENGTH_SHORT).show();
                }
            })
            .addOnFailureListener(e -> ErrorHandler.handleError(this, "Failed to create chat", e));
    }

    private void filterChatsByTab() {
        List<Chat> filtered = new ArrayList<>();
        String currentUserId = mAuth.getCurrentUser().getUid();

        // Show or hide the Create Group button based on the selected tab
        if (currentTab != null && currentTab.equals("Groups")) {
            createGroupButton.setVisibility(View.VISIBLE);
        } else {
            createGroupButton.setVisibility(View.GONE);
        }

        if (currentTab != null) {
            switch (currentTab) {
                case "Groups":
                    // Show only group chats
                    for (Chat chat : chatList) {
                        if (chat.isGroup()) {
                            filtered.add(chat);
                        }
                    }
                    break;
                case "Favourites":
                    // Show only favourite chats
                    for (Chat chat : chatList) {
                        if (chat.getFavourite() != null && chat.getFavourite().contains(currentUserId)) {
                            filtered.add(chat);
                        }
                    }
                    break;
                default:
                    // Show all chats
                    filtered.addAll(chatList);
                    break;
            }
        } else {
            filtered.addAll(chatList);
        }
        chatAdapter.setChats(filtered);
    }

    private void loadAllUsers() {
        db.collection("users").get().addOnSuccessListener(querySnapshot -> {
            allUsers.clear();
            for (QueryDocumentSnapshot doc : querySnapshot) {
                User user = doc.toObject(User.class);
                allUsers.add(user);
            }
        });
    }

    private void showCreateGroupDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create Group");
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        EditText groupNameInput = new EditText(this);
        groupNameInput.setHint("Group Name");
        layout.addView(groupNameInput);
        ScrollView scrollView = new ScrollView(this);
        LinearLayout usersLayout = new LinearLayout(this);
        usersLayout.setOrientation(LinearLayout.VERTICAL);
        List<CheckBox> checkBoxes = new ArrayList<>();
        String currentUserId = mAuth.getCurrentUser().getUid();
        for (User user : allUsers) {
            if (!user.getUid().equals(currentUserId)) {
                CheckBox checkBox = new CheckBox(this);
                checkBox.setText(user.getName());
                checkBox.setTag(user.getUid());
                usersLayout.addView(checkBox);
                checkBoxes.add(checkBox);
            }
        }
        scrollView.addView(usersLayout);
        layout.addView(scrollView);
        builder.setView(layout);
        builder.setPositiveButton("Create", (dialog, which) -> {
            String groupName = groupNameInput.getText().toString().trim();
            List<String> selectedUserIds = new ArrayList<>();
            selectedUserIds.add(currentUserId); // Add self
            for (CheckBox cb : checkBoxes) {
                if (cb.isChecked()) {
                    selectedUserIds.add((String) cb.getTag());
                }
            }
            Toast.makeText(this, "Creating group: " + groupName + " with participants: " + selectedUserIds, Toast.LENGTH_LONG).show();
            if (groupName.isEmpty() || selectedUserIds.size() < 2) {
                Toast.makeText(this, "Enter group name and select at least one user", Toast.LENGTH_SHORT).show();
                return;
            }
            createGroupChat(groupName, selectedUserIds);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // Create a group chat in Firestore with debug logging
    private void createGroupChat(String groupName, List<String> userIds) {
        // Create a new Chat object with group settings
        Chat groupChat = new Chat(
            userIds,
            "Group created",
            com.google.firebase.Timestamp.now(),
            true,  // isGroup
            groupName  // groupName
        );

        // Add to Firestore
        db.collection("chats").add(groupChat)
            .addOnSuccessListener(docRef -> {
                Toast.makeText(this, "Group '" + groupName + "' created successfully!", Toast.LENGTH_SHORT).show();
                // Switch to Groups tab
                TabLayout.Tab groupsTab = chatTabs.getTabAt(1);
                if (groupsTab != null) {
                    groupsTab.select();
                }
                loadChats();
            })
            .addOnFailureListener(e -> ErrorHandler.handleError(this, "Failed to create group", e));
    }

    // Show group info dialog
    public void showGroupInfoDialog(Chat chat) {
        db.collection("users")
            .whereIn("uid", chat.getParticipants())
            .get()
            .addOnSuccessListener(querySnapshot -> {
                StringBuilder info = new StringBuilder();
                info.append("Group Name: ").append(chat.getGroupName()).append("\n\nParticipants:\n");
                for (QueryDocumentSnapshot doc : querySnapshot) {
                    User user = doc.toObject(User.class);
                    info.append("- ").append(user.getName()).append("\n");
                }
                new AlertDialog.Builder(this)
                    .setTitle("Group Info")
                    .setMessage(info.toString())
                    .setPositiveButton("OK", null)
                    .show();
            });
    }

    private void showCurrentUserProfileMenu() {
        String currentUserId = mAuth.getCurrentUser().getUid();
        String currentUserEmail = mAuth.getCurrentUser().getEmail();
        db.collection("users").document(currentUserId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null) {
                        showProfileInfoDialog(user);
                    }
                } else {
                    User newUser = new User(currentUserId, currentUserEmail, currentUserEmail.split("@")[0]);
                    db.collection("users").document(currentUserId)
                        .set(newUser)
                        .addOnSuccessListener(aVoid -> showProfileInfoDialog(newUser));
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Error loading profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void showProfileInfoDialog(User user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_profile_info, null);
        
        ImageView profileImageView = dialogView.findViewById(R.id.profileImageView);
        TextView nameTextView = dialogView.findViewById(R.id.nameTextView);
        TextView emailTextView = dialogView.findViewById(R.id.emailTextView);
        
        // Set profile image using TextDrawableHelper
        profileImageView.setImageDrawable(TextDrawableHelper.create(this, user.getName()));
        
        nameTextView.setText(user.getName());
        emailTextView.setText(user.getEmail());
        
        builder.setView(dialogView)
               .setPositiveButton("Edit Profile", (dialog, which) -> {
                   startActivity(new Intent(this, ProfileSettingsActivity.class));
               })
               .setNegativeButton("Close", null)
               .show();
    }

    private void setUserOnlineStatus(boolean isOnline) {
        // Add a null check for the current user
        if (mAuth.getCurrentUser() != null) {
            String currentUserId = mAuth.getCurrentUser().getUid();
            db.collection("users").document(currentUserId)
                .update(
                    "isOnline", isOnline,
                    "lastSeen", System.currentTimeMillis()
                );
        }
        // If mAuth.getCurrentUser() is null, we do nothing.
    }
}