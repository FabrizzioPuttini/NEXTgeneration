package com.example.myapplication.core;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;

public class AuthAttemptGateTest {
  @Test
  public void lateOldLoginCannotOverwriteNewAccount() {
    AuthAttemptGate gate = new AuthAttemptGate();
    AtomicReference<String> session = new AtomicReference<>();
    long loginA = gate.begin(), loginB = gate.begin();
    assertTrue(gate.complete(loginB, true, () -> session.set("B")));
    assertFalse(gate.complete(loginA, true, () -> session.set("A")));
    assertEquals("B", session.get());
  }

  @Test
  public void destroyedScreenCannotCommit() {
    AuthAttemptGate gate = new AuthAttemptGate();
    AtomicReference<String> session = new AtomicReference<>();
    long ticket = gate.begin();
    gate.invalidate(ticket);
    assertFalse(gate.complete(ticket, true, () -> session.set("A")));
    assertNull(session.get());
  }

  @Test
  public void oldDestroyCannotInvalidateReplacement() {
    AuthAttemptGate gate = new AuthAttemptGate();
    long old = gate.begin(), replacement = gate.begin();
    gate.invalidate(old);
    assertTrue(gate.complete(replacement, true, () -> {}));
  }

  @Test
  public void finishingScreenAndDuplicateCallbackNeverCommit() {
    AuthAttemptGate gate = new AuthAttemptGate();
    long ticket = gate.begin();
    assertFalse(gate.complete(ticket, false, () -> fail("finishing screen committed")));
    long current = gate.begin();
    assertTrue(gate.complete(current, true, () -> {}));
    assertFalse(gate.complete(current, true, () -> fail("duplicate callback committed")));
  }
}
