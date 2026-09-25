package com.example.myapplication;

import static android.app.ProgressDialog.show;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.aba_cursos.CursosActivity;
import com.example.myapplication.network.ApiClient;
import com.example.myapplication.network.PerfilResponse;
import com.example.myapplication.network.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PerfilActivity extends AppCompatActivity {

    ImageView avatarPerfil;
    TextView textNome, textEscola, btnSair;
    LinearLayout layoutPresenca, layoutCertificados, layoutConfiguracoes, layoutAjuda, layoutSobre, layoutInicio, layoutCursos, layoutAgenda, layoutPerfil;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

        avatarPerfil = findViewById(R.id.avatarPerfil);
        textNome = findViewById(R.id.textNome);
        textEscola = findViewById(R.id.textEscola);
        btnSair = findViewById(R.id.btnSair);
        layoutPresenca = findViewById(R.id.layoutPresenca);
        layoutCertificados = findViewById(R.id.layoutCertificados);
        layoutConfiguracoes = findViewById(R.id.layoutConfiguracoes);
        layoutAjuda = findViewById(R.id.layoutAjuda);
        layoutSobre = findViewById(R.id.layoutSobre);
        layoutInicio = findViewById(R.id.layoutInicio);
        layoutCursos = findViewById(R.id.layoutCursos);
        layoutAgenda = findViewById(R.id.layoutAgenda);
        layoutPerfil = findViewById(R.id.layoutPerfil);

        layoutInicio.setOnClickListener(v -> {
            Intent inicio = new Intent(PerfilActivity.this, PaginaInicial.class);
            startActivity(inicio);
        });
        layoutCursos.setOnClickListener(v -> {
            Intent cursos = new Intent(PerfilActivity.this, CursosActivity.class);
            startActivity(cursos);
        });
        layoutAgenda.setOnClickListener(v -> {
            Intent agenda = new Intent(PerfilActivity.this, AgendaActivity.class);
            startActivity(agenda);
        });

        layoutConfiguracoes.setOnClickListener(v -> {
            Intent configurcoes = new Intent(PerfilActivity.this, ConfiguracoesActivity.class);
            startActivity(configurcoes);
        });

        layoutSobre.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = "https://proximaetapa.org.br/";
                Intent siteProximaEtapa = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(siteProximaEtapa);
            }
        });

        // Chama a função para buscar os dados do usuário no servidor ao abrir a tela
        carregarPerfil();

        btnSair.setOnClickListener(v -> {
            logout();
        });
    }
    private void carregarPerfil(){
        String token = SessionManager.getToken(this);

        if (token == null) {
            textNome.setText("Usuário não logado");
            textEscola.setText("");
            return;
        }

        ApiClient.getApiService()
                .getPerfil("Bearer " + token)
                .enqueue(new Callback<PerfilResponse>(){
                    @Override
                    public void onResponse(Call<PerfilResponse> call, Response<PerfilResponse> response){
                        if (response.isSuccessful() && response.body() != null){
                            PerfilResponse perfil = response.body();
                            textNome.setText(perfil.getNome());
                            textEscola.setText(perfil.getEscola());
                        }
                        else {
                            try {
                                String errorBody = response.errorBody() != null ? response.errorBody().string() : "Erro desconhecido";
                                Toast.makeText(PerfilActivity.this, "Erro: " + errorBody, Toast.LENGTH_LONG).show();
                            } catch (Exception e) {
                                Toast.makeText(PerfilActivity.this, "Erro ao carregar perfil: " + response.code(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                    @Override
                    public void onFailure(Call<PerfilResponse> call, Throwable t){
                        Toast.makeText(PerfilActivity.this, "Falha na conexão: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private void logout(){
        SessionManager.clearToken(this);
        Intent logout = new Intent(PerfilActivity.this, MainActivity.class);
        startActivity(logout);
        finish();
    }

}

