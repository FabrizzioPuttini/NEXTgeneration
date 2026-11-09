package com.example.myapplication.core;

/** Coordinates asynchronous login callbacks across Activity replacements. */
public final class AuthAttemptGate {
  private long generation;

  public synchronized long begin() {
    return ++generation;
  }

  public synchronized void invalidate(long ticket) {
    if (ticket == generation) generation++;
  }

  public synchronized boolean complete(long ticket, boolean screenAlive, Runnable commit) {
    if (ticket != generation || !screenAlive) return false;
    generation++; // Consume before committing; duplicate callbacks cannot run twice.
    commit.run();
    return true;
  }
}
