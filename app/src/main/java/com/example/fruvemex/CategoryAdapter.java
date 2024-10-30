package com.example.fruvemex;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private Context context;
    private List<Category> categoryList;
    private OnItemClickListener onItemClickListener;

    public CategoryAdapter(Context context, List<Category> categoryList) {
        this.context = context;
        this.categoryList = categoryList;
    }

    public void setCategoryList(List<Category> categoryList) {
        this.categoryList = categoryList;
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = categoryList.get(position);
        holder.categoryNameTextView.setText(category.getNombre());

        // Cargar la imagen utilizando Picasso
        Picasso.get().load(category.getImagen()).into(holder.categoryImageView, new Callback() {
            @Override
            public void onSuccess() {
                // Imagen cargada exitosamente
            }

            @Override
            public void onError(Exception e) {
                // Error al cargar la imagen
            }
        });

        // Configurar el clic en el elemento del RecyclerView
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Verificar si se estableció un listener
                if (onItemClickListener != null) {
                    // Obtener la posición actual usando getAdapterPosition()
                    int adapterPosition = holder.getAdapterPosition();
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        // Llamar al método onItemClick del listener
                        onItemClickListener.onItemClick(adapterPosition);
                    }
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return categoryList.size();
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public class CategoryViewHolder extends RecyclerView.ViewHolder {

        TextView categoryNameTextView;
        ImageView categoryImageView;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryNameTextView = itemView.findViewById(R.id.categoryNameTextView);
            categoryImageView = itemView.findViewById(R.id.categoryImageView);
        }
    }
}
