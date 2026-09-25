package com.example.myapplication.network;

// O que o controller res.json(...) retorna no cadastro
public class CadastroResponse {
    private String message;
    private String error;

    public String getMessage() { return message; }
    public String getError() { return error; }
}