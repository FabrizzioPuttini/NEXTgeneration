package com.example.myapplication.network;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Configuração central do Retrofit usado por todas as telas do app.
 *
 * BASE_URL aponta para o backend Node/Express (src/Entrega 1/Backend)
 * rodando localmente (npm start), acessado a partir do EMULADOR Android.
 *
 * 10.0.2.2 é o endereço especial que o emulador usa para chegar ao
 * "localhost" da máquina onde ele está rodando. Se o teste for feito em
 * um celular físico na mesma rede Wi-Fi, troque por
 * "http://SEU_IP_LOCAL:3000/" (ex.: "http://192.168.1.50:3000/"). Se o
 * backend for publicado (Render, etc.), troque pela URL pública.
 */
public final class ApiClient {

    private static final String BASE_URL = "http://10.0.2.2:3000/";

    private static Retrofit retrofit;

    private ApiClient() {
        // Classe utilitária; não deve ser instanciada.
    }

    public static ApiService getApiService() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(ApiService.class);
    }
}
