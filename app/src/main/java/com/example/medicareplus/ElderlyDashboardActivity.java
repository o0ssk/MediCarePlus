package com.example.medicareplus;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class ElderlyDashboardActivity extends AppCompatActivity {

    // 1. تعريف العناصر
    private TextView tvElderlyGreeting, tvMedTime, tvMedName, tvMedDosage;
    private androidx.cardview.widget.CardView btnConfirmTaken, btnEmergencySOS; // تغيير المتغير هنا
    private LinearLayout medInfoLayout, navLogoutElderly;
    private TextView tvRemainingCount;
    private androidx.cardview.widget.CardView btnAiAssistant;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private String currentMedId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_elderly_dashboard);

        // 2. ربط العناصر بالـ IDs
        tvElderlyGreeting = findViewById(R.id.tvElderlyGreeting);
        tvMedTime = findViewById(R.id.tvMedTime);
        tvMedName = findViewById(R.id.tvMedName);
        tvMedDosage = findViewById(R.id.tvMedDosage);
        btnConfirmTaken = findViewById(R.id.btnConfirmTaken);
        btnEmergencySOS = findViewById(R.id.btnEmergencySOS);
        medInfoLayout = findViewById(R.id.medInfoLayout);
        tvRemainingCount = findViewById(R.id.tvRemainingCount);
        navLogoutElderly = findViewById(R.id.navLogoutElderly);
        btnAiAssistant = findViewById(R.id.btnAiAssistant);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnAiAssistant.setOnClickListener(v -> {
            startActivity(new Intent(ElderlyDashboardActivity.this, AiAssistantActivity.class));
        });

        LinearLayout navScheduleElderly = findViewById(R.id.navScheduleElderly);
        navScheduleElderly.setOnClickListener(v -> {
            Intent intent = new Intent(ElderlyDashboardActivity.this, MyAppointmentsActivity.class);
            startActivity(intent);
        });

        navLogoutElderly.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(ElderlyDashboardActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        });

        loadPatientData();
        loadNextMedication();

        btnConfirmTaken.setOnClickListener(v -> {
            if (currentMedId != null) {
                confirmMedication();
            } else {
                Toast.makeText(this, "You are all caught up for today!", Toast.LENGTH_SHORT).show();
            }
        });

        btnEmergencySOS.setOnClickListener(v -> sendEmergencyAlert());
    }

    private void loadPatientData() {
        String userId = mAuth.getCurrentUser().getUid();

        db.collection("Users").document(userId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String name = documentSnapshot.getString("name");
                tvElderlyGreeting.setText("Good Morning,\n" + name + ",");
            }
        });
    }

    private void loadNextMedication() {
        String patientId = mAuth.getCurrentUser().getUid();

        db.collection("Medications")
                .whereEqualTo("patientId", patientId)
                .whereEqualTo("isTaken", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {

                        int remainingMeds = queryDocumentSnapshots.size();
                        tvRemainingCount.setText(String.valueOf(remainingMeds));

                        QueryDocumentSnapshot nextMed = (QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments().get(0);
                        currentMedId = nextMed.getId();

                        tvMedName.setText(nextMed.getString("name"));
                        tvMedDosage.setText(nextMed.getString("dosage"));
                        tvMedTime.setText(nextMed.getString("time"));

                        btnConfirmTaken.setEnabled(true);
                        btnConfirmTaken.setAlpha(1.0f);
                        medInfoLayout.setVisibility(View.VISIBLE);
                    } else {
                        currentMedId = null;
                        tvRemainingCount.setText("0");

                        tvMedName.setText("All Done! 🎉");
                        tvMedDosage.setText("No pending meds");
                        tvMedTime.setText("--:--");

                        btnConfirmTaken.setEnabled(false);
                        btnConfirmTaken.setAlpha(0.5f);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading medications.", Toast.LENGTH_SHORT).show();
                });
    }

    private void confirmMedication() {
        db.collection("Medications").document(currentMedId)
                .update("isTaken", true)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Great job! Dose confirmed ✅", Toast.LENGTH_SHORT).show();
                    loadNextMedication();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update status. Check connection.", Toast.LENGTH_SHORT).show();
                });
    }

    private void sendEmergencyAlert() {
        String patientId = mAuth.getCurrentUser().getUid();

        db.collection("Users").document(patientId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String caregiverId = documentSnapshot.getString("linkedCaregiverId");
                String patientName = documentSnapshot.getString("name");

                if (caregiverId != null && !caregiverId.isEmpty()) {
                    java.util.Map<String, Object> emergencyData = new java.util.HashMap<>();
                    emergencyData.put("patientId", patientId);
                    emergencyData.put("patientName", patientName);
                    emergencyData.put("caregiverId", caregiverId);
                    emergencyData.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());
                    emergencyData.put("status", "active");

                    db.collection("Emergencies").add(emergencyData)
                            .addOnSuccessListener(documentReference -> {
                                Toast.makeText(this, "🚨 SOS Sent! Help is on the way.", Toast.LENGTH_LONG).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to send SOS alert.", Toast.LENGTH_SHORT).show();
                            });
                } else {
                    Toast.makeText(this, "No caregiver linked to send SOS!", Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}