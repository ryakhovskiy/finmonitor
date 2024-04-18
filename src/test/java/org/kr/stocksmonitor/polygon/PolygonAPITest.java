package org.kr.stocksmonitor.polygon;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.kr.stocksmonitor.exceptions.RestCallException;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PolygonAPITest {

    private static final Logger logger = LogManager.getLogger(PolygonAPITest.class);

    private final PolygonAPI api = new PolygonAPI();

    @Test
    void testLoadTickerTypesStocks() {
        logger.info("testLoadTickerTypesStocks");
        try {
            List<TickerType> types = api.loadTickerTypes("stocks");
            assertNotNull(types);
            assertEquals(24, types.size());
        } catch (RestCallException e) {
            logger.error(e);
            throw new RuntimeException(e);
        }
    }

    @Test
    void testLoadTickerTypesIndicies() {
        logger.info("testLoadTickerTypesIndicies");
        try {
            List<TickerType> types = api.loadTickerTypes("indicies");
            assertNotNull(types);
            assertEquals(0, types.size());
        } catch (RestCallException e) {
            logger.error(e);
            throw new RuntimeException(e);
        }
    }

    @Test
    void testLoadTickerTypesOptions() {
        logger.info("testLoadTickerTypesOptions");
        try {
            List<TickerType> types = api.loadTickerTypes("options");
            assertNotNull(types);
            assertEquals(0, types.size());
        } catch (RestCallException e) {
            logger.error(e);
            throw new RuntimeException(e);
        }
    }

    @Test
    void testLoadTickerTypesCrypto() {
        logger.info("testLoadTickerTypesCrypto");
        try {
            List<TickerType> types = api.loadTickerTypes("crypto");
            assertNotNull(types);
            assertEquals(0, types.size());
        } catch (RestCallException e) {
            logger.error(e);
            throw new RuntimeException(e);
        }
    }

    @Test
    void testLoadTickerTypesFX() {
        logger.info("testLoadTickerTypesFX");
        try {
            List<TickerType> types = api.loadTickerTypes("fx");
            assertNotNull(types);
            assertEquals(0, types.size());
        } catch (RestCallException e) {
            logger.error(e);
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testNewsApi() {
        Ticker t = new Ticker("AAPL", "Apple Inc.", "stocks", "FUND");
        LocalDate startDate = LocalDate.now().minusMonths(3);
        LocalDate endDate = LocalDate.now();
        try {
            List<NewsArticle> news = api.getTickerNews(t, startDate, endDate);

            //hopefully there were some news about Apple in the last 3 months...
            assertNotEquals(0, news.size());
        } catch (RestCallException e) {
            logger.error(e);
            throw new RuntimeException(e);
        }
    }

//    @Test
//    public void testMalformedParameter() {
//        final String malformedParameterKey = "published_utc.gte=";
//        final List<NameValuePair > parameters = Arrays.asList(
//                new BasicNameValuePair(malformedParameterKey, "2024-04-12"),
//                new BasicNameValuePair("published_utc.lte", "2024-04-12"),
//                new BasicNameValuePair("ticker", "APPL"),
//                new BasicNameValuePair("limit", "1000"),
//                new BasicNameValuePair("sort", "published_utc")
//        );
//
//        assertThrows(RestCallException.class, () -> {
//            api.getTickerNews(parameters);
//        });
//    }
}