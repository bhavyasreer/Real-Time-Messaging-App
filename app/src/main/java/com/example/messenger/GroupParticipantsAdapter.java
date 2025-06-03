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
import java.util.List;

public class GroupParticipantsAdapter extends RecyclerView.Adapter<GroupParticipantsAdapter.ParticipantViewHolder> {
    private List<User> participants;
    private Context context;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    public GroupParticipantsAdapter(List<User> participants) {
        this.participants = participants;
        this.db = FirebaseFirestore.getInstance();
        this.mAuth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public ParticipantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_participant, parent, false);
        return new ParticipantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ParticipantViewHolder holder, int position) {
        User participant = participants.get(position);
        holder.nameTextView.setText(participant.getName());
        
        holder.profileImageView.setImageDrawable(
            TextDrawableHelper.create(context, participant.getName())
        );

        // Set click listener for the participant item
        holder.itemView.setOnClickListener(v -> {
            String currentUserId = mAuth.getCurrentUser().getUid();
            String participantId = participant.getUid();
            
            // Don't open chat if clicking on self
            if (currentUserId.equals(participantId)) {
                return;
            }

            // Find or create chat between current user and selected participant
            db.collection("chats")
                .whereArrayContains("participants", currentUserId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    String chatId = null;
                    for (var doc : querySnapshot.getDocuments()) {
                        List<String> chatParticipants = (List<String>) doc.get("participants");
                        if (chatParticipants != null && 
                            chatParticipants.size() == 2 && 
                            chatParticipants.contains(participantId)) {
                            chatId = doc.getId();
                            break;
                        }
                    }

                    // If chat exists, open it
                    if (chatId != null) {
                        Intent intent = new Intent(context, ChatActivity.class);
                        intent.putExtra("chatId", chatId);
                        intent.putExtra("otherUserId", participantId);
                        intent.putExtra("isGroup", false);
                        context.startActivity(intent);
                    } else {
                        // Create new chat
                        List<String> newParticipants = List.of(currentUserId, participantId);
                        db.collection("chats")
                            .add(new Chat(newParticipants, "", null))
                            .addOnSuccessListener(docRef -> {
                                Intent intent = new Intent(context, ChatActivity.class);
                                intent.putExtra("chatId", docRef.getId());
                                intent.putExtra("otherUserId", participantId);
                                intent.putExtra("isGroup", false);
                                context.startActivity(intent);
                            });
                    }
                });
        });
    }

    @Override
    public int getItemCount() {
        return participants.size();
    }

    static class ParticipantViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView profileImageView;
        TextView nameTextView;

        ParticipantViewHolder(View itemView) {
            super(itemView);
            profileImageView = itemView.findViewById(R.id.profileImageView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
        }
    }
} 