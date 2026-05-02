package com.example.medicareplus;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView; // 🌟 تمت إضافة هذه المكتبة
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.speech.tts.TextToSpeech;
import java.util.Calendar;
import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class ElderlyDashboardActivity extends AppCompatActivity {

    private TextView tvElderlyGreeting, tvMedTime, tvMedName, tvMedDosage;
    private androidx.cardview.widget.CardView btnConfirmTaken, btnEmergencySOS;
    private LinearLayout medInfoLayout, navLogoutElderly, navChatElderly;
    private TextView tvRemainingCount;
    private androidx.cardview.widget.CardView btnAiAssistant;
    private TextToSpeech tts;
    private androidx.cardview.widget.CardView btnSpeakMed;

    private String linkedCaregiverId = null;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private String currentMedId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_elderly_dashboard);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        tvElderlyGreeting = findViewById(R.id.tvElderlyGreeting);
        tvMedTime = findViewById(R.id.tvMedTime);
        tvMedName = findViewById(R.id.tvMedName);
        tvMedDosage = findViewById(R.id.tvMedDosage);
        btnConfirmTaken = findViewById(R.id.btnConfirmTaken);
        btnEmergencySOS = findViewById(R.id.btnEmergencySOS);
        medInfoLayout = findViewById(R.id.medInfoLayout);
        tvRemainingCount = findViewById(R.id.tvRemainingCount);
        btnAiAssistant = findViewById(R.id.btnAiAssistant);
        btnSpeakMed = findViewById(R.id.btnSpeakMed);
        navChatElderly = findViewById(R.id.navChatElderly);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        navChatElderly.setOnClickListener(v -> {
            if (linkedCaregiverId != null && !linkedCaregiverId.isEmpty()) {
                Intent intent = new Intent(ElderlyDashboardActivity.this, ChatActivity.class);
                intent.putExtra("RECEIVER_ID", linkedCaregiverId);
                startActivity(intent);
            } else {
                Toast.makeText(this, "No caregiver linked to this account!", Toast.LENGTH_SHORT).show();
            }
        });

        btnAiAssistant.setOnClickListener(v -> {
            startActivity(new Intent(ElderlyDashboardActivity.this, AiAssistantActivity.class));
        });

        LinearLayout navScheduleElderly = findViewById(R.id.navScheduleElderly);
        navScheduleElderly.setOnClickListener(v -> {
            Intent intent = new Intent(ElderlyDashboardActivity.this, MyAppointmentsActivity.class);
            startActivity(intent);
        });

        loadPatientData();
        loadNextMedication();

        btnConfirmTaken.setOnClickListener(v -> {
            if (currentMedId != null) {
                confirmMedication();
            } else {
                Toast.makeText(this, "You are all caught up for today!", Toast.LENGTH_SHORT).show();
            }
        });

        btnEmergencySOS.setOnClickListener(v -> sendEmergencyAlert());

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.ENGLISH);
            }
        });

        btnSpeakMed.setOnClickListener(v -> {
            String medName = tvMedName.getText().toString();
            String medDosage = tvMedDosage.getText().toString();
            String textToRead = "Time to take " + medName + ", dosage is " + medDosage;
            tts.speak(textToRead, TextToSpeech.QUEUE_FLUSH, null, null);
        });

        androidx.cardview.widget.CardView imgProfileElderly = findViewById(R.id.imgProfileElderly);
        imgProfileElderly.setOnClickListener(v -> {
            startActivity(new Intent(ElderlyDashboardActivity.this, ProfileActivity.class));
        });
    }

    private void loadPatientData() {
        String userId = mAuth.getCurrentUser().getUid();

        db.collection("Users").document(userId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String name = documentSnapshot.getString("name");
                linkedCaregiverId = documentSnapshot.getString("linkedCaregiverId");
                tvElderlyGreeting.setText("Good Morning,\n" + name + ",");
            }
        });
    }

    private void loadNextMedication() {
        String patientId = mAuth.getCurrentUser().getUid();

        db.collection("Medications")
                .whereEqualTo("patientId", patientId)
                .whereEqualTo("isTaken", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {

                        int remainingMeds = queryDocumentSnapshots.size();
                        tvRemainingCount.setText(String.valueOf(remainingMeds));

                        QueryDocumentSnapshot nextMed = (QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments().get(0);
                        currentMedId = nextMed.getId();

                        String fetchedMedName = nextMed.getString("name");
                        tvMedName.setText(fetchedMedName);
                        tvMedDosage.setText(nextMed.getString("dosage"));
                        tvMedTime.setText(nextMed.getString("time"));

                        String medType = nextMed.getString("type");
                        ImageView pillIcon = findViewById(R.id.pillIcon);

                        if (medType != null) {
                            switch (medType) {
                                case "Syrup": pillIcon.setImageResource(R.drawable.ic_syrup); break;
                                case "Injection": pillIcon.setImageResource(R.drawable.ic_injection); break;
                                case "Inhaler": pillIcon.setImageResource(R.drawable.ic_inhaler); break;
                                case "Drops": pillIcon.setImageResource(R.drawable.ic_drops); break;
                                default: pillIcon.setImageResource(R.drawable.ic_pill); break;
                            }
                        } else {
                            pillIcon.setImageResource(R.drawable.ic_pill);
                        }

                        if (fetchedMedName != null) {
                            Calendar testTime = Calendar.getInstance();
                            testTime.add(Calendar.MINUTE, 1);
                            scheduleMedicationAlarm(fetchedMedName, testTime.get(Calendar.HOUR_OF_DAY), testTime.get(Calendar.MINUTE));
                        }

                        btnConfirmTaken.setEnabled(true);
                        btnConfirmTaken.setAlpha(1.0f);
                        medInfoLayout.setVisibility(View.VISIBLE);
                    } else {
                        currentMedId = null;
                        tvRemainingCount.setText("0");

                        tvMedName.setText("All Done!");
                        tvMedDosage.setText("No pending meds");
                        tvMedTime.setText("--:--");

                        btnConfirmTaken.setEnabled(false);
                        btnConfirmTaken.setAlpha(0.5f);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading medications.", Toast.LENGTH_SHORT).show();
                });
    }

    private void confirmMedication() {
        db.collection("Medications").document(currentMedId)
                .update("isTaken", true)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Great job! Dose confirmed ✅", Toast.LENGTH_SHORT).show();
                    loadNextMedication();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update status. Check connection.", Toast.LENGTH_SHORT).show();
                });
    }

    private void sendEmergencyAlert() {
        String patientId = mAuth.getCurrentUser().getUid();

        db.collection("Users").document(patientId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String caregiverId = documentSnapshot.getString("linkedCaregiverId");
                String patientName = documentSnapshot.getString("name");

                if (caregiverId != null && !caregiverId.isEmpty()) {
                    java.util.Map<String, Object> emergencyData = new java.util.HashMap<>();
                    emergencyData.put("patientId", patientId);
                    emergencyData.put("patientName", patientName);
                    emergencyData.put("caregiverId", caregiverId);
                    emergencyData.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());
                    emergencyData.put("status", "active");

                    db.collection("Emergencies").add(emergencyData)
                            .addOnSuccessListener(documentReference -> {
                                Toast.makeText(this, "🚨 SOS Sent! Help is on the way.", Toast.LENGTH_LONG).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to send SOS alert.", Toast.LENGTH_SHORT).show();
                            });
                } else {
                    Toast.makeText(this, "No caregiver linked to send SOS!", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void scheduleMedicationAlarm(String medName, int hourOfDay, int minute) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(this, MedicationAlarmReceiver.class);
        intent.putExtra("MED_NAME", medName);

        int requestCode = medName.hashCode();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        if (alarmManager != null) {
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}