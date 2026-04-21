package com.example.medicareplus;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class CaregiverMedsListActivity extends AppCompatActivity {

    private LinearLayout medsContainer;
    private ExtendedFloatingActionButton fabAddNewMed;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_caregiver_meds_list);

        medsContainer = findViewById(R.id.medsContainer);
        fabAddNewMed = findViewById(R.id.fabAddNewMed);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        findViewById(R.id.btnBackFromMeds).setOnClickListener(v -> finish());

        fabAddNewMed.setOnClickListener(v -> {
            startActivity(new Intent(this, AddMedicationActivity.class));
        });

        loadPatientMedications();
    }

    private void loadPatientMedications() {
        String caregiverId = mAuth.getCurrentUser().getUid();

        db.collection("Users").whereEqualTo("linkedCaregiverId", caregiverId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        String patientId = querySnapshot.getDocuments().get(0).getId();

                        db.collection("Medications")
                                .whereEqualTo("patientId", patientId)
                                .addSnapshotListener((value, error) -> {
                                    if (error != null || value == null) return;

                                    medsContainer.removeAllViews();

                                    if (value.isEmpty()) {
                                        Toast.makeText(this, "No medications added yet.", Toast.LENGTH_SHORT).show();
                                        return;
                                    }

                                    for (DocumentSnapshot doc : value.getDocuments()) {
                                        String medId = doc.getId();
                                        String name = doc.getString("name");
                                        String dosage = doc.getString("dosage");
                                        String time = doc.getString("time");

                                        addMedicationCardToUI(medId, name, dosage, time);
                                    }
                                });
                    } else {
                        Toast.makeText(this, "Please link a patient first.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void addMedicationCardToUI(String medId, String name, String dosage, String time) {
        View cardView = LayoutInflater.from(this).inflate(R.layout.item_medication_card_modern, medsContainer, false);

        TextView tvName = cardView.findViewById(R.id.tvMedNameItem);
        TextView tvDosage = cardView.findViewById(R.id.tvMedDosageItem);
        TextView tvTime = cardView.findViewById(R.id.tvMedTimeItem);
        View btnDelete = cardView.findViewById(R.id.btnDeleteMed);

        tvName.setText(name);
        tvDosage.setText(dosage);
        tvTime.setText("Scheduled for " + time);

        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Medication")
                    .setMessage("Are you sure you want to remove " + name + " from the schedule?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        db.collection("Medications").document(medId)
                                .delete()
                                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Medication deleted", Toast.LENGTH_SHORT).show());
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        medsContainer.addView(cardView);
    }
}