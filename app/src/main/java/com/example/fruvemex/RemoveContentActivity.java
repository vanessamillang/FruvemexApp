package com.example.fruvemex;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SearchView; // Asegúrate de importar esta clase

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.fruvemex.utils.NavigationUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class RemoveContentActivity extends AppCompatActivity implements RemoveDocumentAdapter.OnRemoveDocumentClickListener {

    private RecyclerView recyclerView;
    private RemoveDocumentAdapter adapter;
    private List<Document> documentList;
    private List<Document> filteredList; // Lista filtrada para manejar el SearchView
    private FirebaseFirestore firestore;
    private String userId; // Para almacenar el ID del usuario
    private TextView usernameTextView; // Para mostrar el nombre de usuario
    private ImageButton profileImageButton; // Para mostrar la imagen de perfil

    private FirebaseAuth mAuth;
    private SearchView searchView; // Añadir la referencia del SearchView

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remove_content);

        // Obtener el userId pasado desde la actividad anterior
        userId = getIntent().getStringExtra("userId");

        // Inicializar FirebaseAuth y Firestore
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Vincular vistas
        usernameTextView = findViewById(R.id.usernameTextView);
        profileImageButton = findViewById(R.id.profileButton);

        // Inicializar RecyclerView
        recyclerView = findViewById(R.id.documentRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Inicializar la lista de documentos
        documentList = new ArrayList<>();
        filteredList = new ArrayList<>(); // Inicializar la lista filtrada
        adapter = new RemoveDocumentAdapter(filteredList, this); // Usar la lista filtrada en el adaptador
        recyclerView.setAdapter(adapter);

        // Cargar documentos desde Firestore
        loadDocuments();

        // Cargar la imagen de perfil y el nombre de usuario
        loadUserProfile();

        // Configurar la barra de navegación
        setupNavigation();

        // Vincular y configurar el SearchView
        searchView = findViewById(R.id.searchView);
        setupSearchView();
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            loadUsername(currentUser.getUid());
            loadProfileImage(currentUser.getUid());
        } else {
            Intent intent = new Intent(RemoveContentActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    private void loadUsername(String userId) {
        firestore.collection("users").document(userId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    String username = document.getString("username");
                    usernameTextView.setText(username);
                }
            } else {
                Toast.makeText(RemoveContentActivity.this, "Error al obtener el nombre de usuario", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadProfileImage(String userId) {
        firestore.collection("users").document(userId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    String imageUrl = document.getString("imagen");
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        Glide.with(this)
                                .load(imageUrl)
                                .placeholder(R.drawable.perfil)
                                .error(R.drawable.perfil)
                                .circleCrop()
                                .into(profileImageButton);
                    }
                }
            } else {
                Toast.makeText(RemoveContentActivity.this, "Error al cargar la imagen de perfil", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadDocuments() {
        firestore.collection("documentos")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Toast.makeText(RemoveContentActivity.this, "Error al cargar documentos", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (value != null) {
                            documentList.clear();
                            for (DocumentChange dc : value.getDocumentChanges()) {
                                Document document = dc.getDocument().toObject(Document.class);
                                switch (dc.getType()) {
                                    case ADDED:
                                        documentList.add(document);
                                        break;
                                    case MODIFIED:
                                        for (int i = 0; i < documentList.size(); i++) {
                                            if (documentList.get(i).getUid().equals(document.getUid())) {
                                                documentList.set(i, document);
                                                break;
                                            }
                                        }
                                        break;
                                    case REMOVED:
                                        documentList.removeIf(doc -> doc.getUid().equals(document.getUid()));
                                        break;
                                }
                            }
                            filteredList.clear(); // Limpiar la lista filtrada
                            filteredList.addAll(documentList); // Añadir todos los documentos inicialmente
                            adapter.notifyDataSetChanged(); // Actualizar el adaptador
                        }
                    }
                });
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false; // No necesitamos manejar el envío explícito
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterDocuments(newText); // Llamar a la función de filtrado cuando cambie el texto
                return true;
            }
        });
    }

    private void filterDocuments(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(documentList); // Mostrar todos los documentos si no hay query
        } else {
            for (Document document : documentList) {
                if (document.getNombre().toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(document); // Agregar documentos que coincidan con la búsqueda
                }
            }
        }
        adapter.notifyDataSetChanged(); // Actualizar el adaptador con la lista filtrada
    }

    @Override
    public void onRemoveDocumentClick(Document document) {
        new AlertDialog.Builder(this)
                .setTitle("Confirmar eliminación")
                .setMessage("¿Estás seguro de que deseas eliminar este documento?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    firestore.collection("documentos").document(document.getUid()).delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(RemoveContentActivity.this, "Documento eliminado", Toast.LENGTH_SHORT).show();
                                documentList.remove(document);
                                filteredList.remove(document);
                                adapter.notifyDataSetChanged();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(RemoveContentActivity.this, "Error al eliminar documento", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void setupNavigation() {
        // Setup navigation buttons
        NavigationUtil.setupProfileButton(this, R.id.profileButton, null);
        NavigationUtil.setupLogoutButton(this, R.id.logoutButton);
        NavigationUtil.setupAddContentButton(this, R.id.addContentButton, null);
        NavigationUtil.setupFavoritesButton(this, R.id.myLibraryButton, null);
        NavigationUtil.setupHomeButton(this, R.id.homeButton, null);
    }
}
