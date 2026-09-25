package com.example.myapplication.aba_cursos;

public class Curso {
    private String nome;
    private int cargaHoraria;
    private String status;


    public Curso(String nome, int cargaHoraria, String status) {
        this.nome = nome;
        this.cargaHoraria = cargaHoraria;
        this.status = status;
    }

    // Getters e Setters
    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public int getCargaHoraria() {
        return cargaHoraria;
    }

    public void setCargaHoraria(int cargaHoraria) {
        this.cargaHoraria = cargaHoraria;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}