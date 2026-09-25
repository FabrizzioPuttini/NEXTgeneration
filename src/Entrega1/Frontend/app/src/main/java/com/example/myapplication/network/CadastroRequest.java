package com.example.myapplication.network;

// O que é enviado no req.body do cadastro
public class CadastroRequest {
    private String nome;
    private String email;
    private String senha;

    public CadastroRequest(String nome, String email, String senha) {
        this.nome = nome;
        this.email = email;
        this.senha = senha;
    }
}