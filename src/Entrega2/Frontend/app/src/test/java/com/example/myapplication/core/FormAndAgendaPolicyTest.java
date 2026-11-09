package com.example.myapplication.core;

import static org.junit.Assert.*;

import com.google.gson.*;
import org.junit.Test;

public class FormAndAgendaPolicyTest {
  @Test
  public void signupMatchesEightCharacterBackendRuleWithoutTrimming() {
    assertFalse(AuthInputPolicy.validSignupPassword("123456"));
    assertFalse(AuthInputPolicy.validSignupPassword("1234567"));
    assertTrue(AuthInputPolicy.validSignupPassword("12345678"));
    assertTrue(AuthInputPolicy.validSignupPassword(" 123456 "));
  }

  @Test
  public void choosesEarliestFutureInsteadOfFirstPastMeeting() {
    JsonArray meetings =
        JsonParser.parseString(
                "[{\"id\":\"old\",\"activity_date\":\"2020-01-01\",\"start_time\":\"09:00:00\"},{\"id\":\"later\",\"activity_date\":\"2035-01-02\",\"start_time\":\"09:00:00\"},{\"id\":\"next\",\"activity_date\":\"2035-01-01\",\"start_time\":\"09:00:00\"}]")
            .getAsJsonArray();
    assertEquals("next", NextMeetingPolicy.next(meetings, 1893456000000L).get("id").getAsString());
  }

  @Test
  public void ignoresInvalidOrPastDatesAndSupportsEmptyAgenda() {
    JsonArray meetings =
        JsonParser.parseString(
                "[{\"activity_date\":\"invalid\"},{\"activity_date\":\"2020-01-01\",\"start_time\":\"09:00:00\"}]")
            .getAsJsonArray();
    assertNull(NextMeetingPolicy.next(meetings, 1893456000000L));
    assertNull(NextMeetingPolicy.next(new JsonArray(), 0));
  }

  @Test
  public void agendaClockUsesSaoPauloRatherThanUtc() {
    JsonArray meetings =
        JsonParser.parseString(
                "[{\"id\":\"morning\",\"activity_date\":\"2026-11-09\",\"start_time\":\"08:00:00\"}]")
            .getAsJsonArray();
    long tenUtc = java.time.Instant.parse("2026-11-09T10:00:00Z").toEpochMilli();
    assertNotNull(NextMeetingPolicy.next(meetings, tenUtc));
    assertNull(NextMeetingPolicy.next(meetings, tenUtc + 3600001L));
  }
}
