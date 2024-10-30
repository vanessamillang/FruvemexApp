package com.example.fruvemex;

public class Category {
    private String nombre;
    private String imagen;
    private String uid;

    public Category(String uid, String nombre, String imagen) {
        this.uid = uid;
        this.nombre = nombre;
        this.imagen = imagen;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getImagen() {
        return imagen;
    }

    public void setImagen(String imagen) {
        this.imagen = imagen;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }
}
