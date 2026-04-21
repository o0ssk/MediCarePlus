package com.example.medicareplus;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail;
    private EditText etPassword;
    private Button btnLogin;
    private TextView tvGoToSignUp;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnLogin = findViewById(R.id.btnLogin);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        tvGoToSignUp = findViewById(R.id.tvGoToSignUp);

        btnLogin.setOnClickListener(v -> loginUser());

        tvGoToSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mAuth.getCurrentUser() != null) {
            checkUserRoleAndRedirect(mAuth.getCurrentUser().getUid());
        }
    }

    private void loginUser() {
        String emailUser = etEmail.getText().toString().trim();
        String passUser = etPassword.getText().toString().trim();

        if (emailUser.isEmpty()) {
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
            return;
        }
        if (passUser.isEmpty()) {
            Toast.makeText(this, "Please enter the password", Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Logging in...");

        mAuth.signInWithEmailAndPassword(emailUser, passUser)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String userId = mAuth.getCurrentUser().getUid();
                        checkUserRoleAndRedirect(userId);
                    } else {
                        Toast.makeText(this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        btnLogin.setEnabled(true);
                        btnLogin.setText("Login →");
                    }
                });
    }

    private void checkUserRoleAndRedirect(String userId) {
        db.collection("Users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");

                        if (role != null && role.equalsIgnoreCase("Caregiver")) {
                            startActivity(new Intent(LoginActivity.this, CaregiverDashboardActivity.class));
                            finish();
                        }
                        else if (role != null && role.equalsIgnoreCase("Elderly")) {
                            startActivity(new Intent(LoginActivity.this, ElderlyDashboardActivity.class));
                            finish();
                        }
                        else {
                            Toast.makeText(this, "Account role is undefined!", Toast.LENGTH_LONG).show();
                            mAuth.signOut();
                            btnLogin.setEnabled(true);
                            btnLogin.setText("Login →");
                        }
                    } else {
                        Toast.makeText(this, "User profile not found in database.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}