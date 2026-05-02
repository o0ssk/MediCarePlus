package com.example.medicareplus;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView; // 🌟 تم استيراد مكتبة ImageView
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Random;

public class ProfileActivity extends AppCompatActivity {

    private EditText etProfileName;
    private TextView tvProfileEmail, tvProfileId, btnBackProfile;
    private ImageView btnCopyIdProfile;
    private Button btnSaveProfile;

    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        etProfileName = findViewById(R.id.etProfileName);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        tvProfileId = findViewById(R.id.tvProfileId);
        btnCopyIdProfile = findViewById(R.id.btnCopyIdProfile);
        btnBackProfile = findViewById(R.id.btnBackProfile);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);

        btnBackProfile.setOnClickListener(v -> finish());

        loadProfileData();

        btnCopyIdProfile.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Patient Code", tvProfileId.getText().toString().replace("Code: ", ""));
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Code Copied! Send it to your caregiver.", Toast.LENGTH_SHORT).show();
        });

        btnSaveProfile.setOnClickListener(v -> updateName());

        Button btnLogoutProfile = findViewById(R.id.btnLogoutProfile);
        btnLogoutProfile.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void loadProfileData() {
        tvProfileEmail.setText(FirebaseAuth.getInstance().getCurrentUser().getEmail());

        db.collection("Users").document(userId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                etProfileName.setText(doc.getString("name"));

                String shortCode = doc.getString("shortCode");
                if (shortCode != null && !shortCode.isEmpty()) {
                    tvProfileId.setText("Code: " + shortCode);
                } else {
                    generateShortCode();
                }
            }
        });
    }

    private void generateShortCode() {
        Random rnd = new Random();
        int number = rnd.nextInt(999999);
        String generatedCode = String.format("%06d", number);

        db.collection("Users").document(userId).update("shortCode", generatedCode)
                .addOnSuccessListener(aVoid -> tvProfileId.setText("Code: " + generatedCode));
    }

    private void updateName() {
        String newName = etProfileName.getText().toString().trim();
        if (newName.isEmpty()) return;

        db.collection("Users").document(userId).update("name", newName)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile Updated!", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }
}