package com.example.myapplication.network;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Guarda o token de login (JWT) do usuário no armazenamento local do
 * celular (SharedPreferences), para outras telas conseguirem usar sem
 * precisar fazer login de novo a cada tela.
 *
 * A tela de Login chama saveToken(...) depois de um login bem
 * sucedido. Qualquer outra tela chama getToken(...) para pegar esse
 * token e usar no header "Authorization: Bearer <token>" das
 * chamadas ao backend.
 */
public final class SessionManager {

    private static final String PREFS_NAME = "proxima_etapa_session";
    private static final String KEY_ACCESS_TOKEN = "access_token";

    public static void saveToken(Context context, String accessToken) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_ACCESS_TOKEN, accessToken).apply();
    }

    public static String getToken(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_ACCESS_TOKEN, null);
    }

    public static void clearToken(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_ACCESS_TOKEN).apply();
    }
}
