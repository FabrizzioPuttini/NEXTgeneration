package com.example.myapplication.core;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.regex.Pattern;

/** QR identifies an encounter; only the backend can authorize attendance. */
public final class QrPayload {
  private static final Pattern UUID =
      Pattern.compile(
          "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
  public final String activityId;
  public final String qrToken;

  private QrPayload(String activityId, String qrToken) {
    this.activityId = activityId;
    this.qrToken = qrToken;
  }

  public static QrPayload parse(String raw) {
    try {
      if (raw == null || raw.length() > 2048) throw new IllegalArgumentException();
      JsonObject o = JsonParser.parseString(raw).getAsJsonObject();
      if (!o.get("v").isJsonPrimitive()
          || !o.get("v").getAsJsonPrimitive().isNumber()
          || !"1".equals(o.get("v").getAsString())) throw new IllegalArgumentException();
      if (!o.get("activityId").isJsonPrimitive()
          || !o.get("activityId").getAsJsonPrimitive().isString()
          || !o.get("qrToken").isJsonPrimitive()
          || !o.get("qrToken").getAsJsonPrimitive().isString())
        throw new IllegalArgumentException();
      String id = o.get("activityId").getAsString().toLowerCase(java.util.Locale.ROOT);
      String token = o.get("qrToken").getAsString();
      if (!UUID.matcher(id).matches() || !token.matches("[A-Za-z0-9_-]{32,256}"))
        throw new IllegalArgumentException();
      return new QrPayload(id, token);
    } catch (RuntimeException e) {
      throw new IllegalArgumentException(
          "QR inválido. Use o código exibido pela equipe do encontro.");
    }
  }
}
