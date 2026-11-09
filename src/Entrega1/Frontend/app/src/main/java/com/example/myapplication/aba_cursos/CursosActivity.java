package com.example.myapplication.aba_cursos;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.AgendaActivity;
import com.example.myapplication.PaginaInicial;
import com.example.myapplication.PerfilActivity;
import com.example.myapplication.R;
import com.example.myapplication.network.ApiClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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
        
        // 2. Conectar a lista ao Adapter e o Adapter à RecyclerView
        adapter = new CursoAdapter(listaDeCursos, (curso, position) -> {
            if ("Disponível".equalsIgnoreCase(curso.getStatus())) {
                String token = com.example.myapplication.network.SessionManager.getToken(this);
                if (token == null) {
                    Toast.makeText(this, "Usuário não logado!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Faz a chamada real para a API para inscrever no curso
                ApiClient.getApiService().inscreverCurso("Bearer " + token, curso.getId()).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            curso.setStatus("Inscrito");
                            adapter.notifyItemChanged(position);

                            // A agenda real é buscada do backend (GET /api/agenda) quando a
                            // tela de Agenda é aberta; não há mais item mockado inserido aqui.

                            Toast.makeText(CursosActivity.this, "Inscrição realizada com sucesso: " + curso.getNome(), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(CursosActivity.this, "Erro ao realizar inscrição.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Toast.makeText(CursosActivity.this, "Falha na conexão: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("API", "Erro na API de inscrição: ", t);
                    }
                });
                
            } else if ("Inscrito".equalsIgnoreCase(curso.getStatus())) {
                Toast.makeText(this, "Você já está inscrito neste curso!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Este curso já foi concluído.", Toast.LENGTH_SHORT).show();
            }
        });

        rvCursos.setAdapter(adapter);

        // 3. Buscar a lista de cursos do Backend foi movido para o onResume()
        // para garantir que os cursos carreguem sempre que a tela for aberta

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

    @Override
    protected void onResume() {
        super.onResume();
        // Toda vez que a tela de Cursos voltar a aparecer (depois de você ir para
        // outra aba e voltar), ela busca a lista de cursos do backend de novo.
        buscarCursosDoBackend();
    }

    private void buscarCursosDoBackend() {
        ApiClient.getApiService().getCursos().enqueue(new Callback<List<Curso>>() {
            @Override
            public void onResponse(Call<List<Curso>> call, Response<List<Curso>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    listaDeCursos.clear();
                    
                    // Como a API geral de cursos não devolve se o usuário está inscrito ou não,
                    // todos virão como "Disponível" por padrão (visto na classe Curso.java).
                    for (Curso c : response.body()) {
                        if (c.getStatus() == null) {
                            c.setStatus("Disponível");
                        }
                    }
                    listaDeCursos.addAll(response.body());
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(CursosActivity.this, "Erro ao carregar os cursos.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Curso>> call, Throwable t) {
                Toast.makeText(CursosActivity.this, "Falha na conexão: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("API", "Erro na API de cursos: ", t);
            }
        });
    }
}