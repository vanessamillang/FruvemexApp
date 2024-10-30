package com.example.fruvemex;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class RemoveDocumentAdapter extends RecyclerView.Adapter<RemoveDocumentAdapter.RemoveDocumentViewHolder> {
    private List<Document> removeDocumentList; // Lista de documentos a eliminar
    private OnRemoveDocumentClickListener listener;

    public interface OnRemoveDocumentClickListener {
        void onRemoveDocumentClick(Document document);
    }

    public RemoveDocumentAdapter(List<Document> removeDocumentList, OnRemoveDocumentClickListener listener) {
        this.removeDocumentList = removeDocumentList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RemoveDocumentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_document, parent, false);
        return new RemoveDocumentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RemoveDocumentViewHolder holder, int position) {
        Document document = removeDocumentList.get(position);
        holder.bind(document, listener);
    }

    @Override
    public int getItemCount() {
        return removeDocumentList.size();
    }

    static class RemoveDocumentViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView; // Cambia el nombre de la variable para que sea más descriptiva
        ImageView documentImageView; // Añadido para la imagen del documento

        RemoveDocumentViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.documentTitle); // ID del título
            documentImageView = itemView.findViewById(R.id.documentImage); // ID de la imagen
        }

        void bind(final Document document, final OnRemoveDocumentClickListener listener) {
            titleTextView.setText(document.getNombre()); // Cambia a getNombre()

            // Cargar la imagen del documento usando Glide
            Glide.with(itemView.getContext())
                    .load(document.getImagen()) // Sigue usando getImagen()
                    .into(documentImageView);

            // Lógica para eliminar el documento al hacer clic
            itemView.setOnClickListener(v -> listener.onRemoveDocumentClick(document));
        }
    }
}
