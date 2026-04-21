package com.example.medicareplus;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class CaregiverDashboardActivity extends AppCompatActivity {

    // 1. تعريف العناصر الجديدة المطابقة لملف التصميم
    private TextView tvCaregiverNameTitle, tvMedsStatus, tvLinkNewPatient, tvPatientNameLabel, navLogout;
    private MaterialCardView cardManageMedsNew, cardAppointmentsNew;
    private CardView fabQuickAdd;

    private ListenerRegistration medsListener;
    private ListenerRegistration emergencyListener;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_caregiver_dashboard);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 2. ربط العناصر بالـ IDs الجديدة
        tvCaregiverNameTitle = findViewById(R.id.tvCaregiverNameTitle);
        tvMedsStatus = findViewById(R.id.tvMedsStatus);
        tvLinkNewPatient = findViewById(R.id.tvLinkNewPatient);
        tvPatientNameLabel = findViewById(R.id.tvPatientNameLabel);
        navLogout = findViewById(R.id.navLogout);
        cardManageMedsNew = findViewById(R.id.cardManageMedsNew);
        cardAppointmentsNew = findViewById(R.id.cardAppointmentsNew);
        fabQuickAdd = findViewById(R.id.fabQuickAdd);

        // 3. جلب اسم مقدم الرعاية وعرضه بالأنجليزية
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("Users").document(userId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String name = documentSnapshot.getString("name");
                tvCaregiverNameTitle.setText("Good Morning,\n" + name);
            }
        });

        // 4. برمجة الأحداث للبطاقات الجديدة
        tvLinkNewPatient.setOnClickListener(v -> {
            Intent intent = new Intent(this, LinkPatientActivity.class);
            startActivity(intent);
        });

        cardManageMedsNew.setOnClickListener(v -> {
            Intent intent = new Intent(this, CaregiverMedsListActivity.class);
            startActivity(intent);
        });

        cardAppointmentsNew.setOnClickListener(v -> {
            Intent intent = new Intent(this, CaregiverAppointmentsListActivity.class);
            startActivity(intent);
        });

        // زر الإضافة العائم السريع (يأخذك لإضافة دواء كإجراء سريع)
        fabQuickAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddMedicationActivity.class);
            startActivity(intent);
        });

        // تسجيل الخروج من الشريط السفلي
        navLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(CaregiverDashboardActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

        // تشغيل الرادارات (الاستماع اللحظي)
        startEmergencyRadar();
        monitorPatientMedications();
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
        // تنبيه الطوارئ باللغة الإنجليزية ليتناسب مع الواجهة
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

    private void monitorPatientMedications() {
        String caregiverId = mAuth.getCurrentUser().getUid();

        db.collection("Users").whereEqualTo("linkedCaregiverId", caregiverId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {

                        DocumentSnapshot patientDoc = queryDocumentSnapshots.getDocuments().get(0);
                        String patientId = patientDoc.getId();
                        String patientName = patientDoc.getString("name");

                        // وضع اسم المريض الحقيقي في البطاقة بدلاً من كلمة "Patient" الثابتة
                        if(patientName != null) {
                            tvPatientNameLabel.setText(patientName);
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

                                    // تحديث حالة الأدوية باللغة الإنجليزية وألوان متناسقة مع التصميم
                                    if (totalMeds > 0) {
                                        tvMedsStatus.setText("Today: " + takenMeds + "/" + totalMeds + " meds taken.");
                                        if (takenMeds == totalMeds) {
                                            tvMedsStatus.setTextColor(Color.parseColor("#16A34A")); // لون أخضر
                                        } else {
                                            tvMedsStatus.setTextColor(Color.parseColor("#F59E0B")); // لون برتقالي
                                        }
                                    } else {
                                        tvMedsStatus.setText("No medications scheduled.");
                                        tvMedsStatus.setTextColor(Color.parseColor("#64748B")); // لون رمادي
                                    }
                                });
                    } else {
                        tvMedsStatus.setText("No linked patient found.");
                    }
                });
    }
}