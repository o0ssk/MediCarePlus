package com.example.medicareplus;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class CaregiverDashboardActivity extends AppCompatActivity {

    private TextView tvCaregiverNameTitle, tvMedsStatus, tvLinkNewPatient;
    private MaterialCardView cardManageMedsNew, cardAppointmentsNew;
    private CardView fabQuickAdd;

    private LinearLayout navHomeCaregiver, navPatientsCaregiver, navChatCaregiver, navLogoutCaregiver;

    private RecyclerView recyclerPatientsList;
    private PatientsAdapter patientsAdapter;
    private List<Patient> patientList;

    private ListenerRegistration medsListener;
    private ListenerRegistration emergencyListener;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private String linkedPatientId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_caregiver_dashboard);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tvCaregiverNameTitle = findViewById(R.id.tvCaregiverNameTitle);
        tvMedsStatus = findViewById(R.id.tvMedsStatus);
        tvLinkNewPatient = findViewById(R.id.tvLinkNewPatient);
        cardManageMedsNew = findViewById(R.id.cardManageMedsNew);
        cardAppointmentsNew = findViewById(R.id.cardAppointmentsNew);
        fabQuickAdd = findViewById(R.id.fabQuickAdd);

        navHomeCaregiver = findViewById(R.id.navHomeCaregiver);
        navPatientsCaregiver = findViewById(R.id.navPatientsCaregiver);
        navChatCaregiver = findViewById(R.id.navChatCaregiver);


        recyclerPatientsList = findViewById(R.id.recyclerPatientsList);
        recyclerPatientsList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        patientList = new ArrayList<>();

        patientsAdapter = new PatientsAdapter(patientList, patient -> {
            linkedPatientId = patient.getId();
            Toast.makeText(this, "Monitoring: " + patient.getName(), Toast.LENGTH_SHORT).show();
            monitorSelectedPatientMeds(linkedPatientId);
        });
        recyclerPatientsList.setAdapter(patientsAdapter);

        String userId = mAuth.getCurrentUser().getUid();
        db.collection("Users").document(userId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String name = documentSnapshot.getString("name");
                tvCaregiverNameTitle.setText("Good Morning,\n" + name);
            }
        });

        tvLinkNewPatient.setOnClickListener(v -> {
            Intent intent = new Intent(this, LinkPatientActivity.class);
            startActivity(intent);
        });

        androidx.cardview.widget.CardView imgProfileCaregiver = findViewById(R.id.imgProfileCaregiver);
        imgProfileCaregiver.setOnClickListener(v -> {
            startActivity(new Intent(CaregiverDashboardActivity.this, CaregiverProfileActivity.class));
        });

        cardManageMedsNew.setOnClickListener(v -> {
            if (linkedPatientId != null) {
                Intent intent = new Intent(this, CaregiverMedsListActivity.class);
                intent.putExtra("PATIENT_ID", linkedPatientId);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Select a patient first!", Toast.LENGTH_SHORT).show();
            }
        });

        cardAppointmentsNew.setOnClickListener(v -> {
            if (linkedPatientId != null) {
                Intent intent = new Intent(this, CaregiverAppointmentsListActivity.class);
                intent.putExtra("PATIENT_ID", linkedPatientId);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Select a patient first!", Toast.LENGTH_SHORT).show();
            }
        });

        fabQuickAdd.setOnClickListener(v -> {
            if (linkedPatientId != null) {
                Intent intent = new Intent(this, AddMedicationActivity.class);
                intent.putExtra("PATIENT_ID", linkedPatientId);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Select a patient first!", Toast.LENGTH_SHORT).show();
            }
        });

        navChatCaregiver.setOnClickListener(v -> {
            if (linkedPatientId != null && !linkedPatientId.isEmpty()) {
                Intent intent = new Intent(CaregiverDashboardActivity.this, ChatActivity.class);
                intent.putExtra("RECEIVER_ID", linkedPatientId);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Select a patient first to chat!", Toast.LENGTH_SHORT).show();
            }
        });

        startEmergencyRadar();
        loadAllLinkedPatients();
    }


    private void loadAllLinkedPatients() {
        String caregiverId = mAuth.getCurrentUser().getUid();

        db.collection("Users").whereEqualTo("linkedCaregiverId", caregiverId)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    patientList.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        patientList.add(new Patient(doc.getId(), doc.getString("name")));
                    }
                    patientsAdapter.notifyDataSetChanged();

                    if (!patientList.isEmpty() && linkedPatientId == null) {
                        Patient firstPatient = patientList.get(0);
                        linkedPatientId = firstPatient.getId();
                        monitorSelectedPatientMeds(linkedPatientId);
                    } else if (patientList.isEmpty()) {
                        tvMedsStatus.setText("No linked patients found.");
                    }
                });
    }

    private void monitorSelectedPatientMeds(String patientId) {
        if (medsListener != null) {
            medsListener.remove();
        }

        medsListener = db.collection("Medications")
                .whereEqualTo("patientId", patientId)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    int totalMeds = value.size();
                    int takenMeds = 0;

                    for (DocumentSnapshot doc : value.getDocuments()) {
                        Boolean isTaken = doc.getBoolean("isTaken");
                        if (isTaken != null && isTaken) takenMeds++;
                    }

                    if (totalMeds > 0) {
                        tvMedsStatus.setText("Today: " + takenMeds + "/" + totalMeds + " meds taken.");
                        if (takenMeds == totalMeds) {
                            tvMedsStatus.setTextColor(Color.parseColor("#16A34A"));
                        } else {
                            tvMedsStatus.setTextColor(Color.parseColor("#F59E0B"));
                        }
                    } else {
                        tvMedsStatus.setText("No medications scheduled.");
                        tvMedsStatus.setTextColor(Color.parseColor("#64748B"));
                    }
                });
    }

    private void startEmergencyRadar() {
        String myCaregiverId = mAuth.getCurrentUser().getUid();

        emergencyListener = db.collection("Emergencies")
                .whereEqualTo("caregiverId", myCaregiverId)
                .whereEqualTo("status", "active")
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;

                    if (value != null && !value.isEmpty()) {
                        for (DocumentChange dc : value.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                String patientName = dc.getDocument().getString("patientName");
                                String emergencyId = dc.getDocument().getId();
                                showEmergencyDialog(patientName, emergencyId);
                            }
                        }
                    }
                });
    }

    private void showEmergencyDialog(String patientName, String emergencyId) {
        new AlertDialog.Builder(this)
                .setTitle("🚨 Emergency Alert!")
                .setMessage("Patient (" + patientName + ") needs immediate help!")
                .setCancelable(false)
                .setPositiveButton("Acknowledge", (dialog, which) -> {
                    db.collection("Emergencies").document(emergencyId)
                            .update("status", "resolved");
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (emergencyListener != null) {
            emergencyListener.remove();
        }
        if (medsListener != null) {
            medsListener.remove();
        }
    }

}