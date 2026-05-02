package com.example.medicareplus;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.medicareplus.models.User;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class SignUpActivity extends AppCompatActivity {

    private MaterialCardView cardSelectPatient, cardSelectCaregiver;
    private EditText etName, etSignUpEmail, etSignUpPassword;
    private Button btnSignUp;
    private TextView tvGoToLoginFromSignUp;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private String selectedRole = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        cardSelectPatient = findViewById(R.id.cardSelectPatient);
        cardSelectCaregiver = findViewById(R.id.cardSelectCaregiver);
        etName = findViewById(R.id.etName);
        etSignUpEmail = findViewById(R.id.etSignUpEmail);
        etSignUpPassword = findViewById(R.id.etSignUpPassword);
        btnSignUp = findViewById(R.id.btnSignUp);

        tvGoToLoginFromSignUp = findViewById(R.id.tvGoToLoginFromSignUp);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        cardSelectPatient.setOnClickListener(v -> {
            selectedRole = "Elderly";
            cardSelectPatient.setStrokeColor(Color.parseColor("#1142D4"));
            cardSelectPatient.setStrokeWidth(5);
            cardSelectCaregiver.setStrokeWidth(0);
        });

        cardSelectCaregiver.setOnClickListener(v -> {
            selectedRole = "Caregiver";
            cardSelectCaregiver.setStrokeColor(Color.parseColor("#1142D4"));
            cardSelectCaregiver.setStrokeWidth(5);
            cardSelectPatient.setStrokeWidth(0);
        });

        btnSignUp.setOnClickListener(v -> registerUser());

        tvGoToLoginFromSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void registerUser() {
        String name = etName.getText().toString().trim();
        String email = etSignUpEmail.getText().toString().trim();
        String password = etSignUpPassword.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedRole.isEmpty()) {
            Toast.makeText(this, "Please select who you are (Patient or Caregiver)", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Creating account...", Toast.LENGTH_SHORT).show();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String userId = mAuth.getCurrentUser().getUid();
                        User newUser = new User(userId, name, email, selectedRole, "");

                        db.collection("Users").document(userId).set(newUser)
                                .addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful()) {
                                        if (selectedRole.equals("Caregiver")) {
                                            startActivity(new Intent(SignUpActivity.this, CaregiverDashboardActivity.class));
                                        } else {
                                            startActivity(new Intent(SignUpActivity.this, ElderlyDashboardActivity.class));
                                        }
                                        finish();
                                    } else {
                                        Toast.makeText(SignUpActivity.this, "Failed to save data: " + task1.getException().getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });
                    } else {
                        Toast.makeText(SignUpActivity.this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}