package org.kr.stocksmonitor.yahoo;

import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.CookieStore;

import org.apache.hc.client5.http.impl.cookie.BasicClientCookie;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.htmlunit.BrowserVersion;
import org.htmlunit.Page;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlButton;
import org.htmlunit.html.HtmlPage;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

public class CrumbManager {

    private static final String COOKIE_URL = "https://finance.yahoo.com/quote/AAPL";
    private static final String CRUMB_URL = "https://query2.finance.yahoo.com/v1/test/getcrumb";
    private static final String ACCEPT_ALL_COOKIES_BUTTON = "//button[@name='agree']";
    private static final String ACCEPT_HEADER_NAME = "Accept";
    private static final String ACCEPT_HEADER_VALUE = "text/html,application/json";
    private static final String USER_AGENT_HEADER_NAME = "User-Agent";
    private static final String USER_AGENT_HEADER_VALUE = "Mozilla/5.0";
    private static final String COOKIE_HEADER_NAME = "Cookie";
    private static final Logger logger = LogManager.getLogger(CrumbManager.class);
    private static final CrumbManager instance = new CrumbManager();
    public static CrumbManager getInstance() {
        return instance;
    }

    private final CookieStore cookieStore = new BasicCookieStore();
    private final HttpClientContext localContext = HttpClientContext.create();
    private String crumb = "";
    private final boolean instantiated;

    private CrumbManager() {
        instantiated = init();
    }

    private boolean init() {
        try (WebClient webClient = new WebClient(BrowserVersion.CHROME)) {
            webClient.getOptions().setJavaScriptEnabled(false);
            webClient.getOptions().setCssEnabled(false);
            HtmlPage page = webClient.getPage(COOKIE_URL);

            HtmlButton button = page.getFirstByXPath(ACCEPT_ALL_COOKIES_BUTTON);
            if (button != null) {
                button.click();
                Page response = webClient.getPage(CRUMB_URL);
                initCookieStore(webClient.getCookieManager().getCookies());
                crumb = response.getWebResponse().getContentAsString();
                logger.trace("crumb set: {}", crumb);
                return true;
            } else {
                logger.error("Cannot capture crumb and cookies, button 'Accept Cookies' is not found");
                return false;
            }
        } catch (IOException e) {
            logger.error("Cannot capture crumb and cookies: ", e);
            return false;
        }
    }

    private void initCookieStore(Set<org.htmlunit.util.Cookie> cookies) {
        for (var c : cookies) {
            final BasicClientCookie cookie = new BasicClientCookie(c.getName(), c.getValue());
            cookie.setDomain(c.getDomain());
            cookie.setPath(c.getPath());
            if (null != c.getExpires())
                cookie.setExpiryDate(c.getExpires().toInstant());
            cookieStore.addCookie(cookie);
            logger.trace("cookie saved: {}", cookie);
        }
        localContext.setCookieStore(cookieStore);
    }

    protected boolean isInstantiated() {
        return instantiated;
    }

    public void prepareRequest(HttpRequest httpGet) {
        httpGet.addHeader(ACCEPT_HEADER_NAME, ACCEPT_HEADER_VALUE);
        httpGet.addHeader(USER_AGENT_HEADER_NAME, USER_AGENT_HEADER_VALUE);
        for (var c : cookieStore.getCookies()) {
            final String header = String.format("%s=%s; Domain=%s", c.getName(), c.getValue(), c.getDomain());
            httpGet.addHeader(COOKIE_HEADER_NAME, header);
            logger.trace("adding cookie header: {}", header);
        }
        if (instantiated) {
            try {
                URI uri = new URIBuilder(httpGet.getUri())
                        .addParameter("crumb", crumb)
                        .build();
                httpGet.setUri(uri);
                logger.trace("crumb added: {}", uri);
            } catch (URISyntaxException e) {
                logger.error(e);
            }
        } else {
            logger.error("Crumb Manager was not properly instantiated");
        }
    }
}
