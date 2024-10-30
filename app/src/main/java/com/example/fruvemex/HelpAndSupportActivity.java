package com.example.fruvemex;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
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
import com.google.firebase.firestore.FirebaseFirestore;

public class HelpAndSupportActivity extends AppCompatActivity {

    private ImageView profileImageView; // Cambiado a ImageView para cargar imágenes
    private TextView profileNameTextView;
    private ImageButton addContentButton, removeContentButton;
    private FirebaseFirestore db; // Asegúrate de que tienes Firestore inicializado
    private FirebaseAuth auth; // Para manejar la autenticación
    private FirebaseUser currentUser; // Definir currentUser

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_and_support);

        // Inicializa las vistas
        profileImageView = findViewById(R.id.profileButton);
        profileNameTextView = findViewById(R.id.usernameTextView);
        addContentButton = findViewById(R.id.addContentButton);
        removeContentButton = findViewById(R.id.removeContentButton);

        // Inicializar Firestore y Auth
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser(); // Obtener el usuario actual

        // Cargar el nombre y la imagen del perfil
        loadUserProfile();

        // Configura los botones de navegación
        setupFooterNavigation();
    }

    private void loadUserProfile() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserProfile", MODE_PRIVATE);

        // Cargar el nombre del usuario
        String userName = sharedPreferences.getString("userName", "Nombre de Usuario");
        profileNameTextView.setText(userName);

        // Cargar la imagen del perfil (asumiendo que la imagen se guarda como URL)
        String userProfileImageUrl = sharedPreferences.getString("userProfileImageUrl", null);
        if (userProfileImageUrl != null) {
            Glide.with(this)
                    .load(userProfileImageUrl)
                    .apply(new RequestOptions().transform(new CircleCrop()))
                    .into(profileImageView);
        } else {
            // Usa drawable por defecto si no hay URL
            profileImageView.setImageResource(R.drawable.perfil);
        }
    }

    private void setupFooterNavigation() {
        // Configurar la navegación de los botones del footer
        NavigationUtil.setupProfileButton(this, R.id.profileButton, null);
        NavigationUtil.setupLogoutButton(this, R.id.logoutButton);

        // Configurar el botón para agregar contenido
        addContentButton.setOnClickListener(v -> {
            if (isNetworkAvailable()) {
                NavigationUtil.setupAddContentButton(this, R.id.addContentButton, null);
            } else {
                Toast.makeText(HelpAndSupportActivity.this, "No hay conexión a internet", Toast.LENGTH_SHORT).show();
            }
        });

        NavigationUtil.setupFavoritesButton(this, R.id.myLibraryButton, null);
        NavigationUtil.setupHomeButton(this, R.id.homeButton, null);
        removeContentButton.setOnClickListener(v -> {
            if (isNetworkAvailable()) {
                NavigationUtil.setupRemoveContentButton(this, R.id.removeContentButton, db, currentUser); // Usar currentUser
            } else {
                Toast.makeText(HelpAndSupportActivity.this, "No hay conexión a internet", Toast.LENGTH_SHORT).show();
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
