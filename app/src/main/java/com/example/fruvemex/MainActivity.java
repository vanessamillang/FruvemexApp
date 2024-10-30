package com.example.fruvemex;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import androidx.appcompat.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.fruvemex.utils.NavigationUtil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ImageButton logoutButton;
    private ImageButton profileButton;
    private ImageButton addContentButton;
    private ImageButton favoritesButton;
    private ImageButton removeContentButton;
    private RecyclerView categoryRecyclerView;
    private CategoryAdapter categoryAdapter;
    private List<Category> categoryList, filteredCategoryList;
    private SearchView searchView;
    private TextView usernameTextView;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private static final String TAG = "MainActivity";
    private static final String CATEGORIES_CACHE = "categories_cache";
    private static final String CACHED_CATEGORIES_KEY = "cachedCategories";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializa Firebase si aún no se ha hecho
        FirebaseApp.initializeApp(this);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Inicialización de los elementos de la UI
        logoutButton = findViewById(R.id.logoutButton);
        profileButton = findViewById(R.id.profileButton);
        addContentButton = findViewById(R.id.addContentButton);
        favoritesButton = findViewById(R.id.myLibraryButton);
        removeContentButton = findViewById(R.id.removeContentButton);
        categoryRecyclerView = findViewById(R.id.categoryRecyclerView);
        searchView = findViewById(R.id.searchView);
        usernameTextView = findViewById(R.id.usernameTextView);

        // Configurar RecyclerView y su adaptador
        categoryList = new ArrayList<>();
        filteredCategoryList = new ArrayList<>();
        categoryAdapter = new CategoryAdapter(this, filteredCategoryList);
        categoryRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        categoryRecyclerView.setAdapter(categoryAdapter);

        // Configurar clic en un elemento del RecyclerView
        categoryAdapter.setOnItemClickListener(position -> {
            Category category = filteredCategoryList.get(position);
            String categoryId = category.getUid();
            String categoryName = category.getNombre();

            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                String userId = currentUser.getUid();
                String username = usernameTextView.getText().toString();

                Intent intent = new Intent(MainActivity.this, DocumentsActivity.class);
                intent.putExtra("categoryId", categoryId);
                intent.putExtra("categoryName", categoryName);
                intent.putExtra("userId", userId);
                intent.putExtra("username", username);
                startActivity(intent);
            }
        });

        // Configurar SearchView
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterCategories(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterCategories(newText);
                return false;
            }
        });

        // Obtener el usuario actual y configurar el botón de perfil
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            loadProfileImage(currentUser.getUid());  // Cargar la imagen de perfil
            loadUsername(currentUser.getUid());      // Cargar el nombre de usuario

            // Configurar la navegación del botón de perfil usando NavigationUtil
            NavigationUtil.setupProfileButton(this, R.id.profileButton, null);
        } else {
            // Si no hay usuario actual, volver a LoginActivity
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }

        // Configurar la navegación para los demás botones
        NavigationUtil.setupLogoutButton(this, R.id.logoutButton);
        // Configurar la navegación del botón "addContentButton"
        addContentButton.setOnClickListener(v -> {
            if (isNetworkAvailable()) {
                // Si hay conexión a internet, ir a AddContentActivity
                NavigationUtil.setupAddContentButton(this, R.id.addContentButton, null);
            } else {
                // Si no hay conexión, mostrar un mensaje
                Toast.makeText(MainActivity.this, "No hay conexión a internet", Toast.LENGTH_SHORT).show();
            }
        });
        NavigationUtil.setupFavoritesButton(this, R.id.myLibraryButton, null);
        // Configurar la navegación del botón "removeContentActivity"
        removeContentButton.setOnClickListener(v -> {
            if (isNetworkAvailable()) {
                // Si hay conexión a internet, ir a RemoveContentActivity
                NavigationUtil.setupRemoveContentButton(this, R.id.removeContentButton, db, currentUser);
            } else {
                // Si no hay conexión, mostrar un mensaje
                Toast.makeText(MainActivity.this, "No hay conexión a internet", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchCategories(); // Vuelve a cargar las categorías cuando la actividad vuelva a primer plano
    }

    private void fetchCategories() {
        if (isNetworkAvailable()) {
            // Si hay conexión a internet, obtener las categorías desde Firestore
            db.collection("categorias")
                    .orderBy("nombre", Query.Direction.ASCENDING)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                categoryList.clear();
                                filteredCategoryList.clear();
                                for (DocumentSnapshot document : task.getResult()) {
                                    String nombre = document.getString("nombre");
                                    String imagenUrl = document.getString("imagen");
                                    String uid = document.getId();

                                    Category category = new Category(uid, nombre, imagenUrl);
                                    categoryList.add(category);
                                }

                                // Guardar las categorías obtenidas en caché
                                saveCategoriesToCache(categoryList);

                                filteredCategoryList.addAll(categoryList);
                                categoryAdapter.notifyDataSetChanged();
                            } else {
                                Log.d(TAG, "Error getting documents: ", task.getException());
                                Toast.makeText(MainActivity.this, "Error al obtener categorías",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        } else {
            // Si no hay conexión, cargar las categorías desde caché
            categoryList = loadCategoriesFromCache();
            if (!categoryList.isEmpty()) {
                filteredCategoryList.clear();
                filteredCategoryList.addAll(categoryList);
                categoryAdapter.notifyDataSetChanged();

                Toast.makeText(MainActivity.this, "Cargando categorías desde caché", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "No hay datos en caché disponibles", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void filterCategories(String text) {
        filteredCategoryList.clear();
        if (text.isEmpty()) {
            filteredCategoryList.addAll(categoryList);
        } else {
            text = text.toLowerCase();
            for (Category category : categoryList) {
                if (category.getNombre().toLowerCase().contains(text)) {
                    filteredCategoryList.add(category);
                }
            }
        }
        categoryAdapter.notifyDataSetChanged();
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
                Toast.makeText(MainActivity.this, "Error al obtener el nombre de usuario",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadProfileImage(String userId) {
        db.collection("users").document(userId).get().addOnCompleteListener(task -> {
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
                                .into(profileButton);
                    }
                }
            } else {
                Log.d(TAG, "Error getting profile image: ", task.getException());
                Toast.makeText(MainActivity.this, "Error al cargar la imagen de perfil",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Método para guardar las categorías en caché
    private void saveCategoriesToCache(List<Category> categories) {
        SharedPreferences sharedPreferences = getSharedPreferences(CATEGORIES_CACHE, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Convertir la lista de categorías a formato JSON
        Gson gson = new Gson();
        String jsonCategories = gson.toJson(categories);

        // Guardar la lista en SharedPreferences
        editor.putString(CACHED_CATEGORIES_KEY, jsonCategories);
        editor.apply();

        Log.d(TAG, "Categorías guardadas en caché");
    }

    // Método para cargar las categorías desde caché
    private List<Category> loadCategoriesFromCache() {
        SharedPreferences sharedPreferences = getSharedPreferences(CATEGORIES_CACHE, MODE_PRIVATE);
        String jsonCategories = sharedPreferences.getString(CACHED_CATEGORIES_KEY, null);

        if (jsonCategories != null) {
            Gson gson = new Gson();
            Type type = new TypeToken<List<Category>>() {}.getType();
            List<Category> categories = gson.fromJson(jsonCategories, type);
            Log.d(TAG, "Categorías cargadas desde caché");
            return categories;
        }
        return new ArrayList<>(); // Devuelve una lista vacía si no hay categorías en caché
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
