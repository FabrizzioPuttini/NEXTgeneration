package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.aba_cursos.CursosActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Tela de Agenda do aluno.
 *
 * Neste momento a tela usa dados de exemplo (mock), definidos em
 * carregarAgendaMock(). Quando o backend estiver integrado, basta
 * substituir essa chamada por uma requisição HTTP para
 * GET /api/agenda (ver README do backend em src/Entrega 1/Backend),
 * mantendo o mesmo formato de AgendaItem e reaproveitando o método
 * exibirAgenda() para preencher a tela.
 */
public class AgendaActivity extends AppCompatActivity {

    LinearLayout agendaContainer, layoutInicio, layoutCursos, layoutAgenda, layoutPerfil;
    TextView textVazio;

    @Override
    protected void onResume() {
        super.onResume();
        // Recarrega a lista atualizada com as novas inscrições quando a tela volta a aparecer
        List<AgendaItem> agendaAtualizada = AgendaManager.getInstance().getAgenda();
        exibirAgenda(agendaAtualizada);
    }

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

        List<AgendaItem> agenda = AgendaManager.getInstance().getAgenda();
        exibirAgenda(agenda);
    }

    /**
     * Dados de exemplo para visualizar a tela antes da integração com o
     * backend. O formato de cada item já corresponde exatamente ao que
     * GET /api/agenda devolve (colunas da view my_agenda).
     */
    private List<AgendaItem> carregarAgendaMock() {
        List<AgendaItem> lista = new ArrayList<>();

        lista.add(new AgendaItem(
                "2026-03-01", "19:00:00", "21:00:00",
                "Introdução à Programação", "Aula 1 — Lógica de programação",
                "Sala 3", false));

        lista.add(new AgendaItem(
                "2026-03-08", "19:00:00", "21:00:00",
                "Introdução à Programação", "Aula 2 — Variáveis e tipos",
                "Sala 3", false));

        lista.add(new AgendaItem(
                "2026-02-22", "14:00:00", "16:00:00",
                "Design Gráfico Básico", "Aula 3 — Identidade visual",
                "Sala 5", true));

        return lista;
    }

    /**
     * Monta um card por item da agenda dentro do agendaContainer.
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
     * Formata a data (YYYY-MM-DD) e o intervalo de horário (HH:MM:SS) do
     * encontro para exibição, ex.: "01/03 · 19:00 - 21:00".
     */
    private String formatarDataHora(AgendaItem item) {
        String[] partesData = item.activityDate.split("-");
        String dataFormatada = partesData.length == 3
                ? partesData[2] + "/" + partesData[1]
                : item.activityDate;

        String horaInicio = item.startTime.length() >= 5 ? item.startTime.substring(0, 5) : item.startTime;
        String horaFim = item.endTime.length() >= 5 ? item.endTime.substring(0, 5) : item.endTime;

        return dataFormatada + " · " + horaInicio + " - " + horaFim;
    }
}
