package com.example.myapplication.aba_cursos;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.AgendaActivity;
import com.example.myapplication.PaginaInicial;
import com.example.myapplication.PerfilActivity;
import com.example.myapplication.R;
import java.util.ArrayList;
import java.util.List;

import com.example.myapplication.AgendaItem;
import com.example.myapplication.AgendaManager;

public class CursosActivity extends AppCompatActivity {

    private RecyclerView rvCursos;
    private CursoAdapter adapter;
    LinearLayout layoutInicio, layoutCursos, layoutAgenda, layoutPerfil;
    private List<Curso> listaDeCursos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cursos);

        layoutInicio = findViewById(R.id.layoutInicio);
        layoutCursos = findViewById(R.id.layoutCursos);
        layoutAgenda = findViewById(R.id.layoutAgenda);
        layoutPerfil = findViewById(R.id.layoutPerfil);

        // 1. Inicializar e configurar a RecyclerView
        rvCursos = findViewById(R.id.rvCursos);
        rvCursos.setLayoutManager(new LinearLayoutManager(this));


        listaDeCursos = new ArrayList<>();
        listaDeCursos.add(new Curso("Preparatório ENEM & Redação Nota 1000", 40, "Inscrito"));
        listaDeCursos.add(new Curso("Workshop: Carreiras em Tecnologia e Programação", 16, "Disponível"));
        listaDeCursos.add(new Curso("Orientação Profissional e Mercado de Trabalho", 20, "Disponível"));
        listaDeCursos.add(new Curso("Imersão Universitária - FECAP & Parceiras", 10, "Concluído"));
        listaDeCursos.add(new Curso("Empreendedorismo Jovem e Liderança", 24, "Disponível"));

        // 3. Conectar a lista ao Adapter e o Adapter à RecyclerView
        adapter = new CursoAdapter(listaDeCursos, (curso, position) -> {
            if ("Disponível".equalsIgnoreCase(curso.getStatus())) {
                curso.setStatus("Inscrito");
                adapter.notifyItemChanged(position);
                
                // Adiciona na agenda global
                AgendaManager.getInstance().adicionarItem(new AgendaItem("2026-04-10", "19:00:00", "21:00:00", "Primeiro Encontro", "Aula 1 — " + curso.getNome(), "Sala Virtual 1", false));
                
                android.widget.Toast.makeText(this, "Inscrição realizada com sucesso: " + curso.getNome(), android.widget.Toast.LENGTH_SHORT).show();
            } else if ("Inscrito".equalsIgnoreCase(curso.getStatus())) {
                android.widget.Toast.makeText(this, "Você já está inscrito neste curso!", android.widget.Toast.LENGTH_SHORT).show();
            } else {
                android.widget.Toast.makeText(this, "Este curso já foi concluído.", android.widget.Toast.LENGTH_SHORT).show();
            }
        });

        rvCursos.setAdapter(adapter);
        layoutInicio.setOnClickListener(v -> {
            Intent inicio = new Intent(CursosActivity.this, PaginaInicial.class);
            startActivity(inicio);
        });
        layoutAgenda.setOnClickListener(v -> {
            Intent agenda = new Intent(CursosActivity.this, AgendaActivity.class);
            startActivity(agenda);
        });
        layoutPerfil.setOnClickListener(v -> {
            Intent perfil = new Intent(CursosActivity.this, PerfilActivity.class);
            startActivity(perfil);
        });
    }
}