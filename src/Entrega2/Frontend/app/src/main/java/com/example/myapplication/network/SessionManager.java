package com.example.myapplication.network;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import com.google.gson.Gson;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Session encrypted with a non-exportable Android Keystore key; excluded from backup. */
public final class SessionManager {
  private static final String PREFS = "aluno_session_v2", KEY = "aluno_session_key_v2";

  public static final class Session {
    public String userId, accessToken, refreshToken;
    public long expiresAt;
  }

  private static SecretKey key() throws Exception {
    KeyStore store = KeyStore.getInstance("AndroidKeyStore");
    store.load(null);
    if (!store.containsAlias(KEY)) {
      KeyGenerator g = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
      g.init(
          new KeyGenParameterSpec.Builder(
                  KEY, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
              .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
              .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
              .build());
      return g.generateKey();
    }
    return (SecretKey) store.getKey(KEY, null);
  }

  public static synchronized Session get(Context c) {
    String raw = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("session", null);
    if (raw == null) return null;
    try {
      String[] parts = raw.split(":");
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(
          Cipher.DECRYPT_MODE,
          key(),
          new GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)));
      Session s =
          new Gson()
              .fromJson(
                  new String(
                      cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)),
                      java.nio.charset.StandardCharsets.UTF_8),
                  Session.class);
      return s != null && s.userId != null && s.accessToken != null ? s : null;
    } catch (Exception e) {
      clearToken(c);
      return null;
    }
  }

  public static synchronized void save(Context c, Session s) {
    if (s == null || s.userId == null || s.accessToken == null || s.refreshToken == null)
      throw new IllegalArgumentException("Sessão incompleta");
    try {
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE, key());
      byte[] bytes =
          cipher.doFinal(new Gson().toJson(s).getBytes(java.nio.charset.StandardCharsets.UTF_8));
      String encoded =
          Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP)
              + ":"
              + Base64.encodeToString(bytes, Base64.NO_WRAP);
      if (!c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
          .edit()
          .putString("session", encoded)
          .commit()) throw new IllegalStateException("Não foi possível salvar sessão");
    } catch (Exception e) {
      throw new IllegalStateException("Não foi possível proteger a sessão neste aparelho.", e);
    }
  }

  public static synchronized boolean isCurrent(Context c, String owner) {
    Session s = get(c);
    return s != null && owner.equals(s.userId);
  }

  public static String getToken(Context c) {
    Session s = get(c);
    return s == null ? null : s.accessToken;
  }

  public static synchronized void clearToken(Context c) {
    c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().commit();
  }
}
