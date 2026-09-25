package com.example.myapplication.network;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Classe que cria a conexão com o backend usando o Retrofit.
 *
 * BASE_URL é o endereço do servidor Node/Express. 10.0.2.2 é o
 * endereço que o EMULADOR usa para chegar no "localhost" da sua
 * máquina (onde o backend está rodando com "npm start"). Se testar em
 * um celular físico, troque pelo IP da sua máquina na rede Wi-Fi.
 */
public final class ApiClient {

    private static final String BASE_URL = "https://nextgeneration-seven.vercel.app/";

    public static ApiService getApiService() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        return retrofit.create(ApiService.class);
    }
}
