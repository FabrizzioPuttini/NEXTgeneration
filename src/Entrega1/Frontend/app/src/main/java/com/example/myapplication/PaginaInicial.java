package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.aba_cursos.CursosActivity;
import com.example.myapplication.network.ApiClient;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.InicioResponse;
import com.example.myapplication.network.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PaginaInicial extends AppCompatActivity {
    LinearLayout layoutInicio, layoutCursos, layoutAgenda, layoutPerfil;
    TextView titulo, nomeCursoAtual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pagina_inicial);

        layoutInicio = findViewById(R.id.layoutInicio);
        layoutCursos = findViewById(R.id.layoutCursos);
        layoutAgenda = findViewById(R.id.layoutAgenda);
        layoutPerfil = findViewById(R.id.layoutPerfil);
        titulo = findViewById(R.id.titulo);
        nomeCursoAtual = findViewById(R.id.nomeCursoAtual);

        carregarDadosInicio();

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

    private void carregarDadosInicio() {
        String token = SessionManager.getToken(this);

        if (token == null || token.isEmpty()) {
            return;
        }

        ApiService apiService = ApiClient.getApiService();

        apiService.getDadosInicio("Bearer " + token).enqueue(new Callback<InicioResponse>() {

            @Override
            public void onResponse(Call<InicioResponse> call, Response<InicioResponse> response) {

                if (response.isSuccessful() && response.body() != null) {

                    InicioResponse dados = response.body();

                    titulo.setText("Olá, " + dados.getNome() + "! 👋");

                    if (dados.getCurso() != null) {
                        nomeCursoAtual.setText(dados.getCurso());
                    } else {
                        nomeCursoAtual.setText("Você ainda não está em nenhum curso");
                    }
                } else {
                    Log.e("PaginaInicial", "Resposta falhou: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<InicioResponse> call, Throwable t) {
                Log.e("PaginaInicial", "Erro ao carregar dados do início", t);
            }
        });
    }
}