package com.example.medicareplus;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class LinkPatientActivity extends AppCompatActivity {

    private EditText etPatientId;
    private Button btnConfirmLink;
    private TextView btnBack;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_link_patient);

        etPatientId = findViewById(R.id.etPatientId);
        btnConfirmLink = findViewById(R.id.btnConfirmLink);
        btnBack = findViewById(R.id.btnBack);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnBack.setOnClickListener(v -> finish());

        btnConfirmLink.setOnClickListener(v -> linkPatientToCaregiver());
    }

    private void linkPatientToCaregiver() {
        String shortCodeInput = etPatientId.getText().toString().trim();

        if (shortCodeInput.isEmpty() || shortCodeInput.length() < 5) {
            Toast.makeText(this, "Please enter a valid 6-digit code.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnConfirmLink.setEnabled(false);
        btnConfirmLink.setText("Searching & Linking...");

        String caregiverId = mAuth.getCurrentUser().getUid();

        db.collection("Users").whereEqualTo("shortCode", shortCodeInput).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {

                        String patientId = queryDocumentSnapshots.getDocuments().get(0).getId();

                        db.collection("Users").document(patientId)
                                .update("linkedCaregiverId", caregiverId)
                                .addOnSuccessListener(aVoid -> {

                                    db.collection("Users").document(caregiverId)
                                            .update("linkedPatientId", patientId)
                                            .addOnSuccessListener(aVoid2 -> {
                                                Toast.makeText(this, "Success! Two-way link established. 🔗", Toast.LENGTH_LONG).show();
                                                finish();
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(this, "Linked patient, but failed to update caregiver.", Toast.LENGTH_SHORT).show();
                                                resetButton();
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Failed to link patient. Check connection.", Toast.LENGTH_SHORT).show();
                                    resetButton();
                                });
                    } else {
                        Toast.makeText(this, "Invalid Code. Patient not found!", Toast.LENGTH_LONG).show();
                        resetButton();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error finding patient.", Toast.LENGTH_SHORT).show();
                    resetButton();
                });
    }

    private void resetButton() {
        btnConfirmLink.setEnabled(true);
        btnConfirmLink.setText("Confirm Link →");
    }
}