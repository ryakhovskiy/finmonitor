package org.kr.stocksmonitor.polygon;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.kr.stocksmonitor.config.ConfigManager;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class PolygonAPI {
    private static final Log logger = LogFactory.getLog(PolygonAPI.class);

    private static final String baseURL = "https://api.polygon.io";
    private static final String tickerTypeGET = "/v3/reference/tickers/types";
    private static final String tickersGET = "/v3/reference/tickers";

    private static final String tickerNewsGET = "/v2/reference/news";

    private static final LinkedList<Long> lastFileCalls = new LinkedList<>();

    private Instant lastCallTime = Instant.now();
    private Instant callTime = Instant.now();
    private String apiKey;

    private ConfigManager config = ConfigManager.getInstance();

    public PolygonAPI() {
        this.apiKey = config.readApiKey();
    }

    public List<TickerType> loadTickerTypes() throws IOException {
        return loadTickerTypes(AssetClass.STOCKS.toString());
    }

    public List<TickerType> loadTickerTypes(String assetClass) throws IOException {
        final String uri = baseURL + tickerTypeGET + "?apiKey=" + apiKey + "&asset_class=" + assetClass;
        URL url = new URL(uri);
        enforceRateLimit();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
            final StringBuilder content = new StringBuilder();
            String inputLine;

            while ((inputLine = reader.readLine()) != null)
                content.append(inputLine);

            return parseJsonResponseTickerTypes(content.toString());
        }
    }

    public Map<String, Map<String, List<Ticker>>> loadAllTickers(ProgressCallback callback) throws IOException {
        List<Ticker> tickers = getTickersInfo(callback);
        return tickers.stream()
                .collect(Collectors.groupingBy(Ticker::getMarket,
                        Collectors.groupingBy(Ticker::getType)));

    }

    public List<Ticker> getTickersInfo(ProgressCallback callback) throws IOException {
        String endpointUrl = baseURL + tickersGET + "?apiKey=" + apiKey + "&limit=1000";
        return getTickersInfo(endpointUrl, callback);
    }

    public List<Ticker> getTickersInfo(String assetClass, String tickerType) throws IOException {
        return getTickersInfo(assetClass, tickerType, "");
    }

    public List<Ticker> getTickersInfo(String assetClass, String tickerType, String name) throws IOException {
        String endpointUrl = baseURL + tickersGET + "?apiKey=" + apiKey + "&limit=1000" + "&market=" + assetClass +
                "&type=" + tickerType + "&sort=ticker&ticker.gte=" + name;
        ProgressCallback callback = _ -> { };
        return getTickersInfo(endpointUrl, callback);
    }

    private List<Ticker> getTickersInfo(String endpointUrl, ProgressCallback callback) throws IOException {
        callTime = Instant.now();
        List<Ticker> tickers = new ArrayList<>();
        getTickersInfo(endpointUrl, tickers, callback);
        return tickers;
    }

    private void getTickersInfo(String endpointUrl, List<Ticker> tickers, ProgressCallback callback) throws IOException {
        while (endpointUrl != null && !endpointUrl.isEmpty()) {
            enforceRateLimit();
            logger.trace("calling endpoint to read tickers info");
            URL url = new URL(appendApiKey(endpointUrl));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            try (BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = rd.readLine()) != null) {
                    response.append(line);
                }

                JSONObject jsonResponse = new JSONObject(response.toString());
                JSONArray results = jsonResponse.getJSONArray("results");
                String status = jsonResponse.getString("status");
                if (!status.equals("OK")) throw new IOException("Status is not OK");
                int count = jsonResponse.optInt("count", results.length());
                if (count != results.length()) throw new IOException("Wrong results count");
                endpointUrl = jsonResponse.optString("next_url");

                // Process results
                for (int i = 0; i < results.length(); i++) {
                    JSONObject result = results.getJSONObject(i);
                    Ticker ticker = new Ticker(result);
                    tickers.add(ticker);
                }

                callback.onProgressUpdate(formatProgressMessage(tickers.size()));
            }
        }
    }

    public List<NewsArticle> getTickerNews(Ticker ticker, LocalDate startDate, LocalDate endDate) throws IOException {
        Instant methodStart = Instant.now();
        //DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String start = startDate.format(formatter);
        String end = endDate.format(formatter);
        String endpointUrl = baseURL + tickerNewsGET + "?apiKey=" + apiKey + "&limit=1000" +
                "&ticker=" + ticker.ticker + "&published_utc.gte=" + start + "&published_utc.lte=" + end +
                "&sort=published_utc";

        List<NewsArticle> news = new ArrayList<>();
        while (endpointUrl != null && !endpointUrl.isEmpty()) {
            enforceRateLimit();
            logger.trace("calling endpoint to read ticker news: " + ticker);
            URL url = new URL(appendApiKey(endpointUrl));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            try (BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = rd.readLine()) != null) {
                    response.append(line);
                }

                JSONObject jsonResponse = new JSONObject(response.toString());
                JSONArray results = jsonResponse.getJSONArray("results");
                String status = jsonResponse.getString("status");
                if (!status.equals("OK")) throw new IOException("Status is not OK");
                int count = jsonResponse.optInt("count", results.length());
                if (count != results.length()) throw new IOException("Wrong results count");
                endpointUrl = jsonResponse.optString("next_url");

                // Process results
                for (int i = 0; i < results.length(); i++) {
                    JSONObject result = results.getJSONObject(i);
                    news.add(new NewsArticle(result));
                }
            }
        }
        Duration methodDuration = Duration.between(methodStart, Instant.now());
        logger.debug(String.format("getTickerNews() took %d ms", methodDuration.toMillis()));
        return news;
    }



    private String appendApiKey(String url) {
        if (!url.contains("apiKey=")) {
            if (url.contains("?")) {
                return url + "&apiKey=" + apiKey;
            } else {
                return url + "?apiKey=" + apiKey;
            }
        }
        return url;
    }

    private List<TickerType> parseJsonResponseTickerTypes(String response) {
        JSONObject json = new JSONObject(response);
        String status = json.getString("status");
        if (!"OK".equals(status))
            return new ArrayList<>();

        if (json.isNull("results")) return new ArrayList<>();
        JSONArray results = json.getJSONArray("results");
        List<TickerType> tickerTypes = new ArrayList<>(results.length());
        for (int i = 0; i < results.length(); i++) {
            JSONObject result = results.getJSONObject(i);
            String code = result.getString("code");
            String description = result.getString("description");
            String assetClass = result.getString("asset_class");
            String locale = result.getString("locale");

            tickerTypes.add(new TickerType(code, description, assetClass, locale));
        }

        return tickerTypes;
    }

    private void enforceRateLimit() {
        logger.debug("enforcing rate limit...");
        lastCallTime = Instant.now();
        lastFileCalls.offer(lastCallTime.toEpochMilli());
        while (lastFileCalls.size() >= 5) {
            final long timeSinceFirstCall = System.currentTimeMillis() - lastFileCalls.pollFirst();
            if (timeSinceFirstCall <= 60_000) {
                try {
                    long sleep_time = 60_050 - timeSinceFirstCall;
                    logger.debug("enforcing rate limit, sleeping for: " + sleep_time);
                    Thread.sleep(sleep_time);
                } catch (InterruptedException e) {
                    logger.error(e);
                }
            }
        }
        logger.debug("exit rate limiting...");
    }

    private String formatProgressMessage(int count) {
        Duration elapsedTime = Duration.between(callTime, Instant.now());
        return "Items collected: " + count + ", Elapsed time: " + formatDuration(elapsedTime);
    }

    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.minusHours(hours).toMinutes();
        long seconds = duration.minusHours(hours).minusMinutes(minutes).getSeconds();
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public String getApiKey() {
        return apiKey;
    }

    public void updateApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public interface ProgressCallback {
        void onProgressUpdate(String progress);
    }
}
