package com.example.fruvemex;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.fruvemex.utils.NavigationUtil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;

public class DocumentDetailActivity extends AppCompatActivity {

    private static final String TAG = "DocumentDetailActivity";

    private TextView documentoNombreTextView;
    private TextView documentoDescripcionTextView;
    private ImageView documentoImageView;
    private String pdfUrl; // Variable para almacenar la URL del PDF
    private TextView usernameTextView, userNameTextView; // TextView para mostrar el username
    private ImageButton favoriteButton; // ImageButton para el favorito
    private boolean isFavorite = false; // Estado del botón de favorito
    private ImageButton profileButton; // Botón para mostrar el perfil
    private ImageView userProfileImageView;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private ImageButton addContentButton, removeContentButton;

    private String documentoId; // Almacenar el documentoId

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document_detail);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        documentoNombreTextView = findViewById(R.id.documentoNombreTextView);
        documentoDescripcionTextView = findViewById(R.id.documentoDescripcionTextView);
        documentoImageView = findViewById(R.id.documentoImageView);
        usernameTextView = findViewById(R.id.usernameTextView);
        favoriteButton = findViewById(R.id.favoriteButton);
        profileButton = findViewById(R.id.profileButton);
        userNameTextView = findViewById(R.id.userNameTextView);
        userProfileImageView = findViewById(R.id.userProfileImageView);
        addContentButton = findViewById(R.id.addContentButton);
        removeContentButton = findViewById(R.id.removeContentButton);

        // Obtener el documento ID desde Intent
        documentoId = getIntent().getStringExtra("documentoId");
        // Obtener el username desde Intent
        String username = getIntent().getStringExtra("username");

        // Configurar el TextView para mostrar el username
        if (username != null && !username.isEmpty()) {
            usernameTextView.setText(username);
        }

        // Cargar imagen de perfil del usuario
        loadProfileImage();

        // Consultar el documento seleccionado
        fetchDocumentoDetails(documentoId);

        // Verificar si el documento ya es favorito
        checkIfFavorite(documentoId, currentUser.getUid());

        // Configurar el botón para abrir el PDF
        Button verPdfButton = findViewById(R.id.verPdfButton);
        verPdfButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openPdfViewerActivity();
            }
        });

        // Configurar el botón para ver el perfil del usuario
        profileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DocumentDetailActivity.this, UserProfileActivity.class);
                startActivity(intent);
            }
        });

        // Configurar el botón de logout
        ImageView logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(DocumentDetailActivity.this, LoginActivity.class);
                startActivity(intent);
                finish(); // Cerrar esta actividad para evitar que el usuario vuelva atrás con el botón de retroceso
            }
        });

        // Configurar el botón de favorito
        favoriteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isFavorite) {
                    removeFavorite(documentoId, currentUser.getUid());
                } else {
                    addFavorite(documentoId, currentUser.getUid());
                }
            }
        });

        // Setup navigation buttons
        NavigationUtil.setupProfileButton(this, R.id.profileButton, null);
        NavigationUtil.setupLogoutButton(this, R.id.logoutButton);
        addContentButton.setOnClickListener(v -> {
            if (isNetworkAvailable()) {
                // Si hay conexión a internet, ir a AddContentActivity
                NavigationUtil.setupAddContentButton(this, R.id.addContentButton, null);
            } else {
                // Si no hay conexión, mostrar un mensaje
                Toast.makeText(DocumentDetailActivity.this, "No hay conexión a internet", Toast.LENGTH_SHORT).show();
            }
        });
        NavigationUtil.setupFavoritesButton(this, R.id.myLibraryButton, null);
        NavigationUtil.setupHomeButton(this, R.id.homeButton, null);
        removeContentButton.setOnClickListener(v -> {
            if (isNetworkAvailable()) {
                // Si hay conexión a internet, ir a RemoveContentActivity
                NavigationUtil.setupRemoveContentButton(this, R.id.removeContentButton, db, currentUser);
            } else {
                // Si no hay conexión, mostrar un mensaje
                Toast.makeText(DocumentDetailActivity.this, "No hay conexión a internet", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadProfileImage() {
        if (currentUser != null) {
            String userId = currentUser.getUid();
            DocumentReference userRef = db.collection("users").document(userId);
            userRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            String profileImageUrl = document.getString("imagen");
                            if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                                // Usar Glide para cargar la imagen en el profileButton
                                Glide.with(DocumentDetailActivity.this)
                                        .load(profileImageUrl)
                                        .circleCrop() // Hace la imagen redonda
                                        .into(profileButton);
                            } else {
                                // Mostrar imagen predeterminada si no existe URL de perfil
                                profileButton.setImageResource(R.drawable.perfil);
                            }
                        }
                    } else {
                        Log.d(TAG, "Error al obtener los datos del usuario", task.getException());
                    }
                }
            });
        }
    }

    private void fetchDocumentoDetails(String documentoId) {
        db.collection("documentos").document(documentoId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document != null && document.exists()) {
                                String nombre = document.getString("nombre");
                                String descripcion = document.getString("descripcion");
                                String imagenUrl = document.getString("imagen");
                                String uploaderId = document.getString("uidUser"); // Obtener el ID del uploader
                                pdfUrl = document.getString("documento"); // Suponiendo que el campo se llama "documento"

                                documentoNombreTextView.setText(nombre);
                                documentoDescripcionTextView.setText(descripcion);

                                // Cargar la imagen utilizando Picasso
                                Picasso.get().load(imagenUrl).into(documentoImageView);

                                // Cargar el nombre e imagen del usuario que subió el documento
                                loadUploaderDetails(uploaderId);
                            } else {
                                Log.d(TAG, "No se encontró el documento");
                            }
                        } else {
                            Log.d(TAG, "Error al obtener el documento", task.getException());
                        }
                    }
                });
    }

    private void loadUploaderDetails(String uploaderId) {
        DocumentReference userRef = db.collection("users").document(uploaderId);
        userRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document != null && document.exists()) {
                        String username = document.getString("username");
                        String profileImageUrl = document.getString("imagen");

                        // Configurar el TextView y ImageView con los datos del uploader
                        userNameTextView.setText(username);
                        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                            Glide.with(DocumentDetailActivity.this)
                                    .load(profileImageUrl)
                                    .circleCrop() // Hace la imagen redonda
                                    .into(userProfileImageView);
                        } else {
                            // Mostrar imagen predeterminada si no existe URL de perfil
                            userProfileImageView.setImageResource(R.drawable.perfil);
                        }
                    } else {
                        Log.d(TAG, "No se encontró el usuario");
                    }
                } else {
                    Log.d(TAG, "Error al obtener los datos del usuario", task.getException());
                }
            }
        });
    }

    private void checkIfFavorite(String documentoId, String userId) {
        db.collection("favoritos")
                .whereEqualTo("uidDoc", documentoId)
                .whereEqualTo("uidUser", userId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            isFavorite = true;
                            favoriteButton.setImageResource(R.drawable.ic_favorite_filled);
                        } else {
                            isFavorite = false;
                            favoriteButton.setImageResource(R.drawable.ic_favorite_border);
                        }
                    }
                });
    }

    private void addFavorite(String documentoId, String userId) {
        Map<String, Object> favorite = new HashMap<>();
        favorite.put("uidDoc", documentoId);
        favorite.put("uidUser", userId);

        db.collection("favoritos")
                .add(favorite)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Documento agregado a favoritos", Toast.LENGTH_SHORT).show();
                    isFavorite = true;
                    favoriteButton.setImageResource(R.drawable.ic_favorite_filled);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error al agregar a favoritos", e);
                    Toast.makeText(this, "Error al agregar a favoritos", Toast.LENGTH_SHORT).show();
                });
    }

    private void removeFavorite(String documentoId, String userId) {
        db.collection("favoritos")
                .whereEqualTo("uidDoc", documentoId)
                .whereEqualTo("uidUser", userId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            for (DocumentSnapshot document : task.getResult()) {
                                document.getReference().delete()
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(DocumentDetailActivity.this, "Documento eliminado de favoritos", Toast.LENGTH_SHORT).show();
                                            isFavorite = false;
                                            favoriteButton.setImageResource(R.drawable.ic_favorite_border);
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.w(TAG, "Error al eliminar de favoritos", e);
                                            Toast.makeText(DocumentDetailActivity.this, "Error al eliminar de favoritos", Toast.LENGTH_SHORT).show();
                                        });
                            }
                        } else {
                            Log.d(TAG, "No se encontró el favorito");
                        }
                    }
                });
    }

    private void openPdfViewerActivity() {
        if (pdfUrl != null && !pdfUrl.isEmpty()) {
            File cachedPdf = new File(getCacheDir(), "cachedDocument.pdf");

            if (cachedPdf.exists()) {
                // Si el PDF ya existe en caché, abrirlo directamente
                Intent intent = new Intent(DocumentDetailActivity.this, PdfViewerActivity.class);
                intent.putExtra("pdfPath", cachedPdf.getAbsolutePath());
                startActivity(intent);
            } else {
                // Si no existe, descargarlo y luego abrirlo
                downloadPdfAndOpen(pdfUrl);
            }
        } else {
            Toast.makeText(this, "No se pudo abrir el documento", Toast.LENGTH_SHORT).show();
        }
    }

    private void downloadPdfAndOpen(String pdfUrl) {
        // Aquí puedes usar una biblioteca como OkHttp para descargar el archivo
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(pdfUrl).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(DocumentDetailActivity.this, "Error al descargar el PDF", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    // Guardar el archivo en caché
                    File cachedPdf = new File(getCacheDir(), "cachedDocument.pdf");
                    BufferedSink sink = Okio.buffer(Okio.sink(cachedPdf));
                    sink.writeAll(response.body().source());
                    sink.close();

                    // Abrir el archivo después de guardarlo
                    runOnUiThread(() -> {
                        Intent intent = new Intent(DocumentDetailActivity.this, PdfViewerActivity.class);
                        intent.putExtra("pdfPath", cachedPdf.getAbsolutePath());
                        startActivity(intent);
                    });
                }
            }
        });
    }
    // Método para verificar si hay conexión a Internet
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        boolean isConnected = activeNetworkInfo != null && activeNetworkInfo.isConnected();

        Log.d(TAG, "Conexión a Internet disponible: " + isConnected);
        return isConnected;
    }

}
