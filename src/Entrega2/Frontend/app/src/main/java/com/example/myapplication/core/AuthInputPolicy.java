package com.example.myapplication.core;

public final class AuthInputPolicy {
  private AuthInputPolicy() {}

  public static boolean validSignupPassword(String password) {
    return password != null && password.length() >= 8;
  }
}
