package com.star.share.cache.hotkey;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.scheduling.annotation.Scheduled;

import com.star.share.cache.config.CacheProperties;

public class HotKeyDetector {
    public enum Level {
        NONE, LOW, MEDIUM, HIGH
    }
    private final CacheProperties cacheProperties;
    private final Map<String, int[]> counters = new ConcurrentHashMap<>();
    private final AtomicInteger current = new AtomicInteger(0);
    private final int segments;

    public HotKeyDetector(CacheProperties cacheProperties) {
        this.cacheProperties = cacheProperties;
        int segSeconds = cacheProperties.getHotKey().getSegmentSeconds();
        int windowSeconds = cacheProperties.getHotKey().getWindowSeconds();
        this.segments = Math.max(1, windowSeconds / Math.max(1, segSeconds));
    }

    public void record(String key) {
        int[] arr = counters.computeIfAbsent(key, k -> new int[segments]);
        arr[current.get()]++;
    }

    @Scheduled(fixedRateString = "${cache.hotkey.segment-seconds:10}000")
    public void rotate() {
        int next = (current.get() + 1) % segments;
        current.set(next);

        for (int[] arr : counters.values()) {
            arr[next] = 0;
        }
    }

    public void clear(String key) {
        int[] arr = counters.get(key);
        if (arr != null)
            Arrays.fill(arr, 0);
    }

    public int heat(String key) {
        int[] arr = counters.get(key);
        if (arr == null)
            return 0;
        int sum = 0;
        for (int i : arr) {
            sum += i;
        }
        return sum;
    }

    public Level level(String key) {
        int heat = heat(key);
        if (heat >= cacheProperties.getHotKey().getLevelHigh()) {
            return Level.HIGH;
        } else if (heat >= cacheProperties.getHotKey().getLevelMedium()) {
            return Level.MEDIUM;
        } else if (heat >= cacheProperties.getHotKey().getLevelLow()) {
            return Level.LOW;
        } else {
            return Level.NONE;
        }
    }

    public int ttlForPublic(int baseTtl, String key) {
        Level l = level(key);
        return baseTtl + extendSeconds(l);
    }

    public int ttlForMine(int baseTtlSeconds, String key) {
        Level l = level(key);
        return baseTtlSeconds + extendSeconds(l);
    }

    private int extendSeconds(Level l) {
        return switch (l) {
            case HIGH -> cacheProperties.getHotKey().getExtendHighSeconds();
            case MEDIUM -> cacheProperties.getHotKey().getExtendMediumSeconds();
            case LOW -> cacheProperties.getHotKey().getExtendLowSeconds();
            default -> 0;
        };
    }

}
