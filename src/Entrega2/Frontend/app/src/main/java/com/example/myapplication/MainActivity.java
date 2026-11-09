package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.network.SessionManager;

public final class MainActivity extends AppCompatActivity {
  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    startActivity(
        new Intent(
            this, SessionManager.get(this) == null ? LoginActivity.class : HubActivity.class));
    finish();
  }
}
