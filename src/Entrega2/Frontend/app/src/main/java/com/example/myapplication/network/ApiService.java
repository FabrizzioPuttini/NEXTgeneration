package com.example.myapplication.network;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {
  @GET("{path}")
  Call<JsonElement> get(
      @Path(value = "path", encoded = true) String path,
      @Header("Authorization") String authorization);

  @POST("{path}")
  Call<JsonElement> post(
      @Path(value = "path", encoded = true) String path,
      @Header("Authorization") String authorization,
      @Body JsonObject body);

  @PATCH("{path}")
  Call<JsonElement> patch(
      @Path(value = "path", encoded = true) String path,
      @Header("Authorization") String authorization,
      @Body JsonObject body);
}
