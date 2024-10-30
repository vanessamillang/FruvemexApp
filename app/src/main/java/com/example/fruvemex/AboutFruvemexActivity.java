package com.example.fruvemex;

import static android.content.ContentValues.TAG;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.example.fruvemex.utils.NavigationUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class AboutFruvemexActivity extends AppCompatActivity {

    private ImageButton profileImageButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private TextView usernameTextView;

    private ImageButton addContentButton, removeContentButton;

    // Declara currentUser como un campo de clase
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_fruvemex);

        addContentButton = findViewById(R.id.addContentButton);
        removeContentButton = findViewById(R.id.removeContentButton);
        usernameTextView = findViewById(R.id.usernameTextView);

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        profileImageButton = findViewById(R.id.profileButton);

        // Cargar la foto de perfil del usuario actual
        currentUser = mAuth.getCurrentUser(); // Asignar el valor a currentUser
        if (currentUser != null) {
            String userId = currentUser.getUid();
            loadProfileImage(userId);
            loadUsername(userId);
        } else {
            Toast.makeText(this, "Error: No se pudo obtener el usuario autenticado", Toast.LENGTH_SHORT).show();
        }

        // Configurar la navegación de los botones del footer
        setupFooterNavigation();
    }

    private void loadUsername(String userId) {
        db.collection("users").document(userId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    String username = document.getString("username");
                    usernameTextView.setText(username);
                    saveToSharedPreferences("username", username); // Guarda en SharedPreferences
                }
            } else {
                Log.d(TAG, "Error getting user document: ", task.getException());
                Toast.makeText(AboutFruvemexActivity.this, "Error al obtener el nombre de usuario",
                        Toast.LENGTH_SHORT).show();
                loadFromSharedPreferences("username"); // Carga desde SharedPreferences si falla
            }
        });
    }

    private void loadProfileImage(String userId) {
        db.collection("users").document(userId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    String profileImageUrl = document.getString("imagen");
                    if (profileImageUrl != null) {
                        Glide.with(this)
                                .load(profileImageUrl)
                                .apply(new RequestOptions().transform(new CircleCrop()))
                                .into(profileImageButton);
                        saveToSharedPreferences("profileImageUrl", profileImageUrl); // Guarda en SharedPreferences
                    } else {
                        // Usa drawable por defecto si no hay URL
                        profileImageButton.setImageResource(R.drawable.perfil);
                    }
                } else {
                    Toast.makeText(this, "Error: El perfil no existe", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Error al cargar la imagen de perfil", Toast.LENGTH_SHORT).show();
                loadFromSharedPreferences("profileImageUrl"); // Carga desde SharedPreferences si falla
            }
        });
    }

    private void saveToSharedPreferences(String key, String value) {
        getSharedPreferences("FruvemexPrefs", Context.MODE_PRIVATE)
                .edit()
                .putString(key, value)
                .apply();
    }

    private void loadFromSharedPreferences(String key) {
        String value = getSharedPreferences("FruvemexPrefs", Context.MODE_PRIVATE)
                .getString(key, null);
        if (key.equals("username") && value != null) {
            usernameTextView.setText(value);
        } else if (key.equals("profileImageUrl") && value != null) {
            Glide.with(this)
                    .load(value)
                    .apply(new RequestOptions().transform(new CircleCrop()))
                    .into(profileImageButton);
        }
    }

    private void setupFooterNavigation() {
        // Configurar la navegación de los botones del footer
        NavigationUtil.setupProfileButton(this, R.id.profileButton, null);
        NavigationUtil.setupLogoutButton(this, R.id.logoutButton);
        addContentButton.setOnClickListener(v -> {
            if (isNetworkAvailable()) {
                NavigationUtil.setupAddContentButton(this, R.id.addContentButton, null);
            } else {
                Toast.makeText(AboutFruvemexActivity.this, "No hay conexión a internet", Toast.LENGTH_SHORT).show();
            }
        });
        NavigationUtil.setupFavoritesButton(this, R.id.myLibraryButton, null);
        NavigationUtil.setupHomeButton(this, R.id.homeButton, null);
        removeContentButton.setOnClickListener(v -> {
            if (isNetworkAvailable()) {
                NavigationUtil.setupRemoveContentButton(this, R.id.removeContentButton, db, currentUser);
            } else {
                Toast.makeText(AboutFruvemexActivity.this, "No hay conexión a internet", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }
}
