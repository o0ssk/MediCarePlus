package com.example.medicareplus;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AddAppointmentActivity extends AppCompatActivity {

    private EditText etDoctorName, etSpecialty, etAppDate, etAppTime;
    private Button btnSaveAppointment;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_appointment);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etDoctorName = findViewById(R.id.etDoctorName);
        etSpecialty = findViewById(R.id.etSpecialty);
        etAppDate = findViewById(R.id.etAppDate);
        etAppTime = findViewById(R.id.etAppTime);
        btnSaveAppointment = findViewById(R.id.btnSaveAppointment);

        findViewById(R.id.btnBackFromAddApp).setOnClickListener(v -> finish());

        btnSaveAppointment.setOnClickListener(v -> saveAppointmentData());
    }

    private void saveAppointmentData() {
        String docName = etDoctorName.getText().toString().trim();
        String specialty = etSpecialty.getText().toString().trim();
        String date = etAppDate.getText().toString().trim();
        String time = etAppTime.getText().toString().trim();

        if (docName.isEmpty() || specialty.isEmpty() || date.isEmpty() || time.isEmpty()) {
            Toast.makeText(this, "Please fill in all clinical details.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSaveAppointment.setEnabled(false);
        btnSaveAppointment.setText("Saving...");

        String caregiverId = mAuth.getCurrentUser().getUid();

        db.collection("Users").whereEqualTo("linkedCaregiverId", caregiverId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String patientId = queryDocumentSnapshots.getDocuments().get(0).getId();

                        Map<String, Object> appointment = new HashMap<>();
                        appointment.put("doctorName", docName);
                        appointment.put("specialty", specialty);
                        appointment.put("date", date);
                        appointment.put("time", time);
                        appointment.put("patientId", patientId);
                        appointment.put("caregiverId", caregiverId);

                        db.collection("Appointments").add(appointment)
                                .addOnSuccessListener(documentReference -> {
                                    Toast.makeText(this, "Appointment Scheduled! 📅", Toast.LENGTH_LONG).show();
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Failed to schedule.", Toast.LENGTH_SHORT).show();
                                    btnSaveAppointment.setEnabled(true);
                                    btnSaveAppointment.setText("Save Appointment →");
                                });
                    } else {
                        Toast.makeText(this, "No linked patient found!", Toast.LENGTH_LONG).show();
                        btnSaveAppointment.setEnabled(true);
                        btnSaveAppointment.setText("Save Appointment →");
                    }
                });
    }
}