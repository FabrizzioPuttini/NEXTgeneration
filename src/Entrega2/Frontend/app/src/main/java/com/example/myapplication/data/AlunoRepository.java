package com.example.myapplication.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.example.myapplication.core.QrPayload;
import java.util.ArrayList;
import java.util.List;

/** Call on a background thread. SQLite statements use bound parameters. */
public final class AlunoRepository implements AutoCloseable {
  private final AlunoDbHelper helper;

  public AlunoRepository(Context c) {
    helper = new AlunoDbHelper(c);
  }

  public static final class Cached {
    public final String payload;
    public final long updatedAt;

    Cached(String payload, long updatedAt) {
      this.payload = payload;
      this.updatedAt = updatedAt;
    }
  }

  public static final class Pending {
    public String activityId, qrToken, state, message;
    public long createdAt;
  }

  public Cached cache(String owner, String resource) {
    try (Cursor c =
        helper
            .getReadableDatabase()
            .rawQuery(
                "SELECT payload,updated_at FROM cache WHERE user_id=? AND resource=?",
                new String[] {owner, resource})) {
      return c.moveToFirst() ? new Cached(c.getString(0), c.getLong(1)) : null;
    }
  }

  public void saveCache(String owner, String resource, String payload) {
    ContentValues v = new ContentValues();
    v.put("user_id", owner);
    v.put("resource", resource);
    v.put("payload", payload);
    v.put("updated_at", System.currentTimeMillis());
    helper
        .getWritableDatabase()
        .insertWithOnConflict("cache", null, v, SQLiteDatabase.CONFLICT_REPLACE);
  }

  public void enqueue(String owner, QrPayload qr) {
    SQLiteDatabase db = helper.getWritableDatabase();
    db.beginTransaction();
    try {
      ContentValues v = new ContentValues();
      v.put("user_id", owner);
      v.put("activity_id", qr.activityId);
      v.put("qr_token", qr.qrToken);
      v.put("state", "pending");
      v.put("message", "Aguardando sincronização");
      v.put("created_at", System.currentTimeMillis());
      db.insertWithOnConflict("checkins", null, v, SQLiteDatabase.CONFLICT_IGNORE);
      v.remove("user_id");
      v.remove("activity_id");
      db.update(
          "checkins",
          v,
          "user_id=? AND activity_id=? AND state='rejected'",
          new String[] {owner, qr.activityId});
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
  }

  public List<Pending> checkins(String owner, boolean pendingOnly) {
    List<Pending> result = new ArrayList<>();
    try (Cursor c =
        helper
            .getReadableDatabase()
            .rawQuery(
                "SELECT activity_id,qr_token,state,message,created_at FROM checkins WHERE user_id=?"
                    + (pendingOnly ? " AND state='pending'" : "")
                    + " ORDER BY created_at DESC",
                new String[] {owner})) {
      while (c.moveToNext()) {
        Pending p = new Pending();
        p.activityId = c.getString(0);
        p.qrToken = c.getString(1);
        p.state = c.getString(2);
        p.message = c.getString(3);
        p.createdAt = c.getLong(4);
        result.add(p);
      }
    }
    return result;
  }

  public void state(String owner, String activity, String state, String message) {
    ContentValues v = new ContentValues();
    v.put("state", state);
    v.put("message", message);
    if (!"pending".equals(state)) v.put("qr_token", "");
    helper
        .getWritableDatabase()
        .update("checkins", v, "user_id=? AND activity_id=?", new String[] {owner, activity});
  }

  public boolean notificationSeen(String owner, String id) {
    try (Cursor c =
        helper
            .getReadableDatabase()
            .rawQuery(
                "SELECT 1 FROM seen_notifications WHERE user_id=? AND notification_id=?",
                new String[] {owner, id})) {
      return c.moveToFirst();
    }
  }

  public void markNotificationSeen(String owner, String id) {
    ContentValues v = new ContentValues();
    v.put("user_id", owner);
    v.put("notification_id", id);
    helper
        .getWritableDatabase()
        .insertWithOnConflict("seen_notifications", null, v, SQLiteDatabase.CONFLICT_IGNORE);
  }

  @Override
  public void close() {
    helper.close();
  }
}
