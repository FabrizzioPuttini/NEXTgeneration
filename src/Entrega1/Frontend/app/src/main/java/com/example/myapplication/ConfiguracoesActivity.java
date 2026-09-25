package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.myapplication.aba_cursos.CursosActivity;

public class ConfiguracoesActivity extends AppCompatActivity {
    ImageButton btnVoltar;
    LinearLayout layoutInicio, layoutCursos, layoutAgenda, layoutPerfil;
    Switch switchTema;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuracoes);

        layoutInicio = findViewById(R.id.layoutInicio);
        layoutCursos = findViewById(R.id.layoutCursos);
        layoutAgenda = findViewById(R.id.layoutAgenda);
        layoutPerfil = findViewById(R.id.layoutPerfil);
        switchTema = findViewById(R.id.switchTema);

        btnVoltar = findViewById(R.id.btnVoltar);
        btnVoltar.setOnClickListener(v -> {
            Intent voltar = new Intent(ConfiguracoesActivity.this, PerfilActivity.class);
            startActivity(voltar);
        });

        // Verifica qual é o modo atual do aplicativo e ajusta o switch visualmente
        int currentNightMode = AppCompatDelegate.getDefaultNightMode();
        if (currentNightMode == AppCompatDelegate.MODE_NIGHT_YES) {
            switchTema.setChecked(true);
        } else {
            switchTema.setChecked(false);
        }

        switchTema.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // Ativa o tema escuro (o Android puxa as cores do colors.xml night)
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                } else {
                    // Ativa o tema claro (o Android puxa as cores do colors.xml normal)
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                }
            }
        });
        layoutInicio = findViewById(R.id.layoutInicio);
        layoutCursos = findViewById(R.id.layoutCursos);
        layoutAgenda = findViewById(R.id.layoutAgenda);
        layoutPerfil = findViewById(R.id.layoutPerfil);

        layoutInicio.setOnClickListener(v -> {
            Intent inicio = new Intent(ConfiguracoesActivity.this, PaginaInicial.class);
            startActivity(inicio);
        });
        layoutCursos.setOnClickListener(v -> {
            Intent cursos = new Intent(ConfiguracoesActivity.this, CursosActivity.class);
            startActivity(cursos);
        });
        layoutAgenda.setOnClickListener(v -> {
            Intent agenda = new Intent(ConfiguracoesActivity.this, AgendaActivity.class);
            startActivity(agenda);
        });
        layoutPerfil.setOnClickListener(v -> {
            Intent perfil = new Intent(ConfiguracoesActivity.this, PerfilActivity.class);
            startActivity(perfil);
        });
    }
}
