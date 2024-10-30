package com.example.fruvemex;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {
    private static final String TAG = "RegisterActivity";

    EditText usernameEditText, emailEditText, passwordEditText, confirmPasswordEditText;
    Button registerButton;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        View createAccountTextView = findViewById(R.id.loginTextView);

        createAccountTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });

        usernameEditText = findViewById(R.id.usernameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        registerButton = findViewById(R.id.registerButton);

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String username = usernameEditText.getText().toString().trim();
                final String email = emailEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString();
                String confirmPassword = confirmPasswordEditText.getText().toString();

                if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                    Toast.makeText(RegisterActivity.this, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!password.equals(confirmPassword)) {
                    Toast.makeText(RegisterActivity.this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
                    return;
                }

                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(RegisterActivity.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    String uid = mAuth.getCurrentUser().getUid();

                                    Map<String, Object> user = new HashMap<>();
                                    user.put("uid", uid);
                                    user.put("username", username);
                                    user.put("email", email);

                                    db.collection("users").document(uid)
                                            .set(user)
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if (task.isSuccessful()) {
                                                        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                                                        intent.putExtra("username", username);
                                                        intent.putExtra("email", email);
                                                        startActivity(intent);
                                                        finish();
                                                    } else {
                                                        Log.w(TAG, "Error writing document", task.getException());
                                                        Toast.makeText(RegisterActivity.this, "Error al guardar datos adicionales",
                                                                Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                            });

                                } else {
                                    Log.w(TAG, "createUserWithEmail:failure", task.getException());
                                    Toast.makeText(RegisterActivity.this, "Registro fallido: " + task.getException().getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });
    }
}
