package com.example.medicareplus;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SettingsActivity extends AppCompatActivity {

    private TextView btnBackSettings, tvSettingsEmail, tvSettingsId;
    private EditText etSettingsName;
    private Button btnCopyId, btnSaveSettings;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
        } else {
            finish();
            return;
        }

        btnBackSettings = findViewById(R.id.btnBackSettings);
        tvSettingsEmail = findViewById(R.id.tvSettingsEmail);
        tvSettingsId = findViewById(R.id.tvSettingsId);
        etSettingsName = findViewById(R.id.etSettingsName);
        btnCopyId = findViewById(R.id.btnCopyId);
        btnSaveSettings = findViewById(R.id.btnSaveSettings);

        btnBackSettings.setOnClickListener(v -> finish());

        loadUserData(currentUser);

        btnCopyId.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Patient ID", currentUserId);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "ID Copied to clipboard! 📋", Toast.LENGTH_SHORT).show();
        });

        btnSaveSettings.setOnClickListener(v -> saveNameChange());
    }

    private void loadUserData(FirebaseUser user) {
        tvSettingsEmail.setText(user.getEmail());
        tvSettingsId.setText(currentUserId);

        db.collection("Users").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        etSettingsName.setText(name);
                    }
                });
    }

    private void saveNameChange() {
        String newName = etSettingsName.getText().toString().trim();

        if (newName.isEmpty()) {
            Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSaveSettings.setEnabled(false);
        btnSaveSettings.setText("Saving...");

        db.collection("Users").document(currentUserId)
                .update("name", newName)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile Updated Successfully! ✅", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                    btnSaveSettings.setEnabled(true);
                    btnSaveSettings.setText("Save Changes");
                });
    }
}