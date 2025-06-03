package com.example.messenger;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
    private Context context;
    private List<User> userList;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    public UserAdapter(Context context, List<User> userList) {
        this.context = context;
        this.userList = userList;
        this.mAuth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        String displayName = user.getName();
        holder.nameTextView.setText(displayName);
        // Hide email TextView
        holder.emailTextView.setVisibility(View.GONE);

        holder.itemView.setOnClickListener(v -> createOrOpenChat(user));
    }

    private void createOrOpenChat(User otherUser) {
        String currentUserId = mAuth.getCurrentUser().getUid();
        
        // Check if chat already exists
        db.collection("chats")
                .whereArrayContains("participants", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Chat chat = doc.toObject(Chat.class);
                        if (chat.getParticipants().contains(otherUser.getUid())) {
                            // Chat exists, open it
                            openChat(doc.getId(), otherUser.getUid());
                            return;
                        }
                    }
                    // No existing chat, create new one
                    createNewChat(currentUserId, otherUser);
                });
    }

    private void createNewChat(String currentUserId, User otherUser) {
        // Fetch current user's name from Firestore
        db.collection("users").document(currentUserId).get().addOnSuccessListener(currentUserDoc -> {
            String currentUserName = "";
            if (currentUserDoc.exists()) {
                User currentUser = currentUserDoc.toObject(User.class);
                if (currentUser != null && currentUser.getName() != null) {
                    currentUserName = currentUser.getName();
                }
            }

            // Store both users' names in the chat document for easy access
            java.util.Map<String, Object> chatData = new java.util.HashMap<>();
            java.util.List<String> participants = java.util.Arrays.asList(currentUserId, otherUser.getUid());
            chatData.put("participants", participants);
            chatData.put("lastMessageText", "Chat started");
            chatData.put("lastMessageTime", com.google.firebase.Timestamp.now());

            // Fix: create the userNames map outside the double-brace block
            java.util.Map<String, String> userNames = new java.util.HashMap<>();
            userNames.put(currentUserId, currentUserName);
            userNames.put(otherUser.getUid(), otherUser.getName());
            chatData.put("userNames", userNames);

        db.collection("chats")
                .add(chatData)
                .addOnSuccessListener(documentReference -> {
                    openChat(documentReference.getId(), otherUser.getUid());
                });
                });
    }

    private void openChat(String chatId, String otherUserId) {
        Intent intent = new Intent(context, ChatActivity.class);
        intent.putExtra("chatId", chatId);
        intent.putExtra("otherUserId", otherUserId);
        context.startActivity(intent);
        ((NewChatActivity) context).finish();
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        TextView emailTextView;

        UserViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            emailTextView = itemView.findViewById(R.id.emailTextView);
        }
    }
}