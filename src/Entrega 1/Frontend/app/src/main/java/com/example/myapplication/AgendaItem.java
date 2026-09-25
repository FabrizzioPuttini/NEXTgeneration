package com.example.myapplication;

import com.google.gson.annotations.SerializedName;

/**
 * Representa um item da agenda do aluno.
 * Os nomes dos campos seguem exatamente as colunas retornadas pelo
 * backend em GET /api/agenda; @SerializedName mapeia o snake_case do
 * JSON (ex.: "activity_date") para os campos em camelCase usados aqui.
 */
public class AgendaItem {

    @SerializedName("activity_date")
    public String activityDate;   // ex.: "2026-03-01"

    @SerializedName("start_time")
    public String startTime;      // ex.: "19:00:00"

    @SerializedName("end_time")
    public String endTime;        // ex.: "21:00:00" (pode vir null)

    @SerializedName("course_title")
    public String courseTitle;    // ex.: "Introdução à Programação"

    @SerializedName("title")
    public String title;          // ex.: "Aula 1 — Lógica de programação"

    @SerializedName("location")
    public String location;       // ex.: "Sala 3"

    @SerializedName("attended")
    public boolean attended;      // true = aluno já compareceu a este encontro

    public AgendaItem(String activityDate, String startTime, String endTime,
                       String courseTitle, String title, String location,
                       boolean attended) {
        this.activityDate = activityDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.courseTitle = courseTitle;
        this.title = title;
        this.location = location;
        this.attended = attended;
    }
}
