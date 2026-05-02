package com.example.medicareplus;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class CaregiverProfileActivity extends AppCompatActivity {

    private EditText etCaregiverName;
    private TextView tvCaregiverEmail, tvCaregiverId, btnBack;
    private Button btnSave;
    private FirebaseFirestore db;
    private String caregiverUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_caregiver_profile);

        db = FirebaseFirestore.getInstance();
        caregiverUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        etCaregiverName = findViewById(R.id.etCaregiverProfileName);
        tvCaregiverEmail = findViewById(R.id.tvProfileEmail);
        tvCaregiverId = findViewById(R.id.tvCaregiverProfileId);
        btnBack = findViewById(R.id.btnBackCaregiverProfile);
        btnSave = findViewById(R.id.btnSaveCaregiverProfile);

        btnBack.setOnClickListener(v -> finish());

        loadCaregiverData();

        btnSave.setOnClickListener(v -> updateCaregiverProfile());

        Button btnLogoutCaregiverProfile = findViewById(R.id.btnLogoutCaregiverProfile);
        btnLogoutCaregiverProfile.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(CaregiverProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void loadCaregiverData() {
        tvCaregiverEmail.setText(FirebaseAuth.getInstance().getCurrentUser().getEmail());
        tvCaregiverId.setText("User UID: " + caregiverUid);

        db.collection("Users").document(caregiverUid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                etCaregiverName.setText(doc.getString("name"));
            }
        });
    }

    private void updateCaregiverProfile() {
        String newName = etCaregiverName.getText().toString().trim();
        if (newName.isEmpty()) {
            Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);
        btnSave.setText("Updating...");

        db.collection("Users").document(caregiverUid).update("name", newName)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile updated successfully! ✅", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Update failed.", Toast.LENGTH_SHORT).show();
                    btnSave.setEnabled(true);
                    btnSave.setText("Save Profile Updates ✓");
                });
    }
}