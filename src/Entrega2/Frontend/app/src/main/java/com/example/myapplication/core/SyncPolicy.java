package com.example.myapplication.core;

public final class SyncPolicy {
  private SyncPolicy() {}

  public enum Action {
    CONFIRM,
    RETRY,
    LOGIN,
    REJECT
  }

  public static Action forStatus(int status) {
    if (status >= 200 && status < 300) return Action.CONFIRM;
    if (status == 401) return Action.LOGIN;
    if (status == 0 || status == 408 || status == 429 || status >= 500) return Action.RETRY;
    return Action.REJECT;
  }
}
