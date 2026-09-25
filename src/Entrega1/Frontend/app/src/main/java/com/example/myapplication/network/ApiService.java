package com.example.myapplication.network;

import com.example.myapplication.AgendaItem;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * Endpoints do backend (src/Entrega 1/Backend) usados pelo app.
 */
public interface ApiService {

    /**
     * GET /api/agenda
     * Retorna os encontros dos cursos em que o aluno logado está
     * inscrito. Exige o header Authorization com o JWT do usuário
     * (ver SessionManager).
     *
     * @param authorization ex.: "Bearer <access_token>"
     * @param from          filtro opcional "YYYY-MM-DD"; pode ser null
     */
    @GET("api/agenda")
    Call<List<AgendaItem>> getAgenda(
            @Header("Authorization") String authorization,
            @Query("from") String from
    );
    
    @GET("api/cursos")
    Call<List<com.example.myapplication.aba_cursos.Curso>> getCursos();
    
    @POST("api/cursos/{id}/inscricao")
    Call<Void> inscreverCurso(
            @Header("Authorization") String authorization,
            @retrofit2.http.Path("id") String cursoId
    );
    
    @GET("api/perfil")
    Call<PerfilResponse> getPerfil(
            @Header("Authorization") String authorization
    );

    @GET("api/inicio")
    Call<InicioResponse> getDadosInicio(
            @Header("Authorization") String authorization
    );
    @POST("api/auth/login")
    Call<LoginResponse> login(@Body LoginRequest loginRequest);

    @POST("api/auth/cadastro")
    Call<CadastroResponse> cadastrar(@Body CadastroRequest cadastroRequest);
}
