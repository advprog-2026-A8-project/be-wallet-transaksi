package id.ac.ui.cs.advprog.bewallettransaksi.controller;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryIdempotencyKeyGuard implements IdempotencyKeyGuard {
    private final Set<String> processedKeys = ConcurrentHashMap.newKeySet();

    @Override
    public boolean register(String key) {
        return processedKeys.add(key);
    }

    @Override
    public void release(String key) {
        processedKeys.remove(key);
    }
}
