package com.example.myapplication.network;

import android.content.Context;
import com.example.myapplication.BuildConfig;
import com.google.gson.*;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import retrofit2.*;
import retrofit2.converter.gson.GsonConverterFactory;

/** All synchronous requests run on the application's executor or WorkManager. */
public final class ApiClient {
  public static final class ApiException extends IOException {
    public final int status;

    public ApiException(int status, String message) {
      super(message);
      this.status = status;
    }
  }

  private static final Object REFRESH_LOCK = new Object();
  private static final ApiService API =
      new Retrofit.Builder()
          .baseUrl(BuildConfig.API_BASE_URL)
          .client(
              new OkHttpClient.Builder()
                  .connectTimeout(15, TimeUnit.SECONDS)
                  .readTimeout(20, TimeUnit.SECONDS)
                  .callTimeout(30, TimeUnit.SECONDS)
                  .build())
          .addConverterFactory(GsonConverterFactory.create())
          .build()
          .create(ApiService.class);

  private ApiClient() {}

  private static Response<JsonElement> execute(
      String method, String path, String token, JsonObject body) throws IOException {
    if (!path.matches("api/[a-zA-Z0-9/_-]+"))
      throw new IllegalArgumentException("Caminho inválido");
    String header = token == null ? null : "Bearer " + token;
    Call<JsonElement> call =
        "GET".equals(method)
            ? API.get(path, header)
            : "PATCH".equals(method) ? API.patch(path, header, body) : API.post(path, header, body);
    return call.execute();
  }

  private static JsonElement body(Response<JsonElement> r) throws IOException {
    if (r.isSuccessful() && r.body() != null) return r.body();
    String message =
        r.code() == 401
            ? "Sua sessão expirou. Entre novamente."
            : r.code() == 403
                ? "Você não tem permissão para esta ação."
                : "Não foi possível concluir. Tente novamente.";
    if (r.errorBody() != null) {
      try {
        JsonObject error = JsonParser.parseString(r.errorBody().string()).getAsJsonObject();
        if (error.has("error") && error.get("error").isJsonPrimitive())
          message = error.get("error").getAsString();
      } catch (RuntimeException ignored) {
        /* Non-JSON gateway response keeps safe fallback. */
      }
    }
    throw new ApiException(r.code(), message);
  }

  public static JsonElement publicPost(String path, JsonObject input) throws IOException {
    return body(execute("POST", path, null, input));
  }

  public static JsonElement request(
      Context context, String owner, String method, String path, JsonObject input)
      throws IOException {
    SessionManager.Session session = SessionManager.get(context);
    if (session == null || !owner.equals(session.userId))
      throw new ApiException(401, "Entre novamente para sincronizar.");
    Response<JsonElement> response = execute(method, path, session.accessToken, input);
    if (response.code() == 401) {
      if (response.errorBody() != null) response.errorBody().close();
      synchronized (REFRESH_LOCK) {
        SessionManager.Session current = SessionManager.get(context);
        if (current == null || !owner.equals(current.userId))
          throw new ApiException(401, "A sessão mudou.");
        if (current.accessToken.equals(session.accessToken)) {
          JsonObject refresh = new JsonObject();
          refresh.addProperty("refresh_token", current.refreshToken);
          JsonObject renewed;
          try {
            renewed = body(execute("POST", "api/auth/refresh", null, refresh)).getAsJsonObject();
          } catch (ApiException e) {
            synchronized (SessionManager.class) {
              SessionManager.Session latest = SessionManager.get(context);
              if (e.status == 401
                  && latest != null
                  && latest.accessToken.equals(current.accessToken))
                SessionManager.clearToken(context);
            }
            throw e;
          }
          SessionManager.Session next = parseSession(renewed);
          if (!owner.equals(next.userId)) throw new ApiException(401, "A sessão mudou.");
          synchronized (SessionManager.class) {
            SessionManager.Session latest = SessionManager.get(context);
            if (latest == null
                || !owner.equals(latest.userId)
                || !latest.accessToken.equals(current.accessToken))
              throw new ApiException(401, "A sessão mudou.");
            SessionManager.save(context, next);
          }
          current = next;
        }
        response = execute(method, path, current.accessToken, input);
      }
    }
    if (!SessionManager.isCurrent(context, owner)) throw new ApiException(401, "A sessão mudou.");
    return body(response);
  }

  public static SessionManager.Session parseSession(JsonObject json) throws IOException {
    try {
      SessionManager.Session s = new SessionManager.Session();
      s.userId = json.getAsJsonObject("user").get("id").getAsString();
      s.accessToken = json.get("access_token").getAsString();
      s.refreshToken = json.get("refresh_token").getAsString();
      s.expiresAt =
          json.has("expires_at") && !json.get("expires_at").isJsonNull()
              ? json.get("expires_at").getAsLong()
              : 0;
      if (!s.userId.matches("[A-Fa-f0-9-]{36}")
          || s.accessToken.isEmpty()
          || s.refreshToken.isEmpty()) throw new IllegalArgumentException();
      return s;
    } catch (RuntimeException e) {
      throw new IOException("Resposta de login incompleta.");
    }
  }
}
