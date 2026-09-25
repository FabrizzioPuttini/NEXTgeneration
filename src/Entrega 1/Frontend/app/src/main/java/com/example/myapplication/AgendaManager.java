package com.example.myapplication;

import java.util.ArrayList;
import java.util.List;


public class AgendaManager {

    private static AgendaManager instance;
    private List<AgendaItem> listaAgenda;

    private AgendaManager() {
        listaAgenda = new ArrayList<>();
        carregarAgendaMockInicial();
    }

    public static synchronized AgendaManager getInstance() {
        if (instance == null) {
            instance = new AgendaManager();
        }
        return instance;
    }

    public List<AgendaItem> getAgenda() {
        return listaAgenda;
    }

    public void adicionarItem(AgendaItem item) {
        listaAgenda.add(item);
    }

    private void carregarAgendaMockInicial() {
        listaAgenda.add(new AgendaItem(
                "2026-03-01", "19:00:00", "21:00:00",
                "Introdução à Programação", "Aula 1 — Lógica de programação",
                "Sala 3", false));

        listaAgenda.add(new AgendaItem(
                "2026-03-08", "19:00:00", "21:00:00",
                "Introdução à Programação", "Aula 2 — Variáveis e tipos",
                "Sala 3", false));

        listaAgenda.add(new AgendaItem(
                "2026-02-22", "14:00:00", "16:00:00",
                "Design Gráfico Básico", "Aula 3 — Identidade visual",
                "Sala 5", true));
    }
}
