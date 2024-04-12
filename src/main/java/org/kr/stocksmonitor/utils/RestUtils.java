package org.kr.stocksmonitor.utils;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kr.stocksmonitor.config.ConfigManager;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.kr.stocksmonitor.exceptions.RestCallException;

public class RestUtils {

    private static final Logger logger = LogManager.getLogger(RestUtils.class);
    private static final LinkedList<Long> lastXCalls = new LinkedList<>();

    private static RestUtils instance = new RestUtils();
    public static RestUtils getInstance() {
        return instance;
    }

    private ConfigManager config = ConfigManager.getInstance();
    private final int maxCallsPerMinute = config.readPolygonMaxCallsPerMinute();

    private RestUtils() {

    }

    public String callEndpoint(String host, String endpoint, String apiKey, List<NameValuePair> parameters) throws RestCallException {
        Instant start = Instant.now();
        enforceRateLimit();
        String result = "";
        HttpGet request = new HttpGet(host + endpoint);
        URIBuilder builder = new URIBuilder(request.getURI()).addParameters(parameters);
        String url = "";
        if (logger.isDebugEnabled())
            url = builder.toString();
        logger.debug("calling endpoint: {}", url);
        builder.addParameter("apiKey", apiKey);
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            request.setURI(builder.build());
            try (CloseableHttpResponse httpResponse = client.execute(request)) {
                HttpEntity entity = httpResponse.getEntity();
                if (entity != null) {
                    result = EntityUtils.toString(entity);
                }
            }
        } catch (IOException | URISyntaxException e) {
            throw new RestCallException(e);
        }

        LogUtils.debugDuration(logger, start, String.format("calling endpoint '%s'", url));
        return result;
    }

    private void enforceRateLimit() {
        logger.debug("enforcing rate limit...");
        Instant start = Instant.now();
        synchronized (lastXCalls) {
            lastXCalls.offer(start.toEpochMilli());
            while (lastXCalls.size() >= maxCallsPerMinute) {
                long firstCall = lastXCalls.pollFirst();
                final long timeSinceFirstCall = System.currentTimeMillis() - firstCall;
                if (timeSinceFirstCall <= 60_000) {
                    try {
                        long sleep_time = 60_050 - timeSinceFirstCall;
                        logger.debug("enforcing rate limit, sleeping for: {}", sleep_time);
                        Thread.sleep(sleep_time);
                    } catch (InterruptedException e) {
                        logger.error(e);
                    }
                }
            }
        }
        LogUtils.debugDuration(logger, start, "enforceRateLimit()");
    }

}
