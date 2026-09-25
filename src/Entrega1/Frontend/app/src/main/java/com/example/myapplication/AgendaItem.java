package com.example.myapplication;

import com.google.gson.annotations.SerializedName;

/**
 * Um encontro (aula) da agenda do aluno.
 *
 * O backend devolve o JSON com nomes separados por "_" (snake_case),
 * por exemplo "activity_date". A anotação @SerializedName diz ao Gson
 * (biblioteca que transforma JSON em objeto Java) em qual campo desta
 * classe cada valor do JSON deve ser colocado.
 */
public class AgendaItem {

    @SerializedName("activity_date")
    public String activityDate; // data do encontro, ex.: "2026-03-01"

    @SerializedName("start_time")
    public String startTime;    // hora de início, ex.: "19:00:00"

    @SerializedName("end_time")
    public String endTime;      // hora de término (pode não vir preenchida)

    @SerializedName("course_title")
    public String courseTitle;  // nome do curso

    @SerializedName("title")
    public String title;        // título do encontro, ex.: "Aula 1"

    @SerializedName("location")
    public String location;     // local do encontro

    @SerializedName("attended")
    public boolean attended;    // true = aluno já compareceu a este encontro
}
