package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.myapplication.core.AuthAttemptGate;
import com.example.myapplication.core.AuthInputPolicy;
import com.example.myapplication.network.*;
import com.example.myapplication.sync.SyncScheduler;
import com.google.gson.JsonObject;

/** Authentication keeps password verbatim and never persists form contents. */
public class AuthActivity extends AppCompatActivity {
  protected boolean signup() {
    return false;
  }

  private static final AuthAttemptGate ATTEMPTS = new AuthAttemptGate();
  private long activeAttempt;
  private boolean busy;

  @Override
  protected void onDestroy() {
    ATTEMPTS.invalidate(activeAttempt);
    super.onDestroy();
  }

  @Override
  protected void onCreate(Bundle saved) {
    super.onCreate(saved);
    activeAttempt = ATTEMPTS.begin();
    setContentView(R.layout.activity_auth);
    View root = findViewById(R.id.screenRoot);
    ViewCompat.setOnApplyWindowInsetsListener(
        root,
        (v, insets) -> {
          androidx.core.graphics.Insets s =
              insets.getInsets(
                  WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
          v.setPadding(s.left, s.top, s.right, s.bottom);
          return insets;
        });
    EditText name = findViewById(R.id.authName),
        email = findViewById(R.id.authEmail),
        password = findViewById(R.id.authPassword);
    password.setSaveEnabled(false);
    email.setSaveEnabled(false);
    name.setSaveEnabled(false);
    Button submit = findViewById(R.id.authSubmit), change = findViewById(R.id.authSwitch);
    TextView status = findViewById(R.id.authStatus);
    if (signup()) {
      findViewById(R.id.nameLayout).setVisibility(View.VISIBLE);
      ((TextView) findViewById(R.id.authTitle)).setText("Crie sua próxima etapa");
      submit.setText("Criar conta");
      change.setText("Já tenho uma conta");
    }
    change.setOnClickListener(v -> {
      startActivity(new Intent(this, signup() ? LoginActivity.class : CadastroActivity.class));
      finish();
    });
    submit.setOnClickListener(v -> {
      if (busy) return;
      String mail = email.getText().toString().trim(),
          secret = password.getText().toString(),
          full = name.getText().toString().trim();
      if (!android.util.Patterns.EMAIL_ADDRESS.matcher(mail).matches()) {
        email.setError("Informe um e-mail válido");
        return;
      }
      if (secret.isEmpty() || (signup() && !AuthInputPolicy.validSignupPassword(secret))) {
        password.setError("Informe uma senha" + (signup() ? " de ao menos 8 caracteres" : ""));
        return;
      }
      if (signup() && full.length() < 2) {
        name.setError("Informe seu nome");
        return;
      }
      busy = true;
      final long request = activeAttempt = ATTEMPTS.begin();
      submit.setEnabled(false);
      change.setEnabled(false);
      status.setText(signup() ? "Criando conta…" : "Entrando…");
      JsonObject body = new JsonObject();
      body.addProperty("email", mail);
      body.addProperty("senha", secret);
      if (signup()) body.addProperty("nome", full);
      AlunoApplication.IO.execute(() -> {
        try {
          JsonObject result =
              ApiClient.publicPost(signup() ? "api/auth/cadastro" : "api/auth/login", body)
                  .getAsJsonObject();
          SessionManager.Session parsed = signup() ? null : ApiClient.parseSession(result);
          runOnUiThread(() ->
              ATTEMPTS.complete(
                  request,
                  !isFinishing() && !isDestroyed(),
                  () -> {
                    // UI lifecycle and generation are checked before the only session commit.
                    try {
                      if (parsed != null) SessionManager.save(this, parsed);
                    } catch (IllegalStateException e) {
                      busy = false;
                      submit.setEnabled(true);
                      change.setEnabled(true);
                      status.setText(e.getMessage());
                      return;
                    }
                    password.setText("");
                    busy = false;
                    submit.setEnabled(true);
                    change.setEnabled(true);
                    if (signup()) {
                      status.setText(
                          "Conta criada. Confira seu e-mail, se solicitado, e entre com sua"
                              + " senha.");
                    } else {
                      SessionManager.Session s = SessionManager.get(this);
                      if (s != null) SyncScheduler.start(this, s.userId);
                      startActivity(
                          new Intent(this, HubActivity.class)
                              .addFlags(
                                  Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                      finish();
                    }
                  }));
        } catch (Exception e) {
          runOnUiThread(() ->
              ATTEMPTS.complete(
                  request,
                  !isFinishing() && !isDestroyed(),
                  () -> {
                    busy = false;
                    submit.setEnabled(true);
                    change.setEnabled(true);
                    status.setText(
                        e instanceof ApiClient.ApiException
                            ? e.getMessage()
                            : "Não foi possível conectar. Confira sua internet e tente novamente.");
                  }));
        }
      });
    });
  }
}
