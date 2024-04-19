package org.kr.stocksmonitor.yahoo;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.htmlunit.BrowserVersion;
import org.htmlunit.Page;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlButton;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.util.Cookie;
import org.junit.jupiter.api.Test;
import org.kr.stocksmonitor.polygon.Ticker;
import yahoofinance.quotes.stock.StockDividend;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;


class YahooAPITest {

    private static final Logger logger = LogManager.getLogger(YahooAPITest.class);

    @Test
    public void testGetQuotes() throws IOException {
        YahooAPI api = YahooAPI.getInstance();
        List<QuoteItem> items = api.getQuotes("SPY");
        assertNotNull(items);
        assertFalse(items.isEmpty());
    }

    @Test
    public void testStockDividend() throws IOException {
        YahooAPI api = YahooAPI.getInstance();
        StockDividend div = api.getStockDividend(new Ticker("AAPL"));
        assertNotNull(div);
    }

    @Test
    public void testHtmlUnit() throws IOException {
        final String url = "https://finance.yahoo.com/quote/AAPL?p=AAPL";
        String crumb = "xxx";
        Map<String, String> cookies = new HashMap<>();
        try (WebClient webClient = new WebClient(BrowserVersion.CHROME)) {
            webClient.getOptions().setJavaScriptEnabled(false);
            webClient.getOptions().setCssEnabled(false);
            HtmlPage page = webClient.getPage(url);

            HtmlButton button = page.getFirstByXPath("//button[@name='agree']");
            if (button != null) {
                button.click();
                logger.debug("button clicked");
                Page response = webClient.getPage("https://query2.finance.yahoo.com/v1/test/getcrumb");
                var wClientCookies = webClient.getCookieManager().getCookies();
                cookies.putAll(wClientCookies.stream().collect(Collectors.toMap(Cookie::getName, Cookie::getValue)));
                crumb = response.getWebResponse().getContentAsString();
                logger.debug(crumb);
                response = webClient.getPage("https://query2.finance.yahoo.com/v7/finance/quote?symbols=AAPL&crumb=" + crumb);
                logger.info(response.getWebResponse().getContentAsString());
            } else {
                logger.error("Button with name 'agree' not found.");
            }
        }

        try (WebClient webClient = new WebClient(BrowserVersion.CHROME)) {
            webClient.getOptions().setJavaScriptEnabled(false);
            webClient.getOptions().setCssEnabled(false);
            for (Map.Entry<String, String> e : cookies.entrySet())
                webClient.getCookieManager().addCookie(new Cookie(".yahoo.com", e.getKey(), e.getValue()));
            Page response = webClient.getPage("https://query2.finance.yahoo.com/v7/finance/quote?symbols=AAPL&crumb=" + crumb);
            logger.info(response.getWebResponse().getContentAsString());
        }
    }
}