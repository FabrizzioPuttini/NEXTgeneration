package com.example.myapplication.network;

import com.google.gson.annotations.SerializedName;

// O que o controller res.json(...) retorna no login
public class LoginResponse {
    private String message;

    @SerializedName("access_token")
    private String accessToken;

    @SerializedName("refresh_token")
    private String refreshToken;

    private String error;

    public String getMessage() { return message; }
    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public String getError() { return error; }
}