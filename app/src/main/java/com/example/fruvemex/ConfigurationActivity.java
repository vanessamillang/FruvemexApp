package com.example.fruvemex;

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

public class ConfigurationActivity extends AppCompatActivity {

    private static final String TAG = "ConfigurationActivity";

    private ImageView profileImageView;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextView usernameTextView;

    private ImageButton addContentButton, removeContentButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuration);

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Referencia al ImageView para la foto de perfil
        profileImageView = findViewById(R.id.profileButton);

        // Referencias a los TextView de las opciones
        TextView editProfileTextView = findViewById(R.id.editProfileTextView);
        TextView accountSettingsTextView = findViewById(R.id.accountSettingsTextView);
        TextView aboutFruvemexTextView = findViewById(R.id.aboutFruvemexTextView);
        TextView helpSupportTextView = findViewById(R.id.helpSupportTextView);
        TextView logoutTextView = findViewById(R.id.logoutTextView);

        addContentButton = findViewById(R.id.addContentButton);
        removeContentButton = findViewById(R.id.removeContentButton);
        usernameTextView = findViewById(R.id.usernameTextView);


        // Configurar listeners para los clics en los TextView
        editProfileTextView.setOnClickListener(v -> {
            Intent intent = new Intent(ConfigurationActivity.this, EditProfileActivity.class);
            startActivity(intent);
        });

       accountSettingsTextView.setOnClickListener(v -> {
            Intent intent = new Intent(ConfigurationActivity.this, AccountSettingsActivity.class);
            startActivity(intent);
        });


       aboutFruvemexTextView.setOnClickListener(v -> {
            Intent intent = new Intent(ConfigurationActivity.this, AboutFruvemexActivity.class);
            startActivity(intent);
        });

        helpSupportTextView.setOnClickListener(v -> {

                Intent intent = new Intent(ConfigurationActivity.this, HelpAndSupportActivity.class);
                startActivity(intent);

        });

        logoutTextView.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(ConfigurationActivity.this, "Has cerrado sesión", Toast.LENGTH_SHORT).show();
            // Redirigir al usuario a la pantalla de inicio de sesión
            Intent intent = new Intent(ConfigurationActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish(); // Opcional: cerrar la actividad actual
        });

        // Cargar la foto de perfil del usuario actual
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            loadProfileImage(userId);
            loadUsername(userId);
        } else {
            Toast.makeText(this, "Error: No se pudo obtener el usuario autenticado", Toast.LENGTH_SHORT).show();
        }

        // Configurar la navegación de los botones del footer
        NavigationUtil.setupProfileButton(this, R.id.profileButton, null);
        NavigationUtil.setupLogoutButton(this, R.id.logoutButton);
        addContentButton.setOnClickListener(v -> {
            if (isNetworkAvailable()) {
                NavigationUtil.setupAddContentButton(this, R.id.addContentButton, null);
            } else {
                Toast.makeText(ConfigurationActivity.this, "No hay conexión a internet", Toast.LENGTH_SHORT).show();
            }
        });
        NavigationUtil.setupFavoritesButton(this, R.id.myLibraryButton, null);
        NavigationUtil.setupHomeButton(this, R.id.homeButton, null);
        removeContentButton.setOnClickListener(v -> {
            if (isNetworkAvailable()) {
                NavigationUtil.setupRemoveContentButton(this, R.id.removeContentButton, db, currentUser);
            } else {
                Toast.makeText(ConfigurationActivity.this, "No hay conexión a internet", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void loadUsername(String userId) {
        db.collection("users").document(userId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    String username = document.getString("username");
                    usernameTextView.setText(username);
                }
            } else {
                Log.d(TAG, "Error getting user document: ", task.getException());
                Toast.makeText(ConfigurationActivity.this, "Error al obtener el nombre de usuario",
                        Toast.LENGTH_SHORT).show();
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
                                .into(profileImageView);
                    } else {
                        // Usa drawable por defecto si no hay URL
                        profileImageView.setImageResource(R.drawable.perfil);
                    }
                } else {
                    Toast.makeText(this, "Error: El perfil no existe", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Error al cargar la imagen de perfil", Toast.LENGTH_SHORT).show();
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
