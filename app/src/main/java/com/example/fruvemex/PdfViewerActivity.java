package com.example.fruvemex;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.barteksc.pdfviewer.PDFView;

import java.io.File;

public class PdfViewerActivity extends AppCompatActivity {

    private PDFView pdfView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_viewer);

        pdfView = findViewById(R.id.pdfView);

        // Obtener la ruta del PDF desde el Intent
        String pdfPath = getIntent().getStringExtra("pdfPath");

        // Verificar si el archivo existe en la ruta proporcionada
        if (pdfPath != null && !pdfPath.isEmpty()) {
            File pdfFile = new File(pdfPath);
            if (pdfFile.exists()) {
                // Mostrar el PDF desde el archivo local
                loadPdfFromFile(pdfFile);
            } else {
                Toast.makeText(this, "El archivo PDF no se encuentra en la ruta especificada", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No se proporcionó una ruta válida para el PDF", Toast.LENGTH_SHORT).show();
        }
    }

    // Método para cargar el PDF desde un archivo local
    private void loadPdfFromFile(File file) {
        pdfView.fromFile(file)
                .enableSwipe(true)    // Permite el deslizamiento horizontal para cambiar de página
                .swipeHorizontal(false) // Deslizamiento vertical por defecto
                .enableDoubletap(true) // Habilita el doble toque para hacer zoom
                .defaultPage(0)       // Página inicial que se muestra
                .load();
    }
}
