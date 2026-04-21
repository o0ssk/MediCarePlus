package com.example.medicareplus;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AiAssistantActivity extends AppCompatActivity {

    private LinearLayout chatContainer;
    private EditText etMessage;
    private CardView btnSendMessage;
    private ScrollView chatScrollView;

    private static final String API_KEY = "AIzaSyARTIVRT2BMKxx89drBkQe0iOmpvCxD8W8";

    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemma-3-27b-it:generateContent?key=" + API_KEY;

    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private OkHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_assistant);

        chatContainer = findViewById(R.id.chatContainer);
        etMessage = findViewById(R.id.etMessage);
        btnSendMessage = findViewById(R.id.btnSendMessage);
        chatScrollView = findViewById(R.id.chatScrollView);

        client = new OkHttpClient();

        findViewById(R.id.btnBackFromAI).setOnClickListener(v -> finish());

        addAiMessage("Hello! I am your Medicare+ AI Assistant. How are you feeling today?");

        btnSendMessage.setOnClickListener(v -> {
            String userText = etMessage.getText().toString().trim();
            if (!userText.isEmpty()) {
                addUserMessage(userText);
                etMessage.setText("");
                addAiMessage("Thinking...");
                callGemmaAPI(userText); // ✅ Renamed for clarity
            }
        });
    }

    private void addUserMessage(String message) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_chat_user, chatContainer, false);
        TextView tv = view.findViewById(R.id.tvUserText);
        tv.setText(message);
        chatContainer.addView(view);
        scrollToBottom();
    }

    private void addAiMessage(String message) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_chat_ai, chatContainer, false);
        TextView tv = view.findViewById(R.id.tvAiText);
        tv.setText(message);
        chatContainer.addView(view);
        scrollToBottom();
    }

    private void updateLastAiMessage(String newText) {
        int childCount = chatContainer.getChildCount();
        if (childCount > 0) {
            View lastView = chatContainer.getChildAt(childCount - 1);
            TextView tv = lastView.findViewById(R.id.tvAiText);
            if (tv != null) {
                tv.setText(newText);
            }
        }
        scrollToBottom();
    }

    private void scrollToBottom() {
        chatScrollView.post(() -> chatScrollView.fullScroll(View.FOCUS_DOWN));
    }

    private void callGemmaAPI(String userMessage) {
        try {
            JSONObject jsonBody = new JSONObject();

            String systemPrompt = "You are Medicare+, a highly compassionate and simple medical AI assistant for elderly patients. " +
                    "Answer directly in 1 or 2 very short, friendly sentences. " +
                    "Never write thoughts, drafts, or internal rules. Just speak to the patient naturally.\n\n" +
                    "Patient says: " + userMessage;

            JSONArray contentsArray = new JSONArray();

            JSONObject contentObj = new JSONObject();
            JSONArray partsArray = new JSONArray();
            JSONObject textObj = new JSONObject();

            textObj.put("text", systemPrompt);
            partsArray.put(textObj);
            contentObj.put("role", "user");
            contentObj.put("parts", partsArray);
            contentsArray.put(contentObj);

            jsonBody.put("contents", contentsArray);

            RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
            Request request = new Request.Builder()
                    .url(API_URL)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> updateLastAiMessage("I'm sorry, I couldn't connect to the network. 🌐"));
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "empty";

                    if (response.isSuccessful()) {
                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);

                            String aiResponseText = jsonResponse.getJSONArray("candidates")
                                    .getJSONObject(0)
                                    .getJSONObject("content")
                                    .getJSONArray("parts")
                                    .getJSONObject(0)
                                    .getString("text");

                            aiResponseText = aiResponseText.replace("**", "").replace("*", "").trim();

                            String finalResponse = aiResponseText;
                            runOnUiThread(() -> updateLastAiMessage(finalResponse));

                        } catch (Exception e) {
                            runOnUiThread(() -> updateLastAiMessage("I couldn't understand the response. Let's try again."));
                        }
                    } else {

                        String errorMsg = "Error " + response.code() + ": " + responseBody;
                        android.util.Log.e("GemmaAPI", errorMsg);
                        runOnUiThread(() -> updateLastAiMessage("Error " + response.code() + " — check Logcat for details."));
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            updateLastAiMessage("Oops, something went wrong on my end.");
        }
    }
}