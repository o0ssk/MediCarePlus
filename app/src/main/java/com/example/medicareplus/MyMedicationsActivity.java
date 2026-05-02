package com.example.medicareplus;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class MyMedicationsActivity extends AppCompatActivity {

    private LinearLayout medsContainer;
    private TextView tvBackToElderlyDash;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_medications);

        medsContainer = findViewById(R.id.medsContainer);
        tvBackToElderlyDash = findViewById(R.id.tvBackToElderlyDash);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tvBackToElderlyDash.setOnClickListener(v -> finish());

        loadMyMedications();
    }

    private void loadMyMedications() {
        String myPatientId = mAuth.getCurrentUser().getUid();

        db.collection("Medications")
                .whereEqualTo("patientId", myPatientId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().isEmpty()) {
                            Toast.makeText(this, "No medications scheduled for today. Stay healthy!", Toast.LENGTH_LONG).show();
                        } else {
                            medsContainer.removeAllViews();

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String name = document.getString("name");
                                String dosage = document.getString("dosage");
                                String time = document.getString("time");

                                String type = document.getString("type");

                                String docId = document.getId();
                                Boolean isTaken = document.getBoolean("isTaken");
                                if (isTaken == null) isTaken = false;

                                addMedicationCardToScreen(name, dosage, time, docId, isTaken, type);
                            }
                        }
                    } else {
                        Toast.makeText(this, "Failed to connect to database.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void addMedicationCardToScreen(String name, String dosage, String time, String documentId, boolean isTaken, String type) {
        View cardView = LayoutInflater.from(this).inflate(R.layout.item_medication, medsContainer, false);

        TextView tvName = cardView.findViewById(R.id.tvItemMedName);
        TextView tvDosage = cardView.findViewById(R.id.tvItemMedDosage);
        TextView tvTime = cardView.findViewById(R.id.tvItemMedTime);
        Button btnMarkAsTaken = cardView.findViewById(R.id.btnMarkAsTaken);

        ImageView imgIcon = cardView.findViewById(R.id.imgItemMedIcon);

        tvName.setText(name);
        tvDosage.setText("Dosage: " + dosage);
        tvTime.setText(time);

        if (type != null && imgIcon != null) {
            switch (type) {
                case "Syrup":
                    imgIcon.setImageResource(R.drawable.ic_syrup);
                    break;
                case "Injection":
                    imgIcon.setImageResource(R.drawable.ic_injection);
                    break;
                case "Inhaler":
                    imgIcon.setImageResource(R.drawable.ic_inhaler);
                    break;
                case "Drops":
                    imgIcon.setImageResource(R.drawable.ic_drops);
                    break;
                case "Pill":
                default:
                    imgIcon.setImageResource(R.drawable.ic_pill);
                    break;
            }
        } else if (imgIcon != null) {
            imgIcon.setImageResource(R.drawable.ic_pill);
        }

        if (isTaken) {
            btnMarkAsTaken.setText("Taken Successfully");
            btnMarkAsTaken.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#94A3B8")));
            btnMarkAsTaken.setEnabled(false);
        }

        btnMarkAsTaken.setOnClickListener(v -> {
            db.collection("Medications").document(documentId)
                    .update("isTaken", true)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Medication marked as taken.", Toast.LENGTH_SHORT).show();
                        btnMarkAsTaken.setText("Taken Successfully");
                        btnMarkAsTaken.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#94A3B8")));
                        btnMarkAsTaken.setEnabled(false);
                    });
        });

        medsContainer.addView(cardView);
    }
}