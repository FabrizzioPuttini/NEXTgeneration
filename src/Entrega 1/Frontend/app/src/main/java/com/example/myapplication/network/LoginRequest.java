package com.example.myapplication.network;

import com.google.gson.annotations.SerializedName;

// O que é enviado no req.body do login
public class LoginRequest {
    private String email;
    private String senha;

    public LoginRequest(String email, String senha) {
        this.email = email;
        this.senha = senha;
    }
}