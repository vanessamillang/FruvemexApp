package com.example.fruvemex;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import androidx.appcompat.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.fruvemex.utils.NavigationUtil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class DocumentsActivity extends AppCompatActivity {

    private static final String TAG = "DocumentsActivity";
    private static final String PREFS_NAME = "documents_activity_prefs";
    private static final String KEY_HAS_VIEWED_BEFORE = "has_viewed_before";

    private TextView categoryNameTextView;
    private TextView usernameTextView;
    private ListView documentosListView;
    private SearchView searchView;
    private ArrayAdapter<String> documentosAdapter;
    private List<String> documentList;
    private List<String> documentIdList;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private ImageButton addContentButton, removeContentButton;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_documents);

        // Inicializar SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Verificar si la pantalla se había visualizado antes
        boolean hasViewedBefore = sharedPreferences.getBoolean(KEY_HAS_VIEWED_BEFORE, false);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        categoryNameTextView = findViewById(R.id.categoryNameTextView);
        usernameTextView = findViewById(R.id.usernameTextView);
        documentosListView = findViewById(R.id.documentosListView);
        searchView = findViewById(R.id.searchView);

        addContentButton = findViewById(R.id.addContentButton);
        removeContentButton = findViewById(R.id.removeContentButton);

        // Inicializar la lista y el adaptador
        documentList = new ArrayList<>();
        documentIdList = new ArrayList<>();
        documentosAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, documentList);
        documentosListView.setAdapter(documentosAdapter);

        // Obtener el ID y nombre de la categoría de MainActivity
        String categoryId = getIntent().getStringExtra("categoryId");
        String categoryName = getIntent().getStringExtra("categoryName");

        // Verificar que el ID y nombre no sean nulos o vacíos
        if (categoryId == null || categoryId.isEmpty() || categoryName == null || categoryName.isEmpty()) {
            String errorMessage = "El ID o nombre de la categoría es nulo o vacío";
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
            Log.e(TAG, errorMessage);
            return;
        }

        // Mostrar el nombre de la categoría en el TextView
        categoryNameTextView.setText(categoryName);

        // Establecer el nombre de usuario
        String username = getIntent().getStringExtra("username");
        if (username != null && !username.isEmpty()) {
            usernameTextView.setText(username);
        }

        // Cargar documentos
        fetchDocumentos(categoryId, hasViewedBefore);

        // Configurar el SearchView
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterDocuments(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterDocuments(newText);
                return false;
            }
        });

        // Configurar el evento de clic en un elemento de ListView
        documentosListView.setOnItemClickListener((parent, view, position, id) -> {
            String documentoId = documentIdList.get(position);
            Intent intent = new Intent(DocumentsActivity.this, DocumentDetailActivity.class);
            intent.putExtra("documentoId", documentoId);
            intent.putExtra("username", usernameTextView.getText().toString());
            startActivity(intent);
        });

        // Cargar imagen de perfil
        ImageButton profileButton = findViewById(R.id.profileButton);
        loadProfileImage(profileButton);

        // Configurar el evento de clic del botón de cierre de sesión
        ImageButton logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(view -> {
            auth.signOut();
            Intent intent = new Intent(DocumentsActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

        // Configurar botones de navegación
        NavigationUtil.setupProfileButton(this, R.id.profileButton, null);
        NavigationUtil.setupLogoutButton(this, R.id.logoutButton);
        addContentButton.setOnClickListener(v -> {
            if (isNetworkAvailable()) {
                NavigationUtil.setupAddContentButton(this, R.id.addContentButton, null);
            } else {
                Toast.makeText(DocumentsActivity.this, "No hay conexión a internet", Toast.LENGTH_SHORT).show();
            }
        });
        NavigationUtil.setupFavoritesButton(this, R.id.myLibraryButton, null);
        NavigationUtil.setupHomeButton(this, R.id.homeButton, null);
        removeContentButton.setOnClickListener(v -> {
            if (isNetworkAvailable()) {
                NavigationUtil.setupRemoveContentButton(this, R.id.removeContentButton, db, currentUser);
            } else {
                Toast.makeText(DocumentsActivity.this, "No hay conexión a internet", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Método para cargar documentos
    private void fetchDocumentos(String categoryId, boolean hasViewedBefore) {
        if (isNetworkAvailable()) {
            // Si hay conexión a internet, obtener los documentos desde Firestore
            db.collection("documentos")
                    .whereEqualTo("categoriaRef", categoryId)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                documentList.clear();
                                documentIdList.clear();
                                for (DocumentSnapshot document : task.getResult().getDocuments()) {
                                    String documentoId = document.getId();
                                    String nombre = document.getString("nombre");
                                    Log.d(TAG, "Nombre del documento: " + nombre);
                                    documentList.add(nombre);
                                    documentIdList.add(documentoId);
                                }
                                documentosAdapter.notifyDataSetChanged();

                                // Guardar los documentos obtenidos en caché
                                saveDocumentsToCache(documentList, documentIdList);

                                // Actualizar el estado de visualización
                                sharedPreferences.edit().putBoolean(KEY_HAS_VIEWED_BEFORE, true).apply();
                            } else {
                                String errorMessage = "Error al obtener documentos: " + task.getException().getMessage();
                                Toast.makeText(DocumentsActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                                Log.e(TAG, errorMessage, task.getException());
                            }
                        }
                    });
        } else {
            // Si no hay conexión y la pantalla se había visualizado antes, cargar documentos desde caché
            if (hasViewedBefore) {
                loadDocumentsFromCacheAndDisplay();
            } else {
                Toast.makeText(DocumentsActivity.this, "No hay conexión a internet y no hay documentos en caché", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Método para cargar documentos desde caché y mostrarlos
    private void loadDocumentsFromCacheAndDisplay() {
        documentList = loadDocumentsFromCache(true);
        documentIdList = loadDocumentsFromCache(false);

        // Verificar que haya documentos en caché antes de actualizar la UI
        if (!documentList.isEmpty()) {
            documentosAdapter.clear();
            documentosAdapter.addAll(documentList);
            documentosAdapter.notifyDataSetChanged();
            Toast.makeText(DocumentsActivity.this, "Cargando documentos desde caché", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(DocumentsActivity.this, "No hay documentos en caché para mostrar", Toast.LENGTH_SHORT).show();
        }
    }

    // Método para verificar la disponibilidad de la red
    private boolean isNetworkAvailable() {
        android.net.ConnectivityManager connectivityManager = (android.net.ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        android.net.NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    // Guardar los documentos en caché
    private void saveDocumentsToCache(List<String> documentNames, List<String> documentIds) {
        SharedPreferences sharedPreferences = getSharedPreferences("documents_cache", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Convertir las listas a formato JSON
        Gson gson = new Gson();
        String jsonDocumentNames = gson.toJson(documentNames);
        String jsonDocumentIds = gson.toJson(documentIds);

        // Guardar en SharedPreferences
        editor.putString("documentNames", jsonDocumentNames);
        editor.putString("documentIds", jsonDocumentIds);
        editor.apply();
    }

    // Cargar documentos desde caché
    private List<String> loadDocumentsFromCache(boolean isNames) {
        SharedPreferences sharedPreferences = getSharedPreferences("documents_cache", MODE_PRIVATE);
        Gson gson = new Gson();
        String json = isNames ? sharedPreferences.getString("documentNames", null) : sharedPreferences.getString("documentIds", null);
        Type type = new TypeToken<List<String>>() {}.getType();
        return gson.fromJson(json, type);
    }

    // Filtrar documentos en el SearchView
    private void filterDocuments(String query) {
        List<String> filteredList = new ArrayList<>();
        for (String document : documentList) {
            if (document.toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(document);
            }
        }
        documentosAdapter.clear();
        documentosAdapter.addAll(filteredList);
        documentosAdapter.notifyDataSetChanged();
    }

    // Cargar imagen de perfil
    private void loadProfileImage(ImageButton profileButton) {
        // Cargar la imagen de perfil del usuario
        if (currentUser != null && currentUser.getPhotoUrl() != null) {
            Glide.with(this)
                    .load(currentUser.getPhotoUrl())
                    .into(profileButton);
        }
    }
}
