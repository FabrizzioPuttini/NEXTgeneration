package com.example.myapplication.sync;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.work.*;
import com.example.myapplication.HubActivity;
import com.example.myapplication.R;
import com.example.myapplication.core.SyncPolicy;
import com.example.myapplication.data.AlunoRepository;
import com.example.myapplication.network.*;
import com.google.gson.*;
import java.io.IOException;

public final class SyncWorker extends Worker {
  public SyncWorker(@NonNull Context c, @NonNull WorkerParameters p) {
    super(c, p);
  }

  @NonNull
  @Override
  public Result doWork() {
    String owner = getInputData().getString("owner");
    Context c = getApplicationContext();
    if (owner == null || !SessionManager.isCurrent(c, owner)) return Result.success();
    boolean retry = false;
    try (AlunoRepository repository = new AlunoRepository(c)) {
      for (AlunoRepository.Pending pending : repository.checkins(owner, true)) {
        if (isStopped() || !SessionManager.isCurrent(c, owner)) return Result.success();
        try {
          JsonObject input = new JsonObject();
          input.addProperty("qrToken", pending.qrToken);
          input.addProperty("activityId", pending.activityId);
          ApiClient.request(c, owner, "POST", "api/presencas/check-in", input);
          repository.state(owner, pending.activityId, "confirmed", "Presença confirmada");
        } catch (ApiClient.ApiException e) {
          SyncPolicy.Action action = SyncPolicy.forStatus(e.status);
          if (action == SyncPolicy.Action.LOGIN) return Result.success();
          if (action == SyncPolicy.Action.RETRY) {
            retry = true;
            continue;
          }
          // A lost response can look expired on retry: reconcile before rejection.
          try {
            JsonArray history =
                ApiClient.request(c, owner, "GET", "api/presencas", null).getAsJsonArray();
            repository.saveCache(owner, "presencas", history.toString());
            boolean found = false;
            for (JsonElement row : history)
              if (pending.activityId.equals(row.getAsJsonObject().get("activity_id").getAsString()))
                found = true;
            repository.state(
                owner,
                pending.activityId,
                found ? "confirmed" : "rejected",
                found ? "Presença confirmada" : e.getMessage());
          } catch (IOException | RuntimeException failure) {
            retry = true;
          }
        } catch (IOException e) {
          retry = true;
        }
      }
      if (!SessionManager.isCurrent(c, owner)) return Result.success();
      try {
        JsonArray history =
            ApiClient.request(c, owner, "GET", "api/presencas", null).getAsJsonArray();
        repository.saveCache(owner, "presencas", history.toString());
        for (JsonElement row : history)
          repository.state(
              owner,
              row.getAsJsonObject().get("activity_id").getAsString(),
              "confirmed",
              "Presença confirmada");
        JsonArray notifications =
            ApiClient.request(c, owner, "GET", "api/notificacoes", null).getAsJsonArray();
        repository.saveCache(owner, "notificacoes", notifications.toString());
        for (JsonElement element : notifications) {
          JsonObject n = element.getAsJsonObject();
          String id = n.get("id").getAsString();
          if (!n.get("lida").getAsBoolean()
              && !repository.notificationSeen(owner, id)
              && SessionManager.isCurrent(c, owner)
              && notifyStudent(n)) repository.markNotificationSeen(owner, id);
        }
      } catch (IOException | RuntimeException failure) {
        retry = true;
      }
    }
    return retry && getRunAttemptCount() < 5 ? Result.retry() : Result.success();
  }

  private boolean notifyStudent(JsonObject item) {
    Context c = getApplicationContext();
    if (Build.VERSION.SDK_INT >= 33
        && ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) return false;
    NotificationManager manager =
        (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
    if (manager == null || !manager.areNotificationsEnabled()) return false;
    if (Build.VERSION.SDK_INT >= 26)
      manager.createNotificationChannel(
          new NotificationChannel(
              "aluno", "Avisos da Próxima Etapa", NotificationManager.IMPORTANCE_DEFAULT));
    Intent intent = new Intent(c, HubActivity.class).putExtra("section", "notificacoes");
    PendingIntent open =
        PendingIntent.getActivity(
            c, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    // Lock-screen content is generic; details remain behind the student session.
    manager.notify(
        item.get("id").getAsString().hashCode(),
        new NotificationCompat.Builder(c, "aluno")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Próxima Etapa")
            .setContentText("Você tem um novo aviso. Abra o aplicativo para consultar.")
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build());
    return true;
  }
}
