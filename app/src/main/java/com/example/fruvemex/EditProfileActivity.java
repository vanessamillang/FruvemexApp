package com.example.fruvemex;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.example.fruvemex.utils.NavigationUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class EditProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    private EditText editName, editPosition;
    private ImageView profilePicture;
    private Button saveProfileButton;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private String userId;
    private Uri profileImageUri;
    private ImageButton addContentButton, removeContentButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // Inicializar Firebase

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        // Referencias a las vistas
        editName = findViewById(R.id.editName);
        profilePicture = findViewById(R.id.profilePicture);
        saveProfileButton = findViewById(R.id.saveProfileButton);

        addContentButton = findViewById(R.id.addContentButton);
        removeContentButton = findViewById(R.id.removeContentButton);

        // Obtener el usuario actual
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid();
            loadUserProfile(userId);

            // Comprobar la conexión antes de permitir la edición del perfil
            if (!isNetworkAvailable()) {
                disableEditing();
                Toast.makeText(this, "Sin conexión. Mostrando información en caché.", Toast.LENGTH_SHORT).show();
            } else {
                // Configurar el botón de guardar perfil
                saveProfileButton.setOnClickListener(v -> saveUserProfile());

                // Configurar el ImageView para seleccionar una nueva imagen de perfil
                profilePicture.setOnClickListener(v -> openImagePicker());
            }

            // Configurar la navegación de los botones del footer
            setupFooterNavigation();
        } else {
            Toast.makeText(this, "Error: No se pudo obtener el usuario autenticado", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadUserProfile(String userId) {
        db.collection("users").document(userId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    String userName = document.getString("username");
                    String profileImageUrl = document.getString("imagen");

                    editName.setText(userName);

                    if (profileImageUrl != null) {
                        Glide.with(this)
                                .load(profileImageUrl)
                                .apply(new RequestOptions().transform(new CircleCrop()))
                                .into(profilePicture);
                    }
                } else {
                    Toast.makeText(this, "Error: El perfil no existe", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Error al cargar el perfil", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveUserProfile() {
        String updatedName = editName.getText().toString();

        // Actualizar la información en Firestore
        db.collection("users").document(userId).update("username", updatedName)
                .addOnSuccessListener(aVoid -> {
                    if (profileImageUri != null) {
                        uploadProfileImage();
                    } else {
                        Toast.makeText(this, "Perfil actualizado con éxito", Toast.LENGTH_SHORT).show();
                        finish(); // Cierra la actividad y regresa a la pantalla anterior
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al actualizar el perfil", Toast.LENGTH_SHORT).show();
                });
    }

    private void uploadProfileImage() {
        StorageReference profileImageRef = storage.getReference().child("profile_images/" + userId + ".jpg");

        // Convertir la imagen a bytes
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), profileImageUri);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] data = baos.toByteArray();

        UploadTask uploadTask = profileImageRef.putBytes(data);
        uploadTask.addOnSuccessListener(taskSnapshot -> profileImageRef.getDownloadUrl().addOnSuccessListener(uri -> {
            // Actualizar la URL de la imagen en Firestore
            db.collection("users").document(userId).update("imagen", uri.toString())
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Perfil actualizado con éxito", Toast.LENGTH_SHORT).show();
                        finish(); // Cierra la actividad y regresa a la pantalla anterior
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error al actualizar la imagen de perfil", Toast.LENGTH_SHORT).show();
                    });
        })).addOnFailureListener(e -> {
            Toast.makeText(this, "Error al subir la imagen de perfil", Toast.LENGTH_SHORT).show();
        });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerResultLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> imagePickerResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    profileImageUri = result.getData().getData();
                    profilePicture.setImageURI(profileImageUri);
                }
            });

    private void setupFooterNavigation() {
        // Configurar la navegación de los botones del footer
        NavigationUtil.setupLogoutButton(this, R.id.logoutButton);
        addContentButton.setOnClickListener(v -> {
            if (isNetworkAvailable()) {
                NavigationUtil.setupAddContentButton(this, R.id.addContentButton, null);
            } else {
                Toast.makeText(EditProfileActivity.this, "No hay conexión a internet", Toast.LENGTH_SHORT).show();
            }
        });
        NavigationUtil.setupFavoritesButton(this, R.id.myLibraryButton, null);
        NavigationUtil.setupHomeButton(this, R.id.homeButton, null);
        removeContentButton.setOnClickListener(v -> {
            if (isNetworkAvailable()) {
                NavigationUtil.setupRemoveContentButton(this, R.id.removeContentButton, db, currentUser);
            } else {
                Toast.makeText(EditProfileActivity.this, "No hay conexión a internet", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    private void disableEditing() {
        editName.setEnabled(false);
        profilePicture.setEnabled(false);
        saveProfileButton.setEnabled(false);
    }
}
