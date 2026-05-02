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

public class CaregiverAppointmentsListActivity extends AppCompatActivity {

    private LinearLayout caregiverAppointmentsContainer;
    private ExtendedFloatingActionButton fabAddAppointment;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_caregiver_appointments_list);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        caregiverAppointmentsContainer = findViewById(R.id.caregiverAppointmentsContainer);
        fabAddAppointment = findViewById(R.id.fabAddAppointment);

        findViewById(R.id.btnBackFromAppointsList).setOnClickListener(v -> finish());

        fabAddAppointment.setOnClickListener(v -> {
            startActivity(new Intent(this, AddAppointmentActivity.class));
        });

        loadPatientAppointments();
    }

    private void loadPatientAppointments() {
        String caregiverId = mAuth.getCurrentUser().getUid();

        db.collection("Appointments")
                .whereEqualTo("caregiverId", caregiverId)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    caregiverAppointmentsContainer.removeAllViews();

                    if (value.isEmpty()) {
                        Toast.makeText(this, "No appointments scheduled.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    for (DocumentSnapshot doc : value) {
                        String docId = doc.getId();
                        String doctorName = doc.getString("doctorName");
                        String specialty = doc.getString("specialty");
                        String date = doc.getString("date");
                        String time = doc.getString("time");

                        addAppointmentCard(docId, doctorName, specialty, date, time);
                    }
                });
    }

    private void addAppointmentCard(String docId, String docName, String specialty, String date, String time) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_appointment_card_modern, caregiverAppointmentsContainer, false);

        TextView tvDoc = view.findViewById(R.id.tvDoctorNameItem);
        TextView tvSpec = view.findViewById(R.id.tvSpecialtyItem);
        TextView tvDate = view.findViewById(R.id.tvDateItem);
        TextView tvTime = view.findViewById(R.id.tvTimeItem);
        View btnDelete = view.findViewById(R.id.btnDeleteAppointment);

        tvDoc.setText(docName);
        tvSpec.setText(specialty != null ? specialty.toUpperCase() : "GENERAL");
        tvDate.setText("📅 " + date);
        tvTime.setText("🕒 " + time);

        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Cancel Appointment")
                    .setMessage("Are you sure you want to cancel the appointment with " + docName + "?")
                    .setPositiveButton("Cancel Visit", (dialog, which) -> {
                        db.collection("Appointments").document(docId)
                                .delete()
                                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Appointment cancelled.", Toast.LENGTH_SHORT).show());
                    })
                    .setNegativeButton("Keep", null)
                    .show();
        });

        caregiverAppointmentsContainer.addView(view);
    }

}