class ChatActivity {package com.example.messenger

    import android.os.Bundle
    import android.widget.Toast
    import androidx.appcompat.app.AppCompatActivity
    import androidx.recyclerview.widget.LinearLayoutManager
    import com.example.messenger.databinding.ActivityChatBinding
    import com.google.firebase.auth.FirebaseAuth
    import com.google.firebase.firestore.*
    import java.util.*

    data class Message(
        val text: String = "",
        val sender: String = "",
        val timestamp: Long = 0
    )

    class ChatActivity : AppCompatActivity() {
        private lateinit var binding: ActivityChatBinding
        private val db = FirebaseFirestore.getInstance()
        private val auth = FirebaseAuth.getInstance()
        private val messages = mutableListOf<Message>()
        private lateinit var adapter: MessageAdapter

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            binding = ActivityChatBinding.inflate(layoutInflater)
            setContentView(binding.root)

            adapter = MessageAdapter(messages, auth.currentUser?.email ?: "")
            binding.recyclerView.layoutManager = LinearLayoutManager(this)
            binding.recyclerView.adapter = adapter

            binding.btnSend.setOnClickListener {
                val msg = binding.etMessage.text.toString()
                val user = auth.currentUser?.email ?: "Anonymous"
                if (msg.isNotEmpty()) {
                    db.collection("messages").add(
                        Message(text = msg, sender = user, timestamp = System.currentTimeMillis())
                    )
                    binding.etMessage.setText("")
                }
            }

            db.collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener { value, error ->
                    if (error != null) {
                        Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }
                    messages.clear()
                    for (doc in value!!.documents) {
                        doc.toObject(Message::class.java)?.let { messages.add(it) }
                    }
                    adapter.notifyDataSetChanged()
                    binding.recyclerView.scrollToPosition(messages.size - 1)
                }
        }
    }
}
