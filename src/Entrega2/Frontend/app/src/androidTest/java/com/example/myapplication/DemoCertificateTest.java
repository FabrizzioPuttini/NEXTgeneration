package com.example.myapplication;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.Intent;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.myapplication.network.SessionManager;
import com.example.myapplication.sync.SyncScheduler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DemoCertificateTest {
  private Context context;

  @Before public void session() {
    context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    SessionManager.Session session = new SessionManager.Session();
    session.userId = "11111111-1111-4111-8111-111111111111";
    session.accessToken = "demo-aluno";
    session.refreshToken = "demo-refresh";
    session.expiresAt = System.currentTimeMillis() / 1000 + 3600;
    SessionManager.save(context, session);
  }

  @After public void cleanup() {
    SyncScheduler.stop(context);
    SessionManager.clearToken(context);
  }

  @Test public void demoAvailableWithoutApiAndAfterRecreation() {
    Intent intent = new Intent(context, HubActivity.class).putExtra("section", "certificados");
    try (ActivityScenario<HubActivity> scenario = ActivityScenario.launch(intent)) {
      scenario.onActivity(activity -> {
        android.widget.Button action = activity.findViewById(R.id.primaryAction);
        assertEquals(android.view.View.VISIBLE, action.getVisibility());
        assertEquals("Certificado de demonstração", action.getText().toString());
      });
      scenario.recreate();
      scenario.onActivity(activity -> {
        android.widget.Button action = activity.findViewById(R.id.primaryAction);
        assertEquals(android.view.View.VISIBLE, action.getVisibility());
        assertEquals("Certificado de demonstração", action.getText().toString());
      });
    }
  }
  @Test public void openIntentSharesOnlyBundledPdfWithReadPermission() throws Exception {
    Intent intent = DemoCertificate.openIntent(context);
    assertEquals(Intent.ACTION_VIEW, intent.getAction());
    assertEquals("application/pdf", intent.getType());
    assertEquals("content", intent.getData().getScheme());
    assertEquals(Intent.FLAG_GRANT_READ_URI_PERMISSION, intent.getFlags());
    assertNotNull(intent.getClipData());
    android.content.pm.ProviderInfo provider = context.getPackageManager()
        .resolveContentProvider(context.getPackageName() + ".demo.files", 0);
    assertNotNull(provider);
    assertFalse(provider.exported);
    assertTrue(provider.grantUriPermissions);
    try (java.io.InputStream bundled = context.getAssets().open("certificado-demo.pdf");
         java.io.InputStream shared = context.getContentResolver().openInputStream(intent.getData())) {
      assertArrayEquals(read(bundled), read(shared));
    }
  }

  @Test public void saveCopiesPdfToChosenContentUriAndRejectsFileUri() throws Exception {
    java.io.File directory = new java.io.File(context.getCacheDir(), "demo-certificates");
    directory.mkdirs();
    java.io.File file = new java.io.File(directory, "export-test.pdf");
    android.net.Uri uri = androidx.core.content.FileProvider.getUriForFile(
        context, context.getPackageName() + ".demo.files", file);
    try {
      DemoCertificate.save(context, uri);
      try (java.io.InputStream bundled = context.getAssets().open("certificado-demo.pdf");
           java.io.InputStream saved = new java.io.FileInputStream(file)) {
        assertArrayEquals(read(bundled), read(saved));
      }
      assertThrows(java.io.IOException.class, () -> DemoCertificate.save(context, android.net.Uri.fromFile(file)));
    } finally {
      file.delete();
    }
  }

  @Test public void providerRejectsFilesOutsideDemoDirectory() {
    java.io.File unrelated = new java.io.File(context.getCacheDir(), "private-session.txt");
    assertThrows(IllegalArgumentException.class, () -> androidx.core.content.FileProvider.getUriForFile(
        context, context.getPackageName() + ".demo.files", unrelated));
  }

  private static byte[] read(java.io.InputStream input) throws java.io.IOException {
    assertNotNull(input);
    java.io.ByteArrayOutputStream result = new java.io.ByteArrayOutputStream();
    byte[] buffer = new byte[8192];
    int length;
    while ((length = input.read(buffer)) != -1) result.write(buffer, 0, length);
    return result.toByteArray();
  }

}
