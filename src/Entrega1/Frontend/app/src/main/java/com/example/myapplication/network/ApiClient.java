package com.example.myapplication.network;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Classe que cria a conexão com o backend usando o Retrofit.
 *
 * BASE_URL é o endereço do servidor Node/Express. Por padrão, aponta
 * para o backend já hospedado em produção na Vercel — não é
 * necessário rodar nada localmente para usar o app normalmente.
 *
 * Para testar contra um backend rodando na sua máquina (via "npm
 * start" em src/Entrega1/Backend), troque o valor abaixo:
 *   - Emulador Android: "http://10.0.2.2:3000/" (10.0.2.2 é como o
 *     emulador enxerga o "localhost" da máquina host)
 *   - Dispositivo físico: "http://<IP_DA_SUA_MAQUINA>:3000/" (IP da
 *     sua máquina na mesma rede Wi-Fi do celular)
 *
 * Tráfego HTTP sem TLS só é permitido para 10.0.2.2/localhost (veja
 * res/xml/network_security_config.xml) — qualquer outro host,
 * incluindo o de produção, exige HTTPS.
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
