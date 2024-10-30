package com.example.fruvemex;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.fruvemex.utils.NavigationUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

public class FavoritesActivity extends AppCompatActivity {

    private static final String TAG = "FavoritesActivity";
    private static final String PREFS_NAME = "FavoritesPrefs";
    private static final String KEY_DOCUMENT_NAMES = "documentNames";
    private static final String KEY_DOCUMENT_IDS = "documentIds";

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ArrayList<String> documentNames;
    private ArrayList<String> documentIds;
    private ArrayAdapter<String> adapter;
    private TextView usernameTextView;
    private ListView documentosListView;
    private String uid;
    private ImageButton profileButton, addContentButton, removeContentButton;
    private boolean hasViewedBefore = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        documentosListView = findViewById(R.id.documentosListView);
        usernameTextView = findViewById(R.id.usernameTextView);
        profileButton = findViewById(R.id.profileButton);
        addContentButton = findViewById(R.id.addContentButton);
        removeContentButton = findViewById(R.id.removeContentButton);

        documentNames = new ArrayList<>();
        documentIds = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, documentNames);
        documentosListView.setAdapter(adapter);

        // Cargar el estado de visualización desde SharedPreferences
        loadViewStatus();

        if (user != null) {
            uid = user.getUid();

            // Cargar el nombre de usuario
            loadUsername(uid);

            // Cargar la imagen de perfil
            loadProfileImage(uid);

            // Cargar documentos favoritos
            loadFavoriteDocuments(uid);
        } else {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show();
        }

        // Setup navigation buttons
        NavigationUtil.setupProfileButton(this, R.id.profileButton, null);
        NavigationUtil.setupLogoutButton(this, R.id.logoutButton);
        setupAddContentButton();
        NavigationUtil.setupFavoritesButton(this, R.id.myLibraryButton, null);
        NavigationUtil.setupHomeButton(this, R.id.homeButton, null);
        setupRemoveContentButton();

        // Configurar el clic en un elemento de la lista para ver los detalles del documento
        documentosListView.setOnItemClickListener((parent, view, position, id) -> {
            String documentId = documentIds.get(position);
            Intent intent = new Intent(FavoritesActivity.this, DocumentDetailActivity.class);
            intent.putExtra("documentoId", documentId);
            intent.putExtra("username", usernameTextView.getText().toString());
            intent.putExtra("uid", uid);
            startActivity(intent);
        });
    }

    private void loadProfileImage(String userId) {
        db.collection("users").document(userId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String profileImageUrl = document.getString("imagen");
                            Glide.with(this)
                                    .load(profileImageUrl != null ? profileImageUrl : R.drawable.perfil)
                                    .circleCrop()
                                    .into(profileButton);
                        } else {
                            Log.d(TAG, "No such document");
                        }
                    } else {
                        Log.d(TAG, "get failed with ", task.getException());
                    }
                });
    }

    private void loadFavoriteDocuments(String uid) {
        if (!isNetworkAvailable() && !hasViewedBefore) {
            Toast.makeText(this, "No se puede cargar la información. No se ha visualizado antes.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("favoritos")
                .whereEqualTo("uidUser", uid)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        documentNames.clear();
                        documentIds.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String uidDoc = document.getString("uidDoc");
                            if (uidDoc != null) {
                                // Obtener el nombre del documento y agregarlo a la lista
                                db.collection("documentos").document(uidDoc).get()
                                        .addOnCompleteListener(documentTask -> {
                                            if (documentTask.isSuccessful()) {
                                                DocumentSnapshot docSnapshot = documentTask.getResult();
                                                if (docSnapshot.exists()) {
                                                    String documentName = docSnapshot.getString("nombre");
                                                    documentNames.add(documentName);
                                                    documentIds.add(uidDoc);
                                                    adapter.notifyDataSetChanged();
                                                } else {
                                                    Log.d(TAG, "No such document");
                                                }
                                            } else {
                                                Log.d(TAG, "get failed with ", documentTask.getException());
                                            }
                                        });
                            } else {
                                Log.d(TAG, "No uidDoc found");
                            }
                        }
                        // Guardar documentos en caché después de cargarlos
                        saveDocumentsToCache();
                        hasViewedBefore = true; // Actualiza el estado si se cargaron documentos
                    } else {
                        Log.d(TAG, "Error getting documents: ", task.getException());
                        // Cargar documentos desde caché si no hay conexión
                        if (!isNetworkAvailable() && hasViewedBefore) {
                            loadDocumentsFromCache();
                        }
                    }
                });
    }

    private void saveDocumentsToCache() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_DOCUMENT_NAMES, new JSONArray(documentNames).toString());
        editor.putString(KEY_DOCUMENT_IDS, new JSONArray(documentIds).toString());
        editor.apply();
    }

    private void loadDocumentsFromCache() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String namesJson = preferences.getString(KEY_DOCUMENT_NAMES, null);
        String idsJson = preferences.getString(KEY_DOCUMENT_IDS, null);

        if (namesJson != null && idsJson != null) {
            try {
                JSONArray namesArray = new JSONArray(namesJson);
                JSONArray idsArray = new JSONArray(idsJson);
                documentNames.clear();
                documentIds.clear();

                for (int i = 0; i < namesArray.length(); i++) {
                    documentNames.add(namesArray.getString(i));
                    documentIds.add(idsArray.getString(i));
                }
                adapter.notifyDataSetChanged(); // Actualizar el adaptador
            } catch (JSONException e) {
                Log.e(TAG, "Error loading documents from cache", e);
            }
        } else {
            Toast.makeText(this, "No hay documentos en caché", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadUsername(String userId) {
        db.collection("users").document(userId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String username = document.getString("username");
                            usernameTextView.setText(username);
                        } else {
                            Log.d(TAG, "No such document");
                        }
                    } else {
                        Log.d(TAG, "get failed with ", task.getException());
                    }
                });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        boolean isConnected = activeNetworkInfo != null && activeNetworkInfo.isConnected();

        Log.d(TAG, "Conexión a Internet disponible: " + isConnected);
        return isConnected;
    }

    private void loadViewStatus() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String namesJson = preferences.getString(KEY_DOCUMENT_NAMES, null);
        if (namesJson != null) {
            hasViewedBefore = true; // Si hay datos en caché, se considera que se ha visualizado antes
        }
    }

    private void setupAddContentButton() {
        addContentButton.setOnClickListener(v -> {
            if (isNetworkAvailable()) {
                NavigationUtil.setupAddContentButton(this, R.id.addContentButton, null);
            } else {
                Toast.makeText(FavoritesActivity.this, "No hay conexión a internet", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupRemoveContentButton() {
        removeContentButton.setOnClickListener(v -> {
            if (isNetworkAvailable()) {
                NavigationUtil.setupRemoveContentButton(this, R.id.removeContentButton, db, mAuth.getCurrentUser());
            } else {
                Toast.makeText(FavoritesActivity.this, "No hay conexión a internet", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
