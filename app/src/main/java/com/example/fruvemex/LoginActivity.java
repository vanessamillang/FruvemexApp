package com.example.fruvemex;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";

    EditText emailEditText, passwordEditText;
    Button loginButton;
    TextView createAccountTextView;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Verificar si ya hay un usuario autenticado
        if (mAuth.getCurrentUser() != null) {
            // Obtener UID del usuario actual
            String uid = mAuth.getCurrentUser().getUid();

            // Obtener datos adicionales del usuario desde Firestore
            db.collection("users").document(uid)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()) {
                                DocumentSnapshot document = task.getResult();
                                if (document.exists()) {
                                    String username = document.getString("username");
                                    String userEmail = document.getString("email");

                                    // Abrir MainActivity y pasar datos adicionales
                                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                    intent.putExtra("username", username);
                                    intent.putExtra("email", userEmail);
                                    startActivity(intent);
                                    finish(); // Finalizar la actividad actual
                                } else {
                                    Log.d(TAG, "No such document");
                                    Toast.makeText(LoginActivity.this, "No se encontraron datos de usuario", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Log.d(TAG, "get failed with ", task.getException());
                                Toast.makeText(LoginActivity.this, "Error al obtener datos de usuario", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }

        // Inicializar vistas después
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        createAccountTextView = findViewById(R.id.createAccountTextView);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString();

                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show();
                    return;
                }

                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    // Inicio de sesión exitoso
                                    Log.d(TAG, "signInWithEmail:success");

                                    // Obtener UID del usuario actual
                                    String uid = mAuth.getCurrentUser().getUid();

                                    // Obtener datos adicionales del usuario desde Firestore
                                    db.collection("users").document(uid)
                                            .get()
                                            .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                @Override
                                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                    if (task.isSuccessful()) {
                                                        DocumentSnapshot document = task.getResult();
                                                        if (document.exists()) {
                                                            String username = document.getString("username");
                                                            String userEmail = document.getString("email");

                                                            // Abrir MainActivity y pasar datos adicionales
                                                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                                            intent.putExtra("username", username);
                                                            intent.putExtra("email", userEmail);
                                                            startActivity(intent);
                                                            finish(); // Finalizar la actividad actual
                                                        } else {
                                                            Log.d(TAG, "No such document");
                                                            Toast.makeText(LoginActivity.this, "No se encontraron datos de usuario", Toast.LENGTH_SHORT).show();
                                                        }
                                                    } else {
                                                        Log.d(TAG, "get failed with ", task.getException());
                                                        Toast.makeText(LoginActivity.this, "Error al obtener datos de usuario", Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                            });

                                } else {
                                    // Inicio de sesión fallido
                                    Log.w(TAG, "signInWithEmail:failure", task.getException());
                                    Toast.makeText(LoginActivity.this, "Autenticación fallida: " + task.getException().getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });

        createAccountTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
    }
}