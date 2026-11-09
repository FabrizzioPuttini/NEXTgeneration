package com.example.myapplication.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/** Data belongs to user_id, never to an access token or the currently visible screen. */
public final class AlunoDbHelper extends SQLiteOpenHelper {
  public AlunoDbHelper(Context context) {
    super(context.getApplicationContext(), "aluno_v2.db", null, 1);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL(
        "CREATE TABLE cache(user_id TEXT NOT NULL, resource TEXT NOT NULL, payload TEXT NOT NULL,"
            + " updated_at INTEGER NOT NULL, PRIMARY KEY(user_id,resource))");
    db.execSQL(
        "CREATE TABLE checkins(user_id TEXT NOT NULL, activity_id TEXT NOT NULL, qr_token TEXT NOT"
            + " NULL, state TEXT NOT NULL CHECK(state IN ('pending','confirmed','rejected')),"
            + " message TEXT NOT NULL, created_at INTEGER NOT NULL, PRIMARY"
            + " KEY(user_id,activity_id))");
    db.execSQL(
        "CREATE TABLE seen_notifications(user_id TEXT NOT NULL, notification_id TEXT NOT NULL,"
            + " PRIMARY KEY(user_id,notification_id))");
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    // Future versions must use additive migrations; queued attendance must never be dropped.
    throw new IllegalStateException(
        "Migração de banco não implementada: " + oldVersion + " → " + newVersion);
  }
}
