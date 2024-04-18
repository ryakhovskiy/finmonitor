package org.kr.stocksmonitor.yahoo;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class CrumbManagerTest {

    private static final Logger log = LogManager.getLogger(CrumbManagerTest.class);

    private static final String AAPL_SYMBOL_URL = "https://query2.finance.yahoo.com/v7/finance/quote?symbols=AAPL";

    @Test
    public void testCrumbManagerInstantiation() {
        assertTrue(CrumbManager.getInstance().isInstantiated());
    }

    @Test
    public void testPrepareRequest() throws IOException {
        CrumbManager crumbManager = CrumbManager.getInstance();
        assertTrue(crumbManager.isInstantiated());

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            ClassicHttpRequest httpGet = ClassicRequestBuilder
                    .get(AAPL_SYMBOL_URL)
                    .build();
            crumbManager.prepareRequest(httpGet);
            client.execute(httpGet, classicHttpResponse -> {
                log.debug("Response: {} - {}", classicHttpResponse.getCode(), classicHttpResponse.getReasonPhrase());
                assertEquals(200, classicHttpResponse.getCode());
                return classicHttpResponse.getReasonPhrase();
            });
        }
    }
}