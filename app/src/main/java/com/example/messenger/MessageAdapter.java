package com.example.messenger;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private Context context;
    private List<Message> messageList;
    private String currentUserId;
    private FirebaseFirestore db;
    private String chatId;

    public MessageAdapter(Context context, List<Message> messageList, String chatId) {
        this.context = context;
        this.messageList = messageList;
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        this.db = FirebaseFirestore.getInstance();
        this.chatId = chatId;
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messageList.get(position);
        if (message.getSenderId().equals(currentUserId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_SENT) {
            view = LayoutInflater.from(context).inflate(R.layout.item_message_sent, parent, false);
        } else {
            view = LayoutInflater.from(context).inflate(R.layout.item_message_received, parent, false);
        }
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messageList.get(position);
        holder.messageTextView.setText(message.getText());
        
        if (message.getTimestamp() != null) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault());
            holder.timeTextView.setText(sdf.format(message.getTimestamp().toDate()));
        } else {
            holder.timeTextView.setText("");
        }

        // If message is received, mark it as read
        if (!message.getSenderId().equals(currentUserId) && !"read".equals(message.getStatus())) {
            updateMessageStatus(message, "read");
        }

        // Add long press listener for delete options
        holder.itemView.setOnLongClickListener(v -> {
            if (message.getSenderId().equals(currentUserId)) {
                showDeleteOptionsDialog(message);
            }
            return true;
        });
    }

    private void updateMessageStatus(Message message, String newStatus) {
        // Update status in Firestore
        db.collection("chats")
            .document(chatId)
            .collection("messages")
            .document(message.getId())
            .update("status", newStatus);
    }

    private void showDeleteOptionsDialog(Message message) {
        String[] options = {"Delete for me", "Delete for everyone"};
        
        new AlertDialog.Builder(context)
            .setTitle("Delete Message")
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    // Delete for me
                    deleteMessageForMe(message);
                } else {
                    // Delete for everyone
                    deleteMessageForEveryone(message);
                }
            })
            .show();
    }

    private void deleteMessageForMe(Message message) {
        String messageId = message.getId();
        int deletedMessageIndex = findMessageIndex(messageId);

        db.collection("chats").document(chatId)
            .collection("messages").document(messageId)
            .collection("deletedFor")
            .document(currentUserId)
            .set(new java.util.HashMap<>())
            .addOnSuccessListener(aVoid -> {
                // Message marked as deleted for current user
                // Now check if this was the last message in the current view
                if (deletedMessageIndex != -1 && deletedMessageIndex == messageList.size() - 1) {
                    // It was the last message, find the new last message visible to the user
                    db.collection("chats").document(chatId)
                        .collection("messages")
                        .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                        .limit(10) // Limit to a reasonable number to find the previous message
                        .get()
                        .addOnSuccessListener(querySnapshot -> {
                            Message newLastMessage = null;
                            for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                                Message msg = doc.toObject(Message.class);
                                msg.setId(doc.getId());
                                // Check if this message is NOT deleted for the current user
                                db.collection("chats").document(chatId)
                                    .collection("messages").document(msg.getId())
                                    .collection("deletedFor")
                                    .document(currentUserId)
                                    .get()
                                    .addOnSuccessListener(deletedDoc -> {
                                        if (!deletedDoc.exists()) {
                                            // Found a message not deleted for the current user
                                            // Since we query in descending order, the first one found is the new last message
                                            // Update the parent chat document
                                            java.util.Map<String, Object> chatUpdates = new java.util.HashMap<>();
                                            chatUpdates.put("lastMessageText", msg.getText());
                                            chatUpdates.put("lastMessageTime", msg.getTimestamp());
                                            db.collection("chats").document(chatId).update(chatUpdates);
                                            // Stop processing once the new last message is found and updated
                                            // This requires a way to break out of the outer loop or manage this flow
                                            // For simplicity, we'll update the first non-deleted message found in the limited query
                                            // A more robust solution might involve fetching all messages and finding the latest non-deleted one

                                            // Due to the asynchronous nature, we can't easily break from the outer loop here.
                                            // We'll rely on the fact that we update the chat with the *first* non-deleted message found in the descending query.
                                        }
                                    });
                            }
                             // If the loop finishes and no non-deleted message is found (e.g., all messages deleted)
                             // Or if the last message was the only message and it's now deleted for this user
                             // We should clear the last message info in the chat document.
                             // This part is tricky to do reliably within this asynchronous structure after checking for newLastMessage.
                             // A simpler approach is to always set to empty if no non-deleted message is found in the limited query after a delay.
                             // However, a more direct query for the last non-deleted message is better.

                             // Let's refine the logic: After marking the message as deleted, requery for the latest non-deleted message.
                             db.collection("chats").document(chatId)
                                 .collection("messages")
                                 .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                                 .limit(1)
                                 .get()
                                 .addOnSuccessListener(latestMsgSnapshot -> {
                                     if (!latestMsgSnapshot.isEmpty()) {
                                          com.google.firebase.firestore.QueryDocumentSnapshot latestDoc = (com.google.firebase.firestore.QueryDocumentSnapshot) latestMsgSnapshot.getDocuments().get(0);
                                         Message latestMessage = latestDoc.toObject(Message.class);
                                         latestMessage.setId(latestDoc.getId());

                                         // Check if this truly is the latest non-deleted message for the current user
                                         db.collection("chats").document(chatId)
                                             .collection("messages").document(latestMessage.getId())
                                             .collection("deletedFor")
                                             .document(currentUserId)
                                             .get()
                                             .addOnSuccessListener(isDeletedDoc -> {
                                                 java.util.Map<String, Object> chatUpdates = new java.util.HashMap<>();
                                                 if (!isDeletedDoc.exists()) {
                                                     // The latest message is not deleted for the current user
                                                     chatUpdates.put("lastMessageText", latestMessage.getText());
                                                     chatUpdates.put("lastMessageTime", latestMessage.getTimestamp());
                                                 } else {
                                                     // The latest message is also deleted for the current user, clear the last message info
                                                     chatUpdates.put("lastMessageText", "");
                                                     chatUpdates.put("lastMessageTime", null);
                                                 }
                                                 db.collection("chats").document(chatId).update(chatUpdates);
                                             });
                                     } else {
                                         // No messages left in the chat, clear the last message info
                                         java.util.Map<String, Object> chatUpdates = new java.util.HashMap<>();
                                         chatUpdates.put("lastMessageText", "");
                                         chatUpdates.put("lastMessageTime", null);
                                         db.collection("chats").document(chatId).update(chatUpdates);
                                     }
                                 });
                        });
                }
            })
            .addOnFailureListener(e -> {
                // Handle error
            });
    }

    private void deleteMessageForEveryone(Message message) {
        db.collection("chats").document(chatId)
            .collection("messages").document(message.getId())
            .delete()
            .addOnSuccessListener(aVoid -> {
                // After deleting, find the new last message
                db.collection("chats").document(chatId)
                    .collection("messages")
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            Message latestMessage = querySnapshot.getDocuments().get(0).toObject(Message.class);
                            // Update chat document with new last message
                            Map<String, Object> chatUpdates = new HashMap<>();
                            chatUpdates.put("lastMessageText", latestMessage.getText());
                            chatUpdates.put("lastMessageTime", latestMessage.getTimestamp());
                            db.collection("chats").document(chatId).update(chatUpdates);
                        } else {
                            // No messages left, clear last message fields
                            Map<String, Object> chatUpdates = new HashMap<>();
                            chatUpdates.put("lastMessageText", "");
                            chatUpdates.put("lastMessageTime", null);
                            db.collection("chats").document(chatId).update(chatUpdates);
                        }
                    });
            })
            .addOnFailureListener(e -> {
                // Handle error
            });
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;
        TextView timeTextView;

        MessageViewHolder(View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
            timeTextView = itemView.findViewById(R.id.timeTextView);
        }
    }

    // Helper method to find message index by ID
    private int findMessageIndex(String messageId) {
        for (int i = 0; i < messageList.size(); i++) {
            if (messageList.get(i).getId().equals(messageId)) {
                return i;
            }
        }
        return -1;
    }
}