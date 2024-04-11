package org.kr.stocksmonitor.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.time.Instant;

public class RestUtils {

    private static final Log logger = LogFactory.getLog(RestUtils.class);

    public String callEndpoint(String endpoint, String apiKey) {
        Instant start = Instant.now();
        logger.debug("calling endpoint: " + endpoint);
        endpoint = appendApiKey(apiKey);

        logger.debug("");
        return "";
    }

    private String appendApiKey(String apiKey) {
        return "";
    }

    private void enforceRateLimiting() {

    }

}
