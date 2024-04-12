package org.kr.stocksmonitor.utils;


import java.time.Duration;
import java.time.Instant;

public class LogUtils {

    public static void debugDuration(org.apache.logging.log4j.Logger logger, Instant start, String action) {
        logger.debug("{} took {} ms", action, Duration.between(start, Instant.now()).toMillis());
    }

}
