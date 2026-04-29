package id.ac.ui.cs.advprog.bewallettransaksi.controller;

public interface IdempotencyKeyGuard {
    boolean register(String key);
}
