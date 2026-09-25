package com.example.myapplication.aba_cursos;

import com.google.gson.annotations.SerializedName;

public class Curso {
    
    @SerializedName("id")
    private String id;

    @SerializedName("title") 
    private String nome;
    
    @SerializedName("workload")
    private String cargaHoraria; // Mudamos para String para evitar qualquer erro numérico do Gson
    
    private String status = "Disponível"; 

    public Curso(String nome, String cargaHoraria, String status) {
        this.nome = nome;
        this.cargaHoraria = cargaHoraria;
        this.status = status;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    
    public String getCargaHoraria() { return cargaHoraria; }
    public void setCargaHoraria(String cargaHoraria) { this.cargaHoraria = cargaHoraria; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}