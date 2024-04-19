package org.kr.stocksmonitor.config;

import org.junit.jupiter.api.Test;
import org.kr.stocksmonitor.yahoo.QuoteItem;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConfigManagerTest {

    @Test
    void testFavoriteQuotes() {

        //symbol_ACN = {"symbol":"ACN","longname":"Accenture plc","typeDisp":"Aktie","exchDisp":"NYSE","index":"quotes","exchange":"NYQ","isYahooFinance":true,"shortname":"Accenture plc","quoteType":"EQUITY"}

        final ConfigManager configManager = ConfigManager.getInstance();
        final List<QuoteItem> quotes = List.of(
                new QuoteItem("ex.1", "test short", "ETF", "X1", "ind", "disp", "long", "exch", false),
                new QuoteItem("ex2", "test short2", "ETF", "X2", "ind", "disp", "long", "exch", false),
                new QuoteItem("ex3", "test short3", "ETF", "X3", "ind", "disp", "long", "exch", false),
                new QuoteItem("ex4", "test short4", "ETF", "X4", "ind", "disp", "long", "exch", false)
        );

        configManager.saveFavoriteQuotes(quotes);
        List<QuoteItem> configQuotes = configManager.readFavoriteQuotes();
        assertFalse(configQuotes.isEmpty());
        assertTrue(quotes.containsAll(configQuotes));

        configManager.deleteFavoriteQuotes(quotes);
        configQuotes = configManager.readFavoriteQuotes();
        for (QuoteItem q : quotes)
            assertFalse(configQuotes.contains(q));
    }
}