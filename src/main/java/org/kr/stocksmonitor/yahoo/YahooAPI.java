package org.kr.stocksmonitor.yahoo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kr.stocksmonitor.polygon.Ticker;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.quotes.stock.StockDividend;

import java.io.IOException;
import java.util.List;

public class YahooAPI {

    private static final Logger logger = LogManager.getLogger(YahooAPI.class);
    private static final YahooAPI instance = new YahooAPI();
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

}
