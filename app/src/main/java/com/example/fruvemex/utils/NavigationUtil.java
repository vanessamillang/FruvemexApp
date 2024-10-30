package com.example.fruvemex.utils;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.fruvemex.AddContentActivity;
import com.example.fruvemex.FavoritesActivity;
import com.example.fruvemex.LoginActivity;
import com.example.fruvemex.MainActivity;
import com.example.fruvemex.RemoveContentActivity;
import com.example.fruvemex.R;
import com.example.fruvemex.UserProfileActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class NavigationUtil {

    public static void setupProfileButton(Activity activity, int buttonId, Bundle extras) {
        ImageButton profileButton = activity.findViewById(buttonId);
        profileButton.setOnClickListener(view -> {
            Intent intent = new Intent(activity, UserProfileActivity.class);
            if (extras != null) {
                intent.putExtras(extras);
            }
            activity.startActivity(intent);
        });
    }

    public static void setupLogoutButton(Activity activity, int buttonId) {
        ImageButton logoutButton = activity.findViewById(buttonId);
        logoutButton.setOnClickListener(view -> {
            FirebaseAuth mAuth = FirebaseAuth.getInstance();
            mAuth.signOut();
            Intent intent = new Intent(activity, LoginActivity.class);
            activity.startActivity(intent);
            activity.finish();
        });
    }

    public static void setupAddContentButton(Activity activity, int buttonId, Bundle extras) {
        ImageButton addContentButton = activity.findViewById(buttonId);
        addContentButton.setOnClickListener(view -> {
            Intent intent = new Intent(activity, AddContentActivity.class);
            if (extras != null) {
                intent.putExtras(extras);
            }
            activity.startActivity(intent);
        });
    }

    public static void setupFavoritesButton(Activity activity, int buttonId, Bundle extras) {
        ImageButton favoritesButton = activity.findViewById(buttonId);
        favoritesButton.setOnClickListener(view -> {
            Intent intent = new Intent(activity, FavoritesActivity.class);
            if (extras != null) {
                intent.putExtras(extras);
            }
            activity.startActivity(intent);
        });
    }

    public static void setupHomeButton(Activity activity, int buttonId, Bundle extras) {
        ImageButton homeButton = activity.findViewById(buttonId);
        homeButton.setOnClickListener(view -> {
            Intent intent = new Intent(activity, MainActivity.class);
            if (extras != null) {
                intent.putExtras(extras);
            }
            activity.startActivity(intent);
        });
    }

    // Modificado para verificar si el usuario es administrador antes de navegar a RemoveContentActivity
    public static void setupRemoveContentButton(Activity activity, int buttonId, FirebaseFirestore db, FirebaseUser currentUser) {
        ImageButton removeContentButton = activity.findViewById(buttonId);
        String uid = currentUser.getUid();

        // Consulta Firestore para verificar si el usuario es administrador
        db.collection("users").document(uid).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    String position = documentSnapshot.getString("position");
                    if ("Administrador".equals(position)) {
                        // Si el usuario es administrador, muestra el botón
                        removeContentButton.setVisibility(View.VISIBLE);
                        removeContentButton.setOnClickListener(view -> {
                            Intent intent = new Intent(activity, RemoveContentActivity.class);
                            activity.startActivity(intent);
                        });
                    } else {
                        // Si no es administrador, oculta el botón
                        removeContentButton.setVisibility(View.GONE);
                    }
                } else {
                    // Si no se encuentra el documento, oculta el botón
                    removeContentButton.setVisibility(View.GONE);
                }
            }
        }).addOnFailureListener(e -> {
            // En caso de error al obtener la información del usuario, oculta el botón
            removeContentButton.setVisibility(View.GONE);
        });
    }
}
