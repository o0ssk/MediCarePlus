package com.example.medicareplus;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AddMedicationActivity extends AppCompatActivity {

    // الحاويات (الخطوات)
    private ScrollView layoutStep1, layoutStep2, layoutStep3;
    private Button btnNextStep;
    private TextView step1Label, step2Label, step3Label, btnCancel;
    private ProgressBar stepProgress;
    private int currentStep = 1;

    // 🚨 إضافات فايربيس وحقول الإدخال الجديدة 🚨
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private EditText etDrugName, etDosageValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_medication);

        // تهيئة فايربيس
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // ربط العناصر
        layoutStep1 = findViewById(R.id.layoutStep1);
        layoutStep2 = findViewById(R.id.layoutStep2);
        layoutStep3 = findViewById(R.id.layoutStep3);

        btnNextStep = findViewById(R.id.btnNextStep);
        btnCancel = findViewById(R.id.btnCancel);

        step1Label = findViewById(R.id.step1Label);
        step2Label = findViewById(R.id.step2Label);
        step3Label = findViewById(R.id.step3Label);
        stepProgress = findViewById(R.id.stepProgress);

        etDrugName = findViewById(R.id.etDrugName);
        etDosageValue = findViewById(R.id.etDosageValue);

        // زر الإلغاء يعيدك للوحة التحكم
        btnCancel.setOnClickListener(v -> finish());

        // برمجة زر التنقل الذكي
        btnNextStep.setOnClickListener(v -> {
            if (currentStep == 1) {
                // التأكد من إدخال البيانات الأساسية قبل الانتقال للخطوة التالية
                if(etDrugName.getText().toString().trim().isEmpty() || etDosageValue.getText().toString().trim().isEmpty()){
                    Toast.makeText(this, "Please enter Drug Name and Dosage", Toast.LENGTH_SHORT).show();
                    return;
                }
                currentStep = 2;
                updateWizardUI();
            } else if (currentStep == 2) {
                currentStep = 3;
                updateWizardUI();
            } else {
                // الخطوة الأخيرة: حفظ البيانات الحقيقية في Firebase
                saveMedicationData();
            }
        });
    }

    private void updateWizardUI() {
        // إخفاء كل الشاشات أولاً
        layoutStep1.setVisibility(View.GONE);
        layoutStep2.setVisibility(View.GONE);
        layoutStep3.setVisibility(View.GONE);

        // تصفير ألوان العناوين للون الرمادي
        step1Label.setTextColor(Color.parseColor("#94A3B8"));
        step2Label.setTextColor(Color.parseColor("#94A3B8"));
        step3Label.setTextColor(Color.parseColor("#94A3B8"));

        // التحديث بناءً على الخطوة الحالية
        if (currentStep == 1) {
            layoutStep1.setVisibility(View.VISIBLE);
            step1Label.setTextColor(Color.parseColor("#1142D4"));
            stepProgress.setProgress(1);
            btnNextStep.setText("Next Step →");

        } else if (currentStep == 2) {
            layoutStep2.setVisibility(View.VISIBLE);
            step2Label.setTextColor(Color.parseColor("#1142D4"));
            stepProgress.setProgress(2);
            btnNextStep.setText("Next Step →");

        } else if (currentStep == 3) {
            layoutStep3.setVisibility(View.VISIBLE);
            step3Label.setTextColor(Color.parseColor("#1142D4"));
            stepProgress.setProgress(3);
            btnNextStep.setText("Finish Adding Drug ✓");
        }
    }

    // 🚨 الدالة المسؤولة عن إرسال البيانات لفايربيس 🚨
    private void saveMedicationData() {
        String drugName = etDrugName.getText().toString().trim();
        String dosage = etDosageValue.getText().toString().trim() + " mg";

        // سنفترض مؤقتاً أن الوقت 08:00 AM، (يمكنك لاحقاً تطويرها لتأخذ الوقت من واجهة الجدولة)
        String time = "08:00 AM";

        String caregiverId = mAuth.getCurrentUser().getUid();

        btnNextStep.setEnabled(false); // إيقاف الزر لمنع الضغط المزدوج
        btnNextStep.setText("Saving...");

        // 1. نبحث عن المريض المربوط بمقدم الرعاية
        db.collection("Users").whereEqualTo("linkedCaregiverId", caregiverId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {

                        String patientId = queryDocumentSnapshots.getDocuments().get(0).getId();

                        Map<String, Object> medication = new HashMap<>();
                        medication.put("name", drugName);
                        medication.put("dosage", dosage);
                        medication.put("time", time);
                        medication.put("patientId", patientId);
                        medication.put("caregiverId", caregiverId);
                        medication.put("isTaken", false);

                        db.collection("Medications").add(medication)
                                .addOnSuccessListener(documentReference -> {
                                    Toast.makeText(this, "Medication Added Successfully! 💊", Toast.LENGTH_LONG).show();
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Failed to save medication", Toast.LENGTH_SHORT).show();
                                    btnNextStep.setEnabled(true);
                                    btnNextStep.setText("Finish Adding Drug ✓");
                                });
                    } else {
                        Toast.makeText(this, "No linked patient found! Please link a patient first.", Toast.LENGTH_LONG).show();
                        btnNextStep.setEnabled(true);
                        btnNextStep.setText("Finish Adding Drug ✓");
                    }
                });
    }
}