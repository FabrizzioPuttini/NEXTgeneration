package com.example.myapplication;
import android.content.Context;
import androidx.appcompat.app.AppCompatDelegate;
/** Device preference, independent from the signed-in account. */
public final class ThemePreference {
 private ThemePreference() {}
 public static int get(Context context) {
  int mode=context.getSharedPreferences("appearance",Context.MODE_PRIVATE).getInt("nightMode",AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
  return mode==AppCompatDelegate.MODE_NIGHT_NO||mode==AppCompatDelegate.MODE_NIGHT_YES?mode:AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
 }
 public static void set(Context context,int mode) {
  if(mode!=AppCompatDelegate.MODE_NIGHT_NO&&mode!=AppCompatDelegate.MODE_NIGHT_YES&&mode!=AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) throw new IllegalArgumentException("Unsupported theme");
  context.getSharedPreferences("appearance",Context.MODE_PRIVATE).edit().putInt("nightMode",mode).apply();
  AppCompatDelegate.setDefaultNightMode(mode);
 }
}
