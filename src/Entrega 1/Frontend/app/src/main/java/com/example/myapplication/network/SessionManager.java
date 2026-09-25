package com.example.myapplication.network;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Guarda o access_token do usuário logado (JWT do Supabase Auth,
 * retornado por POST /api/auth/login) em SharedPreferences.
 *
 * Qualquer tela pode ler o token com SessionManager.getToken(context)
 * para montar o header "Authorization: Bearer <token>" nas chamadas ao
 * backend. Quando a tela de Login estiver integrada com o backend, ela
 * deve chamar SessionManager.saveToken(context, token) logo após um
 * login bem-sucedido — a tela de Agenda (e qualquer outra) já vai
 * passar a usar o token real automaticamente, sem precisar de nenhuma
 * mudança adicional.
 */
public final class SessionManager {

    private static final String PREFS_NAME = "proxima_etapa_session";
    private static final String KEY_ACCESS_TOKEN = "access_token";

    private SessionManager() {
        // Classe utilitária; não deve ser instanciada.
    }

    public static void saveToken(Context context, String accessToken) {
        getPrefs(context)
                .edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .apply();
    }

    public static String getToken(Context context) {
        return getPrefs(context).getString(KEY_ACCESS_TOKEN, null);
    }

    public static void clearToken(Context context) {
        getPrefs(context).edit().remove(KEY_ACCESS_TOKEN).apply();
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
