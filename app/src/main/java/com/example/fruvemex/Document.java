package com.example.fruvemex;

public class Document {
    private String nombre; // Campo para el título
    private String imagen; // Campo para la imagen
    private String uid; // Si es necesario

    // Constructor sin argumentos
    public Document() {
        // Necesario para la deserialización
    }

    // Constructor con parámetros
    public Document(String uid, String nombre, String imagen) {
        this.uid = uid;
        this.nombre = nombre;
        this.imagen = imagen;
    }

    public String getNombre() {
        return nombre; // Método getter para 'nombre'
    }

    public void setNombre(String nombre) {
        this.nombre = nombre; // Método setter para 'nombre'
    }

    public String getImagen() {
        return imagen; // Método getter para 'imagen'
    }

    public void setImagen(String imagen) {
        this.imagen = imagen; // Método setter para 'imagen'
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }
}
