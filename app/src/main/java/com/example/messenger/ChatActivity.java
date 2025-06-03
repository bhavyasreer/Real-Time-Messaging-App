package com.example.messenger;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.DocumentChange;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {
    private RecyclerView messageRecyclerView;
    private MessageAdapter messageAdapter;
    private EditText messageInput;
    private ImageButton sendButton;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String chatId;
    private String otherUserId;
    private List<Message> messageList;
    private boolean isFavourite = false;
    private boolean isGroup = false;
    private String groupName = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Get chat details from intent
        chatId = getIntent().getStringExtra("chatId");
        isGroup = getIntent().getBooleanExtra("isGroup", false);
        groupName = getIntent().getStringExtra("groupName");
        otherUserId = getIntent().getStringExtra("otherUserId");

        if (chatId == null || (!isGroup && otherUserId == null)) {
            finish();
            return;
        }

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (isGroup) {
            // Always fetch groupName from Firestore for accuracy
            db.collection("chats").document(chatId).get().addOnSuccessListener(doc -> {
                String fetchedGroupName = doc.getString("groupName");
                List<String> participants = (List<String>) doc.get("participants");
                getSupportActionBar().setTitle(fetchedGroupName != null ? fetchedGroupName : "Group");
                Toast.makeText(this, "Opened group: " + fetchedGroupName + "\nParticipants: " + participants, Toast.LENGTH_LONG).show();
            });
            toolbar.setOnClickListener(v -> showGroupInfoDialog());
        } else {
            // Get other user's name for toolbar title
            db.collection("users").document(otherUserId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            getSupportActionBar().setTitle(user.getName());
                        }
                    });
        }

        // Initialize views
        messageRecyclerView = findViewById(R.id.messageRecyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);

        // Setup RecyclerView
        messageList = new ArrayList<>();
        messageRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        messageAdapter = new MessageAdapter(this, messageList, chatId);
        messageRecyclerView.setAdapter(messageAdapter);

        // Setup send button
        sendButton.setOnClickListener(v -> sendMessage());

        // Load messages for this chat (works for both group and individual)
        loadMessages();

        // Mark messages as read when chat is opened
        markMessagesAsRead();

        // Check if chat is favourite for this user
        db.collection("chats").document(chatId)
            .get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    List<String> fav = (List<String>) doc.get("favourite");
                    String currentUserId = mAuth.getCurrentUser().getUid();
                    if (fav != null && fav.contains(currentUserId)) {
                        isFavourite = true;
                        invalidateOptionsMenu();
                    }
                }
            });
    }

    private void loadMessages() {
        String currentUserId = mAuth.getCurrentUser().getUid();
        db.collection("chats").document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        // Handle error
                        return;
                    }

                    if (value != null) {
                        // Use a temporary list to build the updated message list
                        List<Message> updatedMessageList = new ArrayList<>(messageList);
                        boolean changed = false;

                        for (DocumentChange dc : value.getDocumentChanges()) {
                            Message message = dc.getDocument().toObject(Message.class);
                            message.setId(dc.getDocument().getId());
                            message.setChatId(chatId); // Ensure chat ID is set

                            // Check if message is deleted for current user synchronously if possible
                            // For a proper real-time filter on deletion, a different Firestore structure might be better.
                            // Keeping asynchronous check for now, but optimizing how updates are applied.
                            dc.getDocument().getReference().collection("deletedFor")
                                .document(currentUserId)
                                .get()
                                .addOnSuccessListener(deletedDoc -> {
                                    // Ensure UI updates are on the main thread
                                    runOnUiThread(() -> {
                                        boolean isDeletedForCurrentUser = deletedDoc.exists();
                                        int oldIndex = dc.getOldIndex();
                                        int newIndex = dc.getNewIndex();

                                        switch (dc.getType()) {
                                            case ADDED:
                                                if (!isDeletedForCurrentUser) {
                                                    // Add new message if not deleted for current user
                                                    // Find the correct index to insert based on timestamp if needed, but for now add and rely on sorting.
                                                     int insertIndex = findMessageInsertIndex(message);
                                                    messageList.add(insertIndex, message);
                                                    messageAdapter.notifyItemInserted(insertIndex);
                                                    // Scroll to bottom if it's the latest message
                                                     if (insertIndex == messageList.size() - 1 || (messageList.size() == 1 && insertIndex == 0)) {
                                                        messageRecyclerView.smoothScrollToPosition(messageList.size() - 1);
                                                    }
                                                }
                                                break;
                                            case MODIFIED:
                                                // Find and update existing message if not deleted for current user
                                                 int existingIndex = findMessageIndex(message.getId());
                                                if (!isDeletedForCurrentUser && existingIndex != -1) {
                                                     // Update the message data
                                                     messageList.set(existingIndex, message);
                                                     messageAdapter.notifyItemChanged(existingIndex);
                                                      // Scroll to bottom if the modified message is the last one
                                                      if (existingIndex == messageList.size() - 1) {
                                                          messageRecyclerView.smoothScrollToPosition(messageList.size() - 1);
                                                      }
                                                } else if (isDeletedForCurrentUser && existingIndex != -1) {
                                                    // If message is now deleted for current user, remove it
                                                     messageList.remove(existingIndex);
                                                     messageAdapter.notifyItemRemoved(existingIndex);
                                                }
                                                // Note: If a modified message is added and wasn't deleted, but is now deleted, the REMOVED case might handle it too.
                                                break;
                                            case REMOVED:
                                                // Remove message
                                                 int removeIndex = findMessageIndex(message.getId());
                                                if (removeIndex != -1) {
                                                    messageList.remove(removeIndex);
                                                    messageAdapter.notifyItemRemoved(removeIndex);
                                                }
                                                break;
                                        }
                                    });
                                });
                        }

                         // Initial load: sort messages and notify adapter once after all initial data is processed
                         // For subsequent real-time updates, granular notifications are used.
                         if (value.getDocumentChanges().size() == value.size()) {
                              runOnUiThread(() -> {
                                 // Sort the initial list by timestamp
                                  messageList.sort((msg1, msg2) -> {
                                     if (msg1.getTimestamp() == null || msg2.getTimestamp() == null) return 0;
                                     return msg1.getTimestamp().compareTo(msg2.getTimestamp());
                                  });
                                 messageAdapter.notifyDataSetChanged();
                                 if (!messageList.isEmpty()) {
                                     messageRecyclerView.smoothScrollToPosition(messageList.size() - 1);
                                 }
                             });
                         }
                    }
                });
    }

    // Helper method to find message index by ID
    private int findMessageIndex(String messageId) {
        for (int i = 0; i < messageList.size(); i++) {
            if (messageList.get(i).getId() != null && messageList.get(i).getId().equals(messageId)) {
                return i;
            }
        }
        return -1;
    }

    // Helper method to find the correct index to insert a new message based on timestamp
    private int findMessageInsertIndex(Message newMessage) {
        for (int i = 0; i < messageList.size(); i++) {
            if (newMessage.getTimestamp() != null && messageList.get(i).getTimestamp() != null &&
                newMessage.getTimestamp().compareTo(messageList.get(i).getTimestamp()) < 0) {
                return i;
            }
        }
        return messageList.size(); // Add to the end if timestamp is latest or null
    }

    private void markMessagesAsRead() {
        String currentUserId = mAuth.getCurrentUser().getUid();
        db.collection("chats").document(chatId)
            .collection("messages")
            .whereNotEqualTo("senderId", currentUserId)
            .whereEqualTo("status", "sent")
            .get()
            .addOnSuccessListener(querySnapshot -> {
                for (QueryDocumentSnapshot doc : querySnapshot) {
                    doc.getReference().update("status", "read");
                }
            });

        // Reset unread count for the current user in the chat document
        db.collection("chats").document(chatId)
            .update("unreadCounts." + currentUserId, 0);
    }

    // Utility to create a chat if not exists between two users
    public void createChatIfNotExists(String user1Id, String user2Id, Runnable onChatReady) {
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
                    db.collection("chats").add(chat).addOnSuccessListener(docRef -> {
                        chatId = docRef.getId();
                        if (onChatReady != null) onChatReady.run();
                    });
                } else {
                    chatId = querySnapshot.getDocuments().get(0).getId();
                    if (onChatReady != null) onChatReady.run();
                }
            });
    }

    // Update sendMessage to ensure chat exists before sending
    private void sendMessage() {
        String messageText = messageInput.getText().toString().trim();
        if (messageText.isEmpty()) {
            return;
        }
        String currentUserId = mAuth.getCurrentUser().getUid();
        Message message = new Message(currentUserId, messageText, com.google.firebase.Timestamp.now());
        
        // Update chat document first
        Map<String, Object> chatUpdates = new HashMap<>();
        chatUpdates.put("lastMessageText", messageText);
        chatUpdates.put("lastMessageTime", com.google.firebase.firestore.FieldValue.serverTimestamp());
        
        db.collection("chats").document(chatId).update(chatUpdates)
            .addOnSuccessListener(aVoid -> {
                // Add the message to the messages subcollection
                db.collection("chats").document(chatId)
                    .collection("messages")
                    .add(message)
                    .addOnSuccessListener(documentReference -> {
                        // Optionally update message with its ID and chat ID
                        documentReference.update(
                            "id", documentReference.getId(),
                            "chatId", chatId,
                                "timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp()
                        );

                        // Increment unread count for other participants
                        db.collection("chats").document(chatId)
                            .get()
                            .addOnSuccessListener(chatDoc -> {
                                if (chatDoc.exists()) {
                                    List<String> participants = (List<String>) chatDoc.get("participants");
                                    if (participants != null) {
                                        Map<String, Object> updates = new HashMap<>();
                                        for (String participantId : participants) {
                                            if (!participantId.equals(currentUserId)) {
                                                updates.put("unreadCounts." + participantId, com.google.firebase.firestore.FieldValue.increment(1));
                                            }
                                        }
                                        if (!updates.isEmpty()) {
                                            db.collection("chats").document(chatId).update(updates);
                                        }
                                    }
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(ChatActivity.this, "Failed to update unread counts", Toast.LENGTH_SHORT).show();
                            });

                        messageInput.setText("");
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(ChatActivity.this, "Failed to send message", Toast.LENGTH_SHORT).show();
                    });
            })
            .addOnFailureListener(e -> {
                Toast.makeText(ChatActivity.this, "Failed to update chat", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        MenuItem favItem = menu.findItem(R.id.action_favourite);
        favItem.setIcon(isFavourite ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.action_favourite) {
            toggleFavourite();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggleFavourite() {
        String currentUserId = mAuth.getCurrentUser().getUid();
        db.collection("chats").document(chatId)
            .get()
            .addOnSuccessListener(doc -> {
                List<String> fav = (List<String>) doc.get("favourite");
                if (fav == null) fav = new ArrayList<>();
                if (fav.contains(currentUserId)) {
                    fav.remove(currentUserId);
                    isFavourite = false;
                } else {
                    fav.add(currentUserId);
                    isFavourite = true;
                }
                db.collection("chats").document(chatId)
                    .update("favourite", fav)
                    .addOnSuccessListener(aVoid -> invalidateOptionsMenu());
            });
    }

    // Show group info dialog for group chats
    private void showGroupInfoDialog() {
        db.collection("chats").document(chatId).get().addOnSuccessListener(chatDoc -> {
            if (chatDoc.exists()) {
                List<String> participants = (List<String>) chatDoc.get("participants");
                String groupName = chatDoc.getString("groupName");
                
                // Create a custom dialog layout
                View dialogView = getLayoutInflater().inflate(R.layout.dialog_group_info, null);
                TextView groupNameTextView = dialogView.findViewById(R.id.groupNameTextView);
                RecyclerView participantsRecyclerView = dialogView.findViewById(R.id.participantsRecyclerView);
                
                // Set group name
                groupNameTextView.setText(groupName);
                
                // Setup participants RecyclerView
                participantsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
                List<User> participantUsers = new ArrayList<>();
                
                // Create and show dialog
                AlertDialog dialog = new AlertDialog.Builder(this)
                    .setView(dialogView)
                    .setPositiveButton("Close", null)
                    .create();
                
                // Load participants
                db.collection("users")
                    .whereIn("uid", participants)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        participantUsers.clear();
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            User user = doc.toObject(User.class);
                            participantUsers.add(user);
                        }
                        // Set adapter with loaded users
                        participantsRecyclerView.setAdapter(new GroupParticipantsAdapter(participantUsers));
                    });
                
                dialog.show();
            }
        });
    }
}