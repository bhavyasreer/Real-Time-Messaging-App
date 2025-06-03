package com.example.messenger;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.messenger.utils.TextDrawableHelper;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatViewHolder> {
    private Context context;
    private List<Chat> chatList;
    private List<Chat> filteredList;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SimpleDateFormat dateFormat;

    public ChatListAdapter(Context context, List<Chat> chatList) {
        this.context = context;
        this.chatList = chatList;
        this.filteredList = new ArrayList<>(chatList);
        this.mAuth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
        this.dateFormat = new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault());
    }

    private String formatTime(Date date) {
        return dateFormat.format(date);
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Chat chat = filteredList.get(position);
        String currentUserId = mAuth.getCurrentUser().getUid();

        // Show group name if this is a group chat
        if (chat.isGroup()) {
            String displayName = chat.getGroupName();
            if (displayName == null || displayName.isEmpty()) {
                displayName = "Group";
            }
            holder.nameTextView.setText(displayName);
            holder.profileImageView.setImageResource(R.drawable.ic_group);
            holder.lastMessageTextView.setText(chat.getLastMessageText());
            if (chat.getLastMessageTime() != null) {
                holder.timeTextView.setText(formatTime(chat.getLastMessageTime().toDate()));
            }
            // Hide online status for groups
            holder.onlineStatusIndicator.setVisibility(View.GONE);
        } else {
            // For individual chats, use the otherUserName (now always set)
            holder.nameTextView.setText(chat.getOtherUserName());
            holder.profileImageView.setImageDrawable(
                TextDrawableHelper.create(context, chat.getOtherUserName() != null ? chat.getOtherUserName() : "?")
            );
            holder.lastMessageTextView.setText(chat.getLastMessageText());
            if (chat.getLastMessageTime() != null) {
                holder.timeTextView.setText(formatTime(chat.getLastMessageTime().toDate()));
            }

            // Get the other user's ID
            String otherUserId = null;
            for (String participant : chat.getParticipants()) {
                if (!participant.equals(currentUserId)) {
                    otherUserId = participant;
                    break;
                }
            }

            // Check online status for the other user
            if (otherUserId != null) {
                db.collection("users").document(otherUserId)
                    .addSnapshotListener((snapshot, error) -> {
                        if (error != null || snapshot == null) return;
                        
                        User otherUser = snapshot.toObject(User.class);
                        if (otherUser != null) {
                            // Show online status if user is online
                            holder.onlineStatusIndicator.setVisibility(
                                otherUser.isOnline() ? View.VISIBLE : View.GONE
                            );
                        }
                    });
            }
        }

        // Set click listener for the chat item
        holder.itemView.setOnClickListener(v -> {
            if (chat.isGroup()) {
                Intent intent = new Intent(context, ChatActivity.class);
                intent.putExtra("chatId", chat.getId());
                intent.putExtra("isGroup", true);
                intent.putExtra("groupName", chat.getGroupName());
                context.startActivity(intent);
            } else {
                String otherUserId = null;
                for (String participant : chat.getParticipants()) {
                    if (!participant.equals(currentUserId)) {
                        otherUserId = participant;
                        break;
                    }
                }
                if (otherUserId != null) {
                    Intent intent = new Intent(context, ChatActivity.class);
                    intent.putExtra("chatId", chat.getId());
                    intent.putExtra("otherUserId", otherUserId);
                    intent.putExtra("isGroup", false);
                    context.startActivity(intent);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    public void filter(String text) {
        filteredList.clear();
        if (text.isEmpty()) {
            filteredList.addAll(chatList);
        } else {
            text = text.toLowerCase();
            for (Chat chat : chatList) {
                if (chat.isGroup()) {
                    // For groups, search by group name
                    if (chat.getGroupName() != null && 
                        chat.getGroupName().toLowerCase().contains(text)) {
                        filteredList.add(chat);
                    }
                } else {
                    // For individual chats, search by user name
                    if (chat.getOtherUserName() != null && 
                        chat.getOtherUserName().toLowerCase().contains(text)) {
                        filteredList.add(chat);
                    }
                }
            }
        }
        notifyDataSetChanged();
    }

    public void setChats(List<Chat> chats) {
        this.chatList = new ArrayList<>(chats);
        this.filteredList = new ArrayList<>(chatList);
        notifyDataSetChanged();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView profileImageView;
        TextView nameTextView;
        TextView lastMessageTextView;
        TextView timeTextView;
        View onlineStatusIndicator;

        ChatViewHolder(View itemView) {
            super(itemView);
            profileImageView = itemView.findViewById(R.id.profileImageView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            lastMessageTextView = itemView.findViewById(R.id.lastMessageTextView);
            timeTextView = itemView.findViewById(R.id.timeTextView);
            onlineStatusIndicator = itemView.findViewById(R.id.onlineStatusIndicator);
        }
    }
}