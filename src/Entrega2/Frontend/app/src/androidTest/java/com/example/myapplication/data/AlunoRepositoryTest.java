package com.example.myapplication.data;

import static org.junit.Assert.*;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.myapplication.core.QrPayload;
import org.junit.*;
import org.junit.runner.RunWith;

/** Integration tests use only the test device's sandbox with synthetic owners. */
@RunWith(AndroidJUnit4.class)
public class AlunoRepositoryTest {
  private Context context;
  private final String a = "test-owner-a", b = "test-owner-b";

  private QrPayload qr(String suffix) {
    return QrPayload.parse(
        "{\"v\":1,\"activityId\":\"22222222-2222-4222-8222-222222222222\",\"qrToken\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
            + suffix
            + "\"}");
  }

  @Before
  public void setup() {
    context = ApplicationProvider.getApplicationContext();
    context.deleteDatabase("aluno_v2.db");
  }

  @After
  public void cleanup() {
    context.deleteDatabase("aluno_v2.db");
  }

  @Test
  public void queueSurvivesReopenAndDeduplicates() {
    try (AlunoRepository r = new AlunoRepository(context)) {
      r.enqueue(a, qr("1"));
      r.enqueue(a, qr("1"));
      assertEquals(1, r.checkins(a, true).size());
    }
    try (AlunoRepository r = new AlunoRepository(context)) {
      assertEquals(1, r.checkins(a, true).size());
      assertEquals("pending", r.checkins(a, true).get(0).state);
    }
  }

  @Test
  public void ownerIsolationIncludesCacheQueueAndNotifications() {
    try (AlunoRepository r = new AlunoRepository(context)) {
      r.saveCache(a, "agenda", "[]");
      r.enqueue(a, qr("1"));
      r.markNotificationSeen(a, "notice");
      assertNull(r.cache(b, "agenda"));
      assertTrue(r.checkins(b, false).isEmpty());
      assertFalse(r.notificationSeen(b, "notice"));
      r.enqueue(b, qr("2"));
      r.state(a, qr("1").activityId, "confirmed", "Confirmed");
      assertTrue(r.checkins(a, true).isEmpty());
      assertEquals(1, r.checkins(b, true).size());
    }
  }

  @Test
  public void terminalStateErasesQrButRejectedCanBeRescanned() {
    try (AlunoRepository r = new AlunoRepository(context)) {
      r.enqueue(a, qr("1"));
      r.state(a, qr("1").activityId, "rejected", "Expired");
      assertEquals("", r.checkins(a, false).get(0).qrToken);
      r.enqueue(a, qr("2"));
      assertEquals(qr("2").qrToken, r.checkins(a, true).get(0).qrToken);
      r.state(a, qr("1").activityId, "confirmed", "Confirmed");
      r.enqueue(a, qr("3"));
      assertTrue(r.checkins(a, true).isEmpty());
      assertEquals(1, r.checkins(a, false).size());
    }
  }
}
