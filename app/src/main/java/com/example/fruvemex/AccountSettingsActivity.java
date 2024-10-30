package com.example.fruvemex;

import static android.content.ContentValues.TAG;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.example.fruvemex.utils.NavigationUtil;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class AccountSettingsActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ImageView profileImageView;
    private EditText emailEditText, phoneEditText, currentPasswordEditText, newPasswordEditText, confirmNewPasswordEditText;
    private Button saveEmailPhoneButton, savePasswordButton;
    private TextView usernameTextView;

    private ImageButton addContentButton, removeContentButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_settings);

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Referencias a los elementos de la interfaz
        profileImageView = findViewById(R.id.profileButton);
        emailEditText = findViewById(R.id.editEmail);
        phoneEditText = findViewById(R.id.editPhoneNumber);
        currentPasswordEditText = findViewById(R.id.currentPassword);
        newPasswordEditText = findViewById(R.id.newPassword);
        confirmNewPasswordEditText = findViewById(R.id.confirmNewPassword);
        saveEmailPhoneButton = findViewById(R.id.saveEmailPhoneButton);
        savePasswordButton = findViewById(R.id.savePasswordButton);

        addContentButton = findViewById(R.id.addContentButton);
        removeContentButton = findViewById(R.id.removeContentButton);
        usernameTextView = findViewById(R.id.usernameTextView);

        // Cargar la información del usuario
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            checkConnectionAndLoadData(userId);
            loadProfileImage(userId);
            loadUsername(userId);
        } else {
            Toast.makeText(this, "Error: No se pudo obtener el usuario autenticado", Toast.LENGTH_SHORT).show();
        }

        // Configurar el listener para guardar los cambios de email y teléfono
        saveEmailPhoneButton.setOnClickListener(v -> updateEmailPhone());

        // Configurar el listener para cambiar la contraseña
        savePasswordButton.setOnClickListener(v -> updatePassword());

        // Configurar la navegación de los botones del footer
        NavigationUtil.setupProfileButton(this, R.id.profileButton, null);
        NavigationUtil.setupLogoutButton(this, R.id.logoutButton);
        addContentButton.setOnClickListener(v -> {
            if (isNetworkAvailable()) {
                NavigationUtil.setupAddContentButton(this, R.id.addContentButton, null);
            } else {
                Toast.makeText(AccountSettingsActivity.this, "No hay conexión a internet", Toast.LENGTH_SHORT).show();
            }
        });
        NavigationUtil.setupFavoritesButton(this, R.id.myLibraryButton, null);
        NavigationUtil.setupHomeButton(this, R.id.homeButton, null);
        removeContentButton.setOnClickListener(v -> {
            if (isNetworkAvailable()) {
                NavigationUtil.setupRemoveContentButton(this, R.id.removeContentButton, db, currentUser);
            } else {
                Toast.makeText(AccountSettingsActivity.this, "No hay conexión a internet", Toast.LENGTH_SHORT).show();
            }
        });

        // Verificar el rol del usuario y ajustar la visibilidad de los botones
        if (currentUser != null) {
            db.collection("users").document(currentUser.getUid()).get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        String position = document.getString("position");
                    } else {
                        Toast.makeText(this, "Error: El perfil no existe", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Error al obtener el rol del usuario", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void checkConnectionAndLoadData(String userId) {
        if (isNetworkAvailable()) {
            loadUserData(userId);
            // Si hay conexión, habilitar los EditText
            emailEditText.setEnabled(true);
            phoneEditText.setEnabled(true);
        } else {
            // Si no hay conexión, deshabilitar los EditText
            emailEditText.setEnabled(false);
            phoneEditText.setEnabled(false);
            // Cargar los datos del caché
            loadCachedUserData(userId);
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
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
                    }
                } else {
                    Toast.makeText(this, "Error: El perfil no existe", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Error al cargar la imagen de perfil", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(AccountSettingsActivity.this, "Error al obtener el nombre de usuario",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadUserData(String userId) {
        db.collection("users").document(userId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    String email = document.getString("email");
                    String phone = document.getString("phone");
                    if (email != null) emailEditText.setText(email);
                    if (phone != null) phoneEditText.setText(phone);

                    // Guardar en SharedPreferences
                    saveUserDataToPreferences(userId, email, phone);
                } else {
                    Toast.makeText(this, "Error: El perfil no existe", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Error al cargar los datos del usuario", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadCachedUserData(String userId) {
        // Aquí cargarías los datos del caché (localmente) si es necesario
        SharedPreferences sharedPreferences = getSharedPreferences("user_data", Context.MODE_PRIVATE);
        String email = sharedPreferences.getString("email_" + userId, null);
        String phone = sharedPreferences.getString("phone_" + userId, null);

        if (email != null) emailEditText.setText(email);
        if (phone != null) phoneEditText.setText(phone);
    }

    private void saveUserDataToPreferences(String userId, String email, String phone) {
        SharedPreferences sharedPreferences = getSharedPreferences("user_data", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("email_" + userId, email);
        editor.putString("phone_" + userId, phone);
        editor.apply();
    }

    private void updateEmailPhone() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            String newEmail = emailEditText.getText().toString();
            String newPhone = phoneEditText.getText().toString();

            // Actualizar el email y el teléfono en Firestore
            db.collection("users").document(userId)
                    .update("email", newEmail, "phone", newPhone)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(AccountSettingsActivity.this, "Email y teléfono actualizados",
                                Toast.LENGTH_SHORT).show();
                        saveUserDataToPreferences(userId, newEmail, newPhone); // Guardar en caché
                    })
                    .addOnFailureListener(e -> Toast.makeText(AccountSettingsActivity.this, "Error al actualizar los datos",
                            Toast.LENGTH_SHORT).show());
        }
    }

    private void updatePassword() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String currentPassword = currentPasswordEditText.getText().toString();
            String newPassword = newPasswordEditText.getText().toString();
            String confirmNewPassword = confirmNewPasswordEditText.getText().toString();

            if (newPassword.equals(confirmNewPassword)) {
                AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), currentPassword);
                currentUser.reauthenticate(credential).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        currentUser.updatePassword(newPassword).addOnCompleteListener(updateTask -> {
                            if (updateTask.isSuccessful()) {
                                Toast.makeText(AccountSettingsActivity.this, "Contraseña actualizada con éxito",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(AccountSettingsActivity.this, "Error al actualizar la contraseña",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        Toast.makeText(AccountSettingsActivity.this, "Error al reautenticar, verifique su contraseña actual",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(AccountSettingsActivity.this, "Las nuevas contraseñas no coinciden",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
}
