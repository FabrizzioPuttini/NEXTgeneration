package com.example.myapplication.network;

import com.google.gson.annotations.SerializedName;

public class PerfilResponse {

    @SerializedName("nome")
    private String nome;

    @SerializedName("escola")
    private String escola;

    public String getNome() {
        return nome;
    }

    public String getEscola() {
        return escola;
    }
}