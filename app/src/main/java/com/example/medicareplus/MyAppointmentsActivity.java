package com.example.medicareplus;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MyAppointmentsActivity extends AppCompatActivity {

    private LinearLayout elderlyAppointmentsContainer;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_appointments);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        elderlyAppointmentsContainer = findViewById(R.id.elderlyAppointmentsContainer);

        findViewById(R.id.btnBackFromElderlyApp).setOnClickListener(v -> finish());

        loadMyUpcomingAppointments();
    }

    private void loadMyUpcomingAppointments() {
        String myPatientId = mAuth.getCurrentUser().getUid();

        db.collection("Appointments")
                .whereEqualTo("patientId", myPatientId)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    elderlyAppointmentsContainer.removeAllViews();

                    if (value.isEmpty()) {
                        Toast.makeText(this, "No upcoming appointments.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    for (DocumentSnapshot doc : value.getDocuments()) {
                        String doctorName = doc.getString("doctorName");
                        String specialty = doc.getString("specialty");
                        String date = doc.getString("date");
                        String time = doc.getString("time");

                        addElderlyAppointmentCard(doctorName, specialty, date, time);
                    }
                });
    }

    private void addElderlyAppointmentCard(String docName, String specialty, String date, String time) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_elderly_appointment, elderlyAppointmentsContainer, false);

        TextView tvDoc = view.findViewById(R.id.tvElderlyDocName);
        TextView tvSpec = view.findViewById(R.id.tvElderlySpecialty);
        TextView tvDate = view.findViewById(R.id.tvElderlyAppDate);
        TextView tvTime = view.findViewById(R.id.tvElderlyAppTime);

        tvDoc.setText(docName);
        tvSpec.setText(specialty != null ? specialty.toUpperCase() : "GENERAL");
        tvDate.setText(date);
        tvTime.setText("at " + time);

        elderlyAppointmentsContainer.addView(view);
    }
}