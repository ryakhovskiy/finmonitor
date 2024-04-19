package org.kr.stocksmonitor.yahoo;

import org.apache.commons.collections.map.LRUMap;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.kr.stocksmonitor.polygon.Ticker;
import org.kr.stocksmonitor.utils.RestUtils2;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.quotes.stock.StockDividend;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class YahooAPI {

    private static final Logger log = LogManager.getLogger(YahooAPI.class);
    private static final YahooAPI instance = new YahooAPI();
    private static final String SEARCH_QUOTES_BASE_URL = "https://query1.finance.yahoo.com/v1/finance/search";
    private static final NameValuePair[] SEARCH_QUOTES_CONFIG_PARAMS = {
            new BasicNameValuePair("lang", "de-de"),
            new BasicNameValuePair("region", "DE"),
            new BasicNameValuePair("quotesCount", "10"),
            new BasicNameValuePair("newsCount", "0"),
            new BasicNameValuePair("listsCount", "0"),
            new BasicNameValuePair("enableFuzzyQuery", "false"),
            new BasicNameValuePair("quotesQueryId", "tss_match_phrase_query"),
            new BasicNameValuePair("multiQuoteQueryId", "multi_quote_single_token_query"),
            new BasicNameValuePair("newsQueryId", "news_cie_vespa"),
            new BasicNameValuePair("enableCb", "false"),
            new BasicNameValuePair("enableNavLinks", "false"),
            new BasicNameValuePair("enableEnhancedTrivialQuery", "true"),
            new BasicNameValuePair("enableResearchReports", "false"),
            new BasicNameValuePair("enableCulturalAssets", "false"),
            new BasicNameValuePair("enableLogoUrl", "false"),
            new BasicNameValuePair("", "")
    };
    private final LRUMap cache = new LRUMap(10000, 0.75f);

    private final CrumbManager crumbManager;

    public static YahooAPI getInstance() {
        return instance;
    }

    private YahooAPI() {
        crumbManager = CrumbManager.getInstance();
    }

    public StockDividend getStockDividend(Ticker ticker) throws IOException {
        Stock stock = YahooFinance.get(ticker.getTicker());
        return stock.getDividend();
    }

    public List<QuoteItem> getQuotes(String name) throws IOException {
        var entry = (List<QuoteItem>)cache.get(name);
        if (null != entry)
            return entry;
        final NameValuePair[] parameters = Arrays.copyOf(SEARCH_QUOTES_CONFIG_PARAMS, SEARCH_QUOTES_CONFIG_PARAMS.length);
        parameters[parameters.length - 1] = new BasicNameValuePair("q", name);
        String response = RestUtils2.getInstance().runQuery(SEARCH_QUOTES_BASE_URL, parameters);
        final JSONObject obj = new JSONObject(response);
        if (!obj.has("quotes")) {
            log.error("Response does not contain the 'quotes' object: {}", response);
            return Collections.emptyList();
        }
        final JSONArray array = obj.getJSONArray("quotes");
        final List<QuoteItem> quotes = new ArrayList<>(array.length());
        for (int i = 0; i < array.length(); i++) {
            JSONObject result = array.getJSONObject(i);
            QuoteItem item = new QuoteItem(result);
            quotes.add(item);
        }
        cache.putIfAbsent(name, quotes);
        return quotes;
    }
}
