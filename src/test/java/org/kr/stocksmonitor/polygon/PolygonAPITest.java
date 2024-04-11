package org.kr.stocksmonitor.polygon;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.Test;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PolygonAPITest {

    private static final Log logger = LogFactory.getLog(PolygonAPITest.class);

    PolygonAPI api = new PolygonAPI();

    @Test
    void loadTickerTypesStocks() {
        try {
            List<TickerType> types = api.loadTickerTypes("stocks");
            assertNotNull(types);
            assertEquals(24, types.size());
        } catch (IOException e) {
            logger.error(e);
            throw new RuntimeException(e);
        }
    }

    @Test
    void loadTickerTypesStocksNoParam() {
        try {
            List<TickerType> types = api.loadTickerTypes();
            assertNotNull(types);
            assertEquals(24, types.size());
        } catch (IOException e) {
            logger.error(e);
            throw new RuntimeException(e);
        }
    }

    @Test
    void loadTickerTypesIndicies() {
        try {
            List<TickerType> types = api.loadTickerTypes("indicies");
            assertNotNull(types);
            assertEquals(0, types.size());
        } catch (IOException e) {
            logger.error(e);
            throw new RuntimeException(e);
        }
    }

    @Test
    void loadTickerTypesOptions() {
        try {
            List<TickerType> types = api.loadTickerTypes("options");
            assertNotNull(types);
            assertEquals(0, types.size());
        } catch (IOException e) {
            logger.error(e);
            throw new RuntimeException(e);
        }
    }

    @Test
    void loadTickerTypesCrypto() {
        try {
            List<TickerType> types = api.loadTickerTypes("crypto");
            assertNotNull(types);
            assertEquals(0, types.size());
        } catch (IOException e) {
            logger.error(e);
            throw new RuntimeException(e);
        }
    }

    @Test
    void loadTickerTypesFX() {
        try {
            List<TickerType> types = api.loadTickerTypes("fx");
            assertNotNull(types);
            assertEquals(0, types.size());
        } catch (IOException e) {
            logger.error(e);
            throw new RuntimeException(e);
        }
    }

    @Test
    void loadAllTickers() {
        try {
            List<TickerType> types = api.loadTickerTypes("stocks");
            for (TickerType t : types) {
                List<Ticker> tickers = api.getTickersInfo(t.getAssetClass(), t.getCode());
                saveTickers(t.getAssetClass() + "_" + t.getCode(), tickers);
            }
        } catch (IOException e) {
            logger.error(e);
        }
    }

    private void saveTickers(String name, List<Ticker> tickers) {

        String csvFilePath = name + ".csv";
        try (CSVPrinter printer = new CSVPrinter(new FileWriter(csvFilePath), CSVFormat.EXCEL.withHeader(
                "Ticker", "Name", "Market", "Locale", "PrimaryExchange", "Type", "Active", "CurrencyName",
                "CIK", "CompositeFigi", "ShareClassFigi", "LastUpdatedUtc"))) {

            for (Ticker ticker : tickers) {
                printer.printRecord(ticker.getTicker(), ticker.getName(), ticker.getMarket(), ticker.getType());
            }

            System.out.println("Tickers saved to " + csvFilePath);
        } catch (IOException e) {
            logger.error(e);

        }
    }

    @Test
    public void testNewsApi() {
        Ticker t = new Ticker("AAPL", "Apple Inc.", "stocks", "FUND");
        LocalDate startDate = LocalDate.now().minusDays(3);
        LocalDate endDate = LocalDate.now();
        try {
            List<NewsArticle> news = api.getTickerNews(t, startDate, endDate);
            assertNotEquals(0, news.size());
        } catch (IOException e) {
            logger.error(e);
            throw new RuntimeException(e);
        }
    }
}