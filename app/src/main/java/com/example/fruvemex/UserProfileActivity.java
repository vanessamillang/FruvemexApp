package com.example.fruvemex;

import android.content.Context;
import android.content.Intent;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.example.fruvemex.utils.NavigationUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserProfileActivity extends AppCompatActivity {
    private EditText nameEditText, emailEditText, phoneEditText, roleEditText;
    private ImageView profileImageView;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String userId;
    private FirebaseUser currentUser;  // Declarar currentUser como variable de instancia

    private ImageButton addContentButton, removeContentButton, settingsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        // Inicializar Firebase y vistas
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        roleEditText = findViewById(R.id.roleEditText);
        profileImageView = findViewById(R.id.profileImageView);

        addContentButton = findViewById(R.id.addContentButton);
        removeContentButton = findViewById(R.id.removeContentButton);
        settingsButton = findViewById(R.id.settingsButton);

        // Configurar botones de navegación
        NavigationUtil.setupLogoutButton(this, R.id.logoutButton);
        addContentButton.setOnClickListener(v -> {
            if (isNetworkAvailable()) {
                NavigationUtil.setupAddContentButton(this, R.id.addContentButton, null);
            } else {
                Toast.makeText(UserProfileActivity.this, "No hay conexión a internet", Toast.LENGTH_SHORT).show();
            }
        });
        NavigationUtil.setupFavoritesButton(this, R.id.myLibraryButton, null);
        NavigationUtil.setupHomeButton(this, R.id.homeButton, null);
        removeContentButton.setOnClickListener(v -> {
            if (isNetworkAvailable()) {
                // Aquí ahora currentUser es accesible porque está declarado como variable de instancia
                NavigationUtil.setupRemoveContentButton(this, R.id.removeContentButton, db, currentUser);
            } else {
                Toast.makeText(UserProfileActivity.this, "No hay conexión a internet", Toast.LENGTH_SHORT).show();
            }
        });

        settingsButton.setOnClickListener(v -> {
            Intent settingsIntent = new Intent(UserProfileActivity.this, ConfigurationActivity.class);
            startActivity(settingsIntent);
        });

        // Deshabilitar campos de texto
        nameEditText.setEnabled(false);
        emailEditText.setEnabled(false);
        phoneEditText.setEnabled(false);
        roleEditText.setEnabled(false);

        // Obtener el usuario actual
        currentUser = mAuth.getCurrentUser();  // Inicializar currentUser aquí

        if (currentUser != null) {
            userId = currentUser.getUid();
            loadUserProfile(userId);
        } else {
            Toast.makeText(UserProfileActivity.this, "Error: No se pudo obtener el usuario autenticado", Toast.LENGTH_SHORT).show();
            // Opcionalmente podrías redirigir al usuario a la pantalla de inicio de sesión
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void loadUserProfile(String userId) {
        db.collection("users").document(userId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    String userName = document.getString("username");
                    String userEmail = document.getString("email");
                    String userPhone = document.getString("phone");
                    String userRole = document.getString("position");
                    String profileImageUrl = document.getString("imagen");

                    nameEditText.setText(userName);
                    emailEditText.setText(userEmail);
                    phoneEditText.setText(userPhone);
                    roleEditText.setText(userRole);

                    if (profileImageUrl != null) {
                        Glide.with(this)
                                .load(profileImageUrl)
                                .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                                .into(profileImageView);
                    }
                } else {
                    Toast.makeText(UserProfileActivity.this, "No se encontró el perfil del usuario", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(UserProfileActivity.this, "Error al cargar perfil", Toast.LENGTH_SHORT).show();
            }
        });
    }
}



