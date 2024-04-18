package yahoofinance.histquotes2;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import yahoofinance.Utils;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;
import yahoofinance.util.RedirectableRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HistQuotes2Request {

    private static final Logger log = LogManager.getLogger(HistQuotes2Request.class);
    private final String symbol;

    private final Calendar from;
    private final Calendar to;

    private final QueryInterval interval;

    public static final Calendar DEFAULT_FROM = Calendar.getInstance();

    static {
        DEFAULT_FROM.add(Calendar.YEAR, -1);
    }
    public static final Calendar DEFAULT_TO = Calendar.getInstance();
    public static final QueryInterval DEFAULT_INTERVAL = QueryInterval.MONTHLY;

    public HistQuotes2Request(String symbol) {
        this(symbol, DEFAULT_INTERVAL);
    }

    public HistQuotes2Request(String symbol, QueryInterval interval) {
        this(symbol, DEFAULT_FROM, DEFAULT_TO, interval);
    }


    public HistQuotes2Request(String symbol, Calendar from, Calendar to) {
        this(symbol, from, to, DEFAULT_INTERVAL);
    }

    public HistQuotes2Request(String symbol, Calendar from, Calendar to, QueryInterval interval) {
        this.symbol = symbol;
        this.from = this.cleanHistCalendar(from);
        this.to = this.cleanHistCalendar(to);
        this.interval = interval;
    }

    public HistQuotes2Request(String symbol, Date from, Date to) {
        this(symbol, from, to, DEFAULT_INTERVAL);
    }

    public HistQuotes2Request(String symbol, Date from, Date to, QueryInterval interval) {
        this(symbol, interval);
        this.from.setTime(from);
        this.to.setTime(to);
        this.cleanHistCalendar(this.from);
        this.cleanHistCalendar(this.to);
    }

    // Constructors to support the old Interval
    public HistQuotes2Request(String symbol, Interval interval) {
        this(symbol, DEFAULT_FROM, DEFAULT_TO, interval);
    }

    public HistQuotes2Request(String symbol, Calendar from, Calendar to, Interval interval) {
        this(symbol, from, to, IntervalMapper.get(interval));
    }

    public HistQuotes2Request(String symbol, Date from, Date to, Interval interval) {
        this(symbol, from, to, IntervalMapper.get(interval));
    }

    /**
     * Put everything smaller than days at 0
     * @param cal calendar to be cleaned
     */
    private Calendar cleanHistCalendar(Calendar cal) {
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.HOUR, 0);
        return cal;
    }

    public List<HistoricalQuote> getResult() throws IOException {
        if(this.from.after(this.to)) {
            log.warn("Unable to retrieve historical quotes. "
                    + "From-date should not be after to-date. From: "
                    + this.from.getTime() + ", to: " + this.to.getTime());
            return Collections.emptyList();
        }

        final String url = YahooFinance.HISTQUOTES2_BASE_URL + this.symbol;
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            ClassicHttpRequest httpGet = ClassicRequestBuilder
                    .get(url)
                    .addParameter("period1", String.valueOf(this.from.getTimeInMillis() / 1000))
                    .addParameter("period2", String.valueOf(this.to.getTimeInMillis() / 1000))
                    .addParameter("interval", this.interval.getTag())
                    .build();

            org.kr.stocksmonitor.yahoo.CrumbManager.getInstance().prepareRequest(httpGet);
            return client.execute(httpGet, classicHttpResponse -> {
                try (InputStreamReader is = new InputStreamReader(classicHttpResponse.getEntity().getContent());
                     BufferedReader br = new BufferedReader(is)) {
                    List<HistoricalQuote> res = new ArrayList<>();
                    br.readLine(); // skip the first line
                    // Parse CSV
                    for (String line = br.readLine(); line != null; line = br.readLine()) {
                        log.debug("Parsing CSV line: {}", Utils.unescape(line));
                        HistoricalQuote quote = this.parseCSVLine(line);
                        res.add(quote);
                    }
                    return res;
                }
            });
        }
    }

    private List<HistoricalQuote> getOldWay() throws IOException {

        List<HistoricalQuote> result = new ArrayList<HistoricalQuote>();
        
        if(this.from.after(this.to)) {
            log.warn("Unable to retrieve historical quotes. "
                    + "From-date should not be after to-date. From: "
                    + this.from.getTime() + ", to: " + this.to.getTime());
            return result;
        }

        Map<String, String> params = new LinkedHashMap<String, String>();
        params.put("period1", String.valueOf(this.from.getTimeInMillis() / 1000));
        params.put("period2", String.valueOf(this.to.getTimeInMillis() / 1000));

        params.put("interval", this.interval.getTag());

        //old crumb manager does not work
        //params.put("crumb", CrumbManager.getCrumb());

        String url = YahooFinance.HISTQUOTES2_BASE_URL + URLEncoder.encode(this.symbol , "UTF-8") + "?" + Utils.getURLParameters(params);

        // Get CSV from Yahoo
        log.info("Sending request: " + url);

        URL request = new URL(url);
        RedirectableRequest redirectableRequest = new RedirectableRequest(request, 5);
        redirectableRequest.setConnectTimeout(YahooFinance.CONNECTION_TIMEOUT);
        redirectableRequest.setReadTimeout(YahooFinance.CONNECTION_TIMEOUT);
        Map<String, String> requestProperties = new HashMap<String, String>();

        //old crumb manager does not work
        //requestProperties.put("Cookie", CrumbManager.getCookie());

        URLConnection connection = redirectableRequest.openConnection(requestProperties);

        InputStreamReader is = new InputStreamReader(connection.getInputStream());
        BufferedReader br = new BufferedReader(is);
        br.readLine(); // skip the first line
        // Parse CSV
        for (String line = br.readLine(); line != null; line = br.readLine()) {

            log.info("Parsing CSV line: " + Utils.unescape(line));
            HistoricalQuote quote = this.parseCSVLine(line);
            result.add(quote);
        }
        return result;
    }

    private HistoricalQuote parseCSVLine(String line) {
        String[] data = line.split(YahooFinance.QUOTES_CSV_DELIMITER);
        return new HistoricalQuote(this.symbol,
                Utils.parseHistDate(data[0]),
                Utils.getBigDecimal(data[1]),
                Utils.getBigDecimal(data[3]),
                Utils.getBigDecimal(data[2]),
                Utils.getBigDecimal(data[4]),
                Utils.getBigDecimal(data[5]),
                Utils.getLong(data[6])
        );
    }

}
