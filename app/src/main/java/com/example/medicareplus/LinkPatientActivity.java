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
        String patientId = etPatientId.getText().toString().trim();

        if (patientId.isEmpty()) {
            Toast.makeText(this, "Please enter the Patient ID.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnConfirmLink.setEnabled(false);
        btnConfirmLink.setText("Linking...");

        String caregiverId = mAuth.getCurrentUser().getUid();

        db.collection("Users").document(patientId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {

                        db.collection("Users").document(patientId)
                                .update("linkedCaregiverId", caregiverId)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Success! Patient linked. 🔗", Toast.LENGTH_LONG).show();
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Failed to link patient. Check connection.", Toast.LENGTH_SHORT).show();
                                    resetButton();
                                });
                    } else {
                        Toast.makeText(this, "Invalid ID. Patient not found!", Toast.LENGTH_LONG).show();
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