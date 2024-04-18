package yahoofinance.quotes.query1v7;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import yahoofinance.Utils;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.util.RedirectableRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class QuotesRequest<T> {

    private static final Logger log = LogManager.getLogger(QuotesRequest.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    protected final String symbols;

    public QuotesRequest(String symbols) {
        this.symbols = symbols;
    }

    public String getSymbols() {
        return symbols;
    }

    protected abstract T parseJson(JsonNode node);

    public T getSingleResult() throws IOException {
        List<T> results = this.getResult();
        if (results.size() > 0) {
            return results.get(0);
        }
        return null;
    }

    public List<T> getResult() throws IOException {
        final String url = YahooFinance.QUOTES_QUERY1V7_BASE_URL;
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            ClassicHttpRequest httpGet = ClassicRequestBuilder
                    .get(url)
                    .addParameter("symbols", this.symbols)
                    .build();

            org.kr.stocksmonitor.yahoo.CrumbManager.getInstance().prepareRequest(httpGet);
            return client.execute(httpGet, classicHttpResponse -> {
                try (InputStreamReader is = new InputStreamReader(classicHttpResponse.getEntity().getContent())) {
                    List<T> result = new ArrayList<>();
                    JsonNode node = objectMapper.readTree(is);
                    if(node.has("quoteResponse") && node.get("quoteResponse").has("result")) {
                        node = node.get("quoteResponse").get("result");
                        for(int i = 0; i < node.size(); i++) {
                            result.add(this.parseJson(node.get(i)));
                        }
                    } else {
                        throw new IOException("Invalid response: " + node.asText());
                    }
                    return result;
                }
            });
        }
    }

    /**
     * Sends the request to Yahoo Finance and parses the result
     *
     * @return List of parsed objects resulting from the Yahoo Finance request
     * @throws IOException when there's a connection problem or the request is incorrect
     */
    private List<T> getOldWay() throws IOException {
        List<T> result = new ArrayList<T>();

        Map<String, String> params = new LinkedHashMap<>();
        params.put("symbols", this.symbols);

        String url = YahooFinance.QUOTES_QUERY1V7_BASE_URL + "?" + Utils.getURLParameters(params);

        // Get JSON from Yahoo
        log.info("Sending request: " + url);

        URL request = new URL(url);
        RedirectableRequest redirectableRequest = new RedirectableRequest(request, 5);
        redirectableRequest.setConnectTimeout(YahooFinance.CONNECTION_TIMEOUT);
        redirectableRequest.setReadTimeout(YahooFinance.CONNECTION_TIMEOUT);
        URLConnection connection = redirectableRequest.openConnection();

        InputStreamReader is = new InputStreamReader(connection.getInputStream());
        JsonNode node = objectMapper.readTree(is);
        if(node.has("quoteResponse") && node.get("quoteResponse").has("result")) {
            node = node.get("quoteResponse").get("result");
            for(int i = 0; i < node.size(); i++) {
                result.add(this.parseJson(node.get(i)));
            }
        } else {
            throw new IOException("Invalid response: " + node.asText());
        }

        return result;
    }

}
