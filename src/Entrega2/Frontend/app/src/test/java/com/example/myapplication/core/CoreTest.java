package com.example.myapplication.core;

import static org.junit.Assert.*;

import org.junit.Test;

public class CoreTest {
  private final String id = "22222222-2222-4222-8222-222222222222";
  private final String token = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

  @Test
  public void validQrPreservesCorrelation() {
    QrPayload p =
        QrPayload.parse("{\"v\":1,\"activityId\":\"" + id + "\",\"qrToken\":\"" + token + "\"}");
    assertNotNull(p);
    assertEquals(id, p.activityId);
    assertEquals(token, p.qrToken);
  }

  @Test
  public void rejectsMalformedAndForeignPayloads() {
    for (String s :
        new String[] {
          "",
          "https://example.com",
          "{}",
          "null",
          "[]",
          "{\"v\":2}",
          "{\"v\":1,\"activityId\":\"bad\",\"qrToken\":\"abc\"}"
        }) {
      try {
        QrPayload.parse(s);
        fail("accepted invalid payload");
      } catch (IllegalArgumentException expected) {
      }
    }
  }

  @Test
  public void retriesNetworkRateLimitAndServerFailures() {
    for (int s : new int[] {0, 408, 429, 500, 503})
      assertEquals(SyncPolicy.Action.RETRY, SyncPolicy.forStatus(s));
  }

  @Test
  public void stopsOnAuthenticationFailure() {
    assertEquals(SyncPolicy.Action.LOGIN, SyncPolicy.forStatus(401));
  }

  @Test
  public void rejectsDefinitiveFailures() {
    for (int s : new int[] {400, 403, 404, 409, 410, 422})
      assertEquals(SyncPolicy.Action.REJECT, SyncPolicy.forStatus(s));
  }

  @Test
  public void confirmsOnlySuccess() {
    assertEquals(SyncPolicy.Action.CONFIRM, SyncPolicy.forStatus(200));
    assertEquals(SyncPolicy.Action.CONFIRM, SyncPolicy.forStatus(201));
  }

  @Test
  public void requiresStringTokenAndCanonicalActivityId() {
    String upper = "ABCDEFAB-2222-4222-8222-222222222222";
    assertEquals(
        upper.toLowerCase(java.util.Locale.ROOT),
        QrPayload.parse("{\"v\":1,\"activityId\":\"" + upper + "\",\"qrToken\":\"" + token + "\"}")
            .activityId);
    try {
      QrPayload.parse(
          "{\"v\":1,\"activityId\":\"" + id + "\",\"qrToken\":12345678901234567890123456789012}");
      fail("numeric token accepted");
    } catch (IllegalArgumentException expected) {
    }
  }
}
