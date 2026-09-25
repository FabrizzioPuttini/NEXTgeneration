package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.aba_cursos.CursosActivity;
import com.example.myapplication.network.ApiClient;
import com.example.myapplication.network.SessionManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Tela de Agenda do aluno.
 *
 * Ao abrir a tela, chamamos o backend (GET /api/agenda) para buscar os
 * encontros do aluno logado e mostramos um card para cada um.
 */
public class AgendaActivity extends AppCompatActivity {

    LinearLayout agendaContainer, layoutInicio, layoutCursos, layoutAgenda, layoutPerfil;
    TextView textVazio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agenda);

        agendaContainer = findViewById(R.id.agendaContainer);
        layoutInicio = findViewById(R.id.layoutInicio);
        layoutCursos = findViewById(R.id.layoutCursos);
        layoutAgenda = findViewById(R.id.layoutAgenda);
        layoutPerfil = findViewById(R.id.layoutPerfil);
        textVazio = findViewById(R.id.textVazio);

        // Menu inferior: troca de tela ao tocar em cada ícone.
        layoutInicio.setOnClickListener(v -> {
            Intent inicio = new Intent(AgendaActivity.this, PaginaInicial.class);
            startActivity(inicio);
        });
        layoutCursos.setOnClickListener(v -> {
            Intent cursos = new Intent(AgendaActivity.this, CursosActivity.class);
            startActivity(cursos);
        });
        layoutPerfil.setOnClickListener(v -> {
            Intent perfil = new Intent(AgendaActivity.this, PerfilActivity.class);
            startActivity(perfil);
        });

        buscarAgendaDoBackend();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Busca a agenda de novo toda vez que a tela volta a aparecer
        // (por exemplo, depois de o aluno se inscrever em um curso).
        buscarAgendaDoBackend();
    }

    /**
     * Pede a lista de encontros do aluno logado ao backend.
     */
    private void buscarAgendaDoBackend() {
        String token = SessionManager.getToken(this);
        if (token == null) {
            Toast.makeText(this, "Usuário não logado!", Toast.LENGTH_SHORT).show();
            return;
        }

        // "enqueue" faz a chamada em segundo plano e chama o Callback
        // quando a resposta chegar, sem travar a tela.
        Call<List<AgendaItem>> chamada = ApiClient.getApiService().getAgenda("Bearer " + token, null);

        chamada.enqueue(new Callback<List<AgendaItem>>() {
            @Override
            public void onResponse(Call<List<AgendaItem>> call, Response<List<AgendaItem>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    exibirAgenda(response.body());
                } else {
                    Toast.makeText(AgendaActivity.this, "Erro ao carregar a agenda.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<AgendaItem>> call, Throwable t) {
                Toast.makeText(AgendaActivity.this, "Falha na conexão com o servidor.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Cria um card para cada encontro dentro do agendaContainer.
     * Se a lista estiver vazia, mostra a mensagem de "nenhum encontro".
     */
    private void exibirAgenda(List<AgendaItem> agenda) {
        agendaContainer.removeAllViews();

        if (agenda.isEmpty()) {
            textVazio.setVisibility(View.VISIBLE);
            return;
        }
        textVazio.setVisibility(View.GONE);

        LayoutInflater inflater = LayoutInflater.from(this);

        for (AgendaItem item : agenda) {
            View card = inflater.inflate(R.layout.item_agenda, agendaContainer, false);

            TextView textDataHora = card.findViewById(R.id.textDataHora);
            TextView textStatus = card.findViewById(R.id.textStatus);
            TextView textTituloEncontro = card.findViewById(R.id.textTituloEncontro);
            TextView textCurso = card.findViewById(R.id.textCurso);
            TextView textLocal = card.findViewById(R.id.textLocal);

            textDataHora.setText(formatarDataHora(item));
            textStatus.setText(item.attended ? "Presença confirmada" : "Aguardando encontro");
            textTituloEncontro.setText(item.title);
            textCurso.setText(item.courseTitle);
            textLocal.setText("📍 " + item.location);

            agendaContainer.addView(card);
        }
    }

    /**
     * Monta o texto de data e horário do card, ex.: "01/03 · 19:00 - 21:00".
     */
    private String formatarDataHora(AgendaItem item) {
        // Data vem como "2026-03-01"; pegamos só dia/mês.
        String[] partesData = item.activityDate.split("-");
        String dataFormatada = partesData[2] + "/" + partesData[1];

        // Hora vem como "19:00:00"; pegamos só HH:mm.
        String horaInicio = item.startTime.substring(0, 5);

        if (item.endTime == null) {
            return dataFormatada + " · " + horaInicio;
        }
        String horaFim = item.endTime.substring(0, 5);
        return dataFormatada + " · " + horaInicio + " - " + horaFim;
    }
}
