package com.example.medicareplus;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
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
                            Toast.makeText(this, "لا توجد أدوية مجدولة لك اليوم. نتمنى لك دوام الصحة!", Toast.LENGTH_LONG).show();
                        } else {
                            medsContainer.removeAllViews();

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String name = document.getString("name");
                                String dosage = document.getString("dosage");
                                String time = document.getString("time");

                                String docId = document.getId();
                                Boolean isTaken = document.getBoolean("isTaken");
                                if (isTaken == null) isTaken = false;

                                addMedicationCardToScreen(name, dosage, time, docId, isTaken);
                            }
                        }
                    } else {
                        Toast.makeText(this, "فشل الاتصال بقاعدة البيانات", Toast.LENGTH_SHORT).show();
                    }

                });
    }

        private void addMedicationCardToScreen(String name, String dosage, String time, String documentId, boolean isTaken) {
            View cardView = LayoutInflater.from(this).inflate(R.layout.item_medication, medsContainer, false);

            TextView tvName = cardView.findViewById(R.id.tvItemMedName);
            TextView tvDosage = cardView.findViewById(R.id.tvItemMedDosage);
            TextView tvTime = cardView.findViewById(R.id.tvItemMedTime);
            Button btnMarkAsTaken = cardView.findViewById(R.id.btnMarkAsTaken);

            tvName.setText(name);
            tvDosage.setText("الجرعة: " + dosage);
            tvTime.setText("🕒 " + time);

            if (isTaken) {
                btnMarkAsTaken.setText("تم التناول بنجاح ✨");
                btnMarkAsTaken.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.GRAY));
                btnMarkAsTaken.setEnabled(false);
            }

            btnMarkAsTaken.setOnClickListener(v -> {
                db.collection("Medications").document(documentId)
                        .update("isTaken", true)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "أحسنت! تم تسجيل تناول الدواء.", Toast.LENGTH_SHORT).show();
                            btnMarkAsTaken.setText("تم التناول بنجاح ✨");
                            btnMarkAsTaken.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.GRAY));
                            btnMarkAsTaken.setEnabled(false);
                        });
            });

            medsContainer.addView(cardView);
        }
 }
