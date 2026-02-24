package com.star.share.cache.config;

import lombok.Data;
@Data
public class CacheProperties {
    private L2 l2 = new L2();
    private HotKey hotKey = new HotKey();

    @Data
    public static class L2 {
        private PublicCfg publicCfg = new PublicCfg();
        private MineCfg mineCfg = new MineCfg();
        private DetailCfg detailCfg = new DetailCfg();
    }

    @Data
    public static class PublicCfg {
        private int ttlSeconds = 15;
        private long maxSize = 1000;
    }

    @Data
    public static class MineCfg {
        private int ttlSeconds = 10;
        private long maxSize = 1000;
    }

    @Data
    public static class DetailCfg {
        private int ttlSeconds = 30;
        private long maxSize = 5000;
    }

    @Data
    public static class HotKey {
        // hot key window size in seconds, used to determine the time range for counting
        // accesses to identify hot keys.
        private int windowSeconds = 60;

        // Number of segments to divide the hot key window into. This allows for more
        // fine-grained tracking of access patterns within the window.
        private int segmentSeconds = 10;

        // Low hotness threshold: if the number of accesses to a key within the window
        // reaches this value, it is considered low hot.
        private int levelLow = 50;

        // Medium hotness threshold: if the number of accesses to a key within the
        // window reaches this value, it is considered medium hot.
        private int levelMedium = 200;

        // High hotness threshold: if the number of accesses to a key within the window
        // reaches this value, it is considered high hot.
        private int levelHigh = 500;

        // Low hotness additional TTL (seconds).
        private int extendLowSeconds = 20;

        // Medium hotness additional TTL (seconds).
        private int extendMediumSeconds = 60;

        // High hotness additional TTL (seconds).
        private int extendHighSeconds = 120;
    }

}
