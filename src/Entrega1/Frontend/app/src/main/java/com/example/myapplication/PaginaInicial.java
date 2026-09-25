package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.aba_cursos.CursosActivity;

public class PaginaInicial extends AppCompatActivity {
    LinearLayout layoutInicio, layoutCursos, layoutAgenda, layoutPerfil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pagina_inicial);

        layoutInicio = findViewById(R.id.layoutInicio);
        layoutCursos = findViewById(R.id.layoutCursos);
        layoutAgenda = findViewById(R.id.layoutAgenda);
        layoutPerfil = findViewById(R.id.layoutPerfil);

        layoutCursos.setOnClickListener(v -> {
            Intent cursos = new Intent(PaginaInicial.this, CursosActivity.class);
            startActivity(cursos);
        });

        layoutAgenda.setOnClickListener(v -> {
            Intent agenda = new Intent(PaginaInicial.this, AgendaActivity.class);
            startActivity(agenda);
        });

        layoutPerfil.setOnClickListener(v -> {
            Intent perfil = new Intent(PaginaInicial.this, PerfilActivity.class);
            startActivity(perfil);
        });
    }
}