package com.example.medicareplus;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView; // 🌟 تم استيراد مكتبة CardView بدلاً من ImageButton
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView recyclerChat;
    private EditText etMessageChat;
    private CardView btnSendMessage;
    private TextView tvChatReceiverName, btnBackChat;

    private FirebaseFirestore db;
    private String currentUserId, receiverId, chatRoomId;
    private MessageAdapter adapter;
    private List<ChatMessage> messageList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        receiverId = getIntent().getStringExtra("RECEIVER_ID");
        chatRoomId = createChatId(currentUserId, receiverId);

        recyclerChat = findViewById(R.id.recyclerChat);
        etMessageChat = findViewById(R.id.etMessageChat);
        btnSendMessage = findViewById(R.id.btnSendMessage);
        tvChatReceiverName = findViewById(R.id.tvChatReceiverName);
        btnBackChat = findViewById(R.id.btnBackChat);

        btnBackChat.setOnClickListener(v -> finish());

        loadReceiverName();

        messageList = new ArrayList<>();
        adapter = new MessageAdapter(messageList, currentUserId);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerChat.setLayoutManager(layoutManager);

        recyclerChat.setAdapter(adapter);

        listenForMessages();

        btnSendMessage.setOnClickListener(v -> sendMessage());
    }

    private void loadReceiverName() {
        if (receiverId == null) return;

        db.collection("Users").document(receiverId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String name = documentSnapshot.getString("name");
                tvChatReceiverName.setText(name);
            } else {
                tvChatReceiverName.setText("User");
            }
        });
    }

    private String createChatId(String id1, String id2) {
        if (id1 == null || id2 == null) return "Unknown_Room";
        return id1.compareTo(id2) < 0 ? id1 + "_" + id2 : id2 + "_" + id1;
    }

    private void sendMessage() {
        String text = etMessageChat.getText().toString().trim();
        if (!text.isEmpty()) {
            ChatMessage msg = new ChatMessage(currentUserId, text, System.currentTimeMillis());
            db.collection("Chats").document(chatRoomId).collection("Messages").add(msg);
            etMessageChat.setText("");
        }
    }

    private void listenForMessages() {
        db.collection("Chats").document(chatRoomId).collection("Messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        messageList.clear();
                        messageList.addAll(value.toObjects(ChatMessage.class));
                        adapter.notifyDataSetChanged();

                        if (!messageList.isEmpty()) {
                            recyclerChat.scrollToPosition(messageList.size() - 1);
                        }
                    }
                });
    }
}