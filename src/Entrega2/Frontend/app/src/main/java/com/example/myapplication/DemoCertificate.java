package com.example.myapplication;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/** A bundled, anonymous sample; never a student's earned certificate. */
public final class DemoCertificate {
  public static final String FILE_NAME = "certificado-demo.pdf";

  private DemoCertificate() {}

  public static Intent openIntent(Context context) throws IOException {
    File directory = new File(context.getCacheDir(), "demo-certificates");
    if (!directory.isDirectory() && !directory.mkdirs())
      throw new IOException("Não foi possível preparar o PDF de demonstração.");
    File pdf = new File(directory, FILE_NAME);
    try (OutputStream output = new FileOutputStream(pdf)) {
      copyAsset(context, output);
    }
    Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".demo.files", pdf);
    Intent intent = new Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, "application/pdf")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    intent.setClipData(ClipData.newRawUri("Certificado de demonstração", uri));
    return intent;
  }

  public static void save(Context context, Uri destination) throws IOException {
    if (destination == null || !"content".equals(destination.getScheme()))
      throw new IOException("Destino de arquivo inválido.");
    try (OutputStream output = context.getContentResolver().openOutputStream(destination, "wt")) {
      if (output == null) throw new IOException("Não foi possível abrir o destino.");
      copyAsset(context, output);
    }
  }

  private static void copyAsset(Context context, OutputStream output) throws IOException {
    try (InputStream input = context.getAssets().open(FILE_NAME)) {
      byte[] buffer = new byte[8192];
      int length;
      while ((length = input.read(buffer)) != -1) output.write(buffer, 0, length);
    }
  }
}
