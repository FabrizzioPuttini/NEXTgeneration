package com.example.myapplication.sync;

import android.content.Context;
import androidx.work.*;
import java.util.concurrent.TimeUnit;

public final class SyncScheduler {
  private SyncScheduler() {}

  public static void start(Context c, String owner) {
    Data data = new Data.Builder().putString("owner", owner).build();
    Constraints network =
        new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
    WorkManager work = WorkManager.getInstance(c);
    work.enqueueUniquePeriodicWork(
        "notificacoes-" + owner,
        ExistingPeriodicWorkPolicy.KEEP,
        new PeriodicWorkRequest.Builder(SyncWorker.class, 15, TimeUnit.MINUTES)
            .setInputData(data)
            .setConstraints(network)
            .addTag("aluno-sync")
            .build());
    sync(c, owner);
  }

  public static void sync(Context c, String owner) {
    WorkManager.getInstance(c)
        .enqueueUniqueWork(
            "checkins-" + owner,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setInputData(new Data.Builder().putString("owner", owner).build())
                .setConstraints(
                    new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .addTag("aluno-sync")
                .build());
  }

  public static void stop(Context c) {
    WorkManager.getInstance(c).cancelAllWorkByTag("aluno-sync");
  }
}
