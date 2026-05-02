package com.example.medicareplus;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AddMedicationActivity extends AppCompatActivity {


    private ScrollView layoutStep1, layoutStep2, layoutStep3;
    private Button btnNextStep;
    private TextView step1Label, step2Label, step3Label, btnCancel;
    private ProgressBar stepProgress;
    private int currentStep = 1;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private EditText etDrugName, etDosageValue, etTimePicker;
    private RadioGroup rgMedType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_medication);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

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

        rgMedType = findViewById(R.id.rgMedType);
        etTimePicker = findViewById(R.id.etTimePicker);

        btnCancel.setOnClickListener(v -> finish());

        btnNextStep.setOnClickListener(v -> {
            if (currentStep == 1) {
                if(etDrugName.getText().toString().trim().isEmpty() || etDosageValue.getText().toString().trim().isEmpty()){
                    Toast.makeText(this, "Please enter Drug Name and Dosage", Toast.LENGTH_SHORT).show();
                    return;
                }
                currentStep = 2;
                updateWizardUI();
            } else if (currentStep == 2) {
                if(etTimePicker.getText().toString().trim().isEmpty()){
                    Toast.makeText(this, "Please set the medication time", Toast.LENGTH_SHORT).show();
                    return;
                }
                currentStep = 3;
                updateWizardUI();
            } else {
                saveMedicationData();
            }
        });
    }

    private void updateWizardUI() {
        layoutStep1.setVisibility(View.GONE);
        layoutStep2.setVisibility(View.GONE);
        layoutStep3.setVisibility(View.GONE);

        step1Label.setTextColor(Color.parseColor("#94A3B8"));
        step2Label.setTextColor(Color.parseColor("#94A3B8"));
        step3Label.setTextColor(Color.parseColor("#94A3B8"));

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

    private void saveMedicationData() {
        String drugName = etDrugName.getText().toString().trim();
        String dosage = etDosageValue.getText().toString().trim() + " mg";

        String time = etTimePicker.getText().toString().trim();

        String medType;
        if (rgMedType != null) {
            int selectedId = rgMedType.getCheckedRadioButtonId();
            if (selectedId == R.id.rbSyrup) medType = "Syrup";
            else if (selectedId == R.id.rbInjection) medType = "Injection";
            else if (selectedId == R.id.rbInhaler) medType = "Inhaler";
            else if (selectedId == R.id.rbDrops) medType = "Drops";
            else {
                medType = "Pill";
            }
        } else {
            medType = "Pill";
        }

        String caregiverId = mAuth.getCurrentUser().getUid();

        btnNextStep.setEnabled(false);
        btnNextStep.setText("Saving...");

        db.collection("Users").whereEqualTo("linkedCaregiverId", caregiverId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {

                        String patientId = queryDocumentSnapshots.getDocuments().get(0).getId();

                        Map<String, Object> medication = new HashMap<>();
                        medication.put("name", drugName);
                        medication.put("dosage", dosage);
                        medication.put("time", time);
                        medication.put("type", medType);
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