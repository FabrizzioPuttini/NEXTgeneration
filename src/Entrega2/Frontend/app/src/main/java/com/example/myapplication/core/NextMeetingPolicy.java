package com.example.myapplication.core;

import com.google.gson.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Agenda returns date/time fields formatted in America/Sao_Paulo; never label an old encounter as
 * upcoming.
 */
public final class NextMeetingPolicy {
  private NextMeetingPolicy() {}

  public static JsonObject next(JsonArray meetings, long now) {
    JsonObject next = null;
    long first = Long.MAX_VALUE;
    SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
    parser.setLenient(false);
    parser.setTimeZone(TimeZone.getTimeZone("America/Sao_Paulo"));
    for (JsonElement element : meetings) {
      try {
        JsonObject meeting = element.getAsJsonObject();
        String date = meeting.get("activity_date").getAsString();
        String time =
            meeting.has("start_time") && !meeting.get("start_time").isJsonNull()
                ? meeting.get("start_time").getAsString()
                : "00:00:00";
        if (!date.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}.*")
            || !time.matches("[0-9]{2}:[0-9]{2}(:[0-9]{2})?")) continue;
        if (time.length() == 5) time += ":00";
        Date parsed = parser.parse(date.substring(0, 10) + " " + time);
        if (parsed != null && parsed.getTime() >= now && parsed.getTime() < first) {
          first = parsed.getTime();
          next = meeting;
        }
      } catch (Exception invalid) {
        /* Malformed/absent dates cannot establish an upcoming meeting. */
      }
    }
    return next;
  }
}
