package com.example.fruvemex;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.example.fruvemex.utils.NavigationUtil;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddContentActivity extends AppCompatActivity {

    private static final String TAG = "AddContentActivity";
    private List<String> categoryIds; // Lista para guardar los IDs de las categorías
    private static final int PICK_IMAGE_REQUEST_CATEGORY = 1;
    private static final int PICK_IMAGE_REQUEST_DOCUMENT = 2;
    private static final int PICK_PDF_REQUEST = 3;

    private EditText etCategoryName;
    private EditText etDocumentName;
    private EditText etDocumentDescription;
    private ImageView ivCategoryImage;
    private ImageView ivDocumentImage;
    private ImageButton profileButton;
    private Button btnSelectCategoryImage;
    private Button btnSelectDocumentImage;
    private Button btnSelectDocumentPdf;
    private Button btnSaveCategory;
    private Button btnSaveDocument;
    private TextView usernameTextView;

    private Uri categoryImageUri;
    private Uri documentImageUri;
    private Uri documentPdfUri;

    private FirebaseFirestore db;
    private StorageReference storageRef;
    private FirebaseUser currentUser;

    private Spinner spinnerCategory;
    private FirebaseAuth mAuth;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_content);

        etCategoryName = findViewById(R.id.etCategoryName);
        etDocumentName = findViewById(R.id.etDocumentName);
        etDocumentDescription = findViewById(R.id.etDocumentDescription);
        ivCategoryImage = findViewById(R.id.ivCategoryImage);
        ivDocumentImage = findViewById(R.id.ivDocumentImage);
        btnSelectCategoryImage = findViewById(R.id.btnSelectCategoryImage);
        btnSelectDocumentImage = findViewById(R.id.btnSelectDocumentImage);
        btnSelectDocumentPdf = findViewById(R.id.btnSelectDocumentPDF);
        btnSaveCategory = findViewById(R.id.btnSaveCategory);
        btnSaveDocument = findViewById(R.id.btnSaveDocument);
        usernameTextView = findViewById(R.id.usernameTextView);
        profileButton = findViewById(R.id.profileButton); // Inicializar el botón de perfil


        spinnerCategory = findViewById(R.id.spinnerCategory);

        categoryIds = new ArrayList<>();

        FirebaseAuth auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        db = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        db.setFirestoreSettings(settings);

        storageRef = FirebaseStorage.getInstance().getReference();

        btnSelectCategoryImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileChooser(PICK_IMAGE_REQUEST_CATEGORY);
            }
        });

        btnSelectDocumentImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileChooser(PICK_IMAGE_REQUEST_DOCUMENT);
            }
        });

        btnSelectDocumentPdf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileChooser(PICK_PDF_REQUEST);
            }
        });

        btnSaveCategory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveCategory();
            }
        });

        btnSaveDocument.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveDocument();
            }
        });

        RadioGroup radioGroup = findViewById(R.id.radioGroup);
        final CardView categoryCardView = findViewById(R.id.categoryCardView);
        final CardView documentCardView = findViewById(R.id.documentCardView);

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.rbAddCategory) {
                    categoryCardView.setVisibility(View.VISIBLE);
                    documentCardView.setVisibility(View.GONE);
                    spinnerCategory.setVisibility(View.GONE); // Ocultar el Spinner cuando se agrega una categoría
                } else if (checkedId == R.id.rbAddDocument) {
                    categoryCardView.setVisibility(View.GONE);
                    documentCardView.setVisibility(View.VISIBLE);
                    spinnerCategory.setVisibility(View.VISIBLE); // Mostrar el Spinner cuando se agrega un documento
                    loadCategoriesIntoSpinner(); // Cargar las categorías en el Spinner
                }
            }
        });

        // Inicialización
        db = FirebaseFirestore.getInstance();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            loadUsername(currentUser.getUid());
            loadProfileImage(currentUser.getUid());
        } else {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show();
            finish(); // Cerrar actividad si el usuario no está autenticado
            return;
        }


        // Setup navigation buttons
        NavigationUtil.setupProfileButton(this, R.id.profileButton, null);
        NavigationUtil.setupLogoutButton(this, R.id.logoutButton);
        NavigationUtil.setupAddContentButton(this, R.id.addContentButton, null);
        NavigationUtil.setupFavoritesButton(this, R.id.myLibraryButton, null);
        NavigationUtil.setupHomeButton(this, R.id.homeButton, null);
        NavigationUtil.setupRemoveContentButton(this, R.id.removeContentButton, db, currentUser);
    }

    private void openFileChooser(int requestCode) {
        Intent intent = new Intent();
        intent.setType(requestCode == PICK_IMAGE_REQUEST_CATEGORY ? "image/*" : requestCode == PICK_IMAGE_REQUEST_DOCUMENT ? "image/*" : "application/pdf");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            if (requestCode == PICK_IMAGE_REQUEST_CATEGORY) {
                categoryImageUri = data.getData();
                ivCategoryImage.setImageURI(categoryImageUri);
                ivCategoryImage.setVisibility(View.VISIBLE);
            } else if (requestCode == PICK_IMAGE_REQUEST_DOCUMENT) {
                documentImageUri = data.getData();
                ivDocumentImage.setImageURI(documentImageUri);
                ivDocumentImage.setVisibility(View.VISIBLE);
            } else if (requestCode == PICK_PDF_REQUEST) {
                documentPdfUri = data.getData();
                Toast.makeText(this, "PDF selected: " + documentPdfUri.toString(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveCategory() {
        final String categoryName = etCategoryName.getText().toString().trim();

        if (categoryName.isEmpty() || categoryImageUri == null) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Upload image to Firebase Storage
        final StorageReference fileReference = storageRef.child("categories/" + System.currentTimeMillis() + ".jpg");

        fileReference.putFile(categoryImageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                fileReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        String downloadUrl = uri.toString();
                        String documentId = db.collection("categorias").document().getId();

                        // Save category to Firestore with the generated ID
                        Map<String, Object> category = new HashMap<>();
                        category.put("nombre", categoryName);
                        category.put("imagen", downloadUrl);
                        category.put("uid", documentId); // Usar el ID generado como UID
                        category.put("userUid", currentUser.getUid()); // Identificador del usuario

                        db.collection("categorias")
                                .document(documentId)
                                .set(category)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Toast.makeText(AddContentActivity.this, "Category added successfully", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(AddContentActivity.this, "Error adding category", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(AddContentActivity.this, "Error uploading image", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveDocument() {
        final String documentName = etDocumentName.getText().toString().trim();
        final String documentDescription = etDocumentDescription.getText().toString().trim();
        int selectedPosition = spinnerCategory.getSelectedItemPosition(); // Obtener la posición seleccionada

        // Obtener el ID de la categoría correspondiente
        final String selectedCategoryId = categoryIds.get(selectedPosition);

        if (documentName.isEmpty() || documentDescription.isEmpty() || documentImageUri == null || documentPdfUri == null) {
            Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        // Crear referencia para la imagen en Firebase Storage
        final StorageReference imageRef = storageRef.child("documents/images/" + System.currentTimeMillis() + ".jpg");

        // Subir la imagen
        imageRef.putFile(documentImageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // Obtener la URL de la imagen
                imageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri imageDownloadUri) {
                        final String imageDownloadUrl = imageDownloadUri.toString();

                        // Crear referencia para el PDF en Firebase Storage
                        final StorageReference pdfRef = storageRef.child("documents/pdfs/" + System.currentTimeMillis() + ".pdf");

                        // Subir el PDF
                        pdfRef.putFile(documentPdfUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                // Obtener la URL del PDF
                                pdfRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri pdfDownloadUri) {
                                        String pdfDownloadUrl = pdfDownloadUri.toString();
                                        String documentId = db.collection("documentos").document().getId();

                                        // Guardar el documento en Firestore
                                        Map<String, Object> document = new HashMap<>();
                                        document.put("nombre", documentName);
                                        document.put("descripcion", documentDescription);
                                        document.put("imagen", imageDownloadUrl);
                                        document.put("documento", pdfDownloadUrl);
                                        document.put("categoriaRef", selectedCategoryId); // Guardar el ID de la categoría
                                        document.put("uid", documentId); // Usar el ID generado como UID
                                        document.put("uidUser", currentUser.getUid()); // Identificador del usuario
                                        document.put("addedAt", Timestamp.now()); // Guardar fecha y hora actuales

                                        db.collection("documentos")
                                                .document(documentId)
                                                .set(document)
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        Toast.makeText(AddContentActivity.this, "Documento añadido exitosamente", Toast.LENGTH_SHORT).show();
                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        Toast.makeText(AddContentActivity.this, "Error al añadir documento", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(AddContentActivity.this, "Error al subir PDF", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(AddContentActivity.this, "Error al subir imagen", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
            }
        });
    }

    private void loadCategoriesIntoSpinner() {
        db.collection("categorias")
                .whereEqualTo("userUid", currentUser.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> categories = new ArrayList<>();
                    categoryIds.clear(); // Limpiar la lista anterior

                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        String categoryName = document.getString("nombre");
                        String categoryId = document.getId(); // Obtener el ID de la categoría

                        categories.add(categoryName);
                        categoryIds.add(categoryId); // Guardar el ID
                    }

                    // Crear un ArrayAdapter y asignarlo al Spinner
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerCategory.setAdapter(adapter);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AddContentActivity.this, "Error al cargar categorías", Toast.LENGTH_SHORT).show();
                });
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
                            Toast.makeText(AddContentActivity.this, "No such document", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(AddContentActivity.this, "Error getting username", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadProfileImage(String userId) {
        db.collection("users").document(userId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String profileImageUrl = document.getString("imagen");
                            // Cargar la imagen de perfil con Glide
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
}
