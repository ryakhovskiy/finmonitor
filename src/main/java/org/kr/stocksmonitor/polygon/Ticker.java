package org.kr.stocksmonitor.polygon;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class Ticker implements Serializable {
    private static final Log logger = LogFactory.getLog(Ticker.class);

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Path TICKERS_FILE = Paths.get(System.getProperty("user.dir"), "tickers.dat");

    final String ticker;
    final String name;
    final String market;
    final String type;

    public Ticker(String ticker, String name, String market, String type) {
        this.ticker = ticker;
        this.name = name;
        this.market = market;
        this.type = type;
    }

    public Ticker(JSONObject json) {
        this.ticker = json.optString("ticker", "");
        this.name = json.optString("name", "");
        this.market = json.optString("market", "");
        this.type = json.optString("type", "");
    }

    @Override
    public String toString() {
        return "Stock{" +
                "ticker='" + ticker + '\'' +
                ", name='" + name + '\'' +
                ", market='" + market + '\'' +
                ", type='" + type + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ticker other = (Ticker) o;
        return other.getTicker().equals(ticker);
    }

    @Override
    public int hashCode() {
        return ticker.hashCode();
    }

    public String getMarket() {
        return market;
    }

    public String getType() {
        return type;
    }

    public String getTicker() { return ticker; }
    public String getName() { return name; }

    public static void saveToFile(Map<String, Map<String, List<Ticker>>> tickerMap) throws IOException {
        logger.debug("saving tickers to the file: " + TICKERS_FILE);
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(TICKERS_FILE.toFile()))) {
            oos.writeObject(tickerMap);
        }
    }

    public static Map<String, Map<String, List<Ticker>>> readFromFile() throws IOException, ClassNotFoundException {
        if (!Files.exists(TICKERS_FILE))
            throw new FileNotFoundException("Ticker file does not exists: " + TICKERS_FILE.toFile().getAbsoluteFile());
        logger.debug("loading tickers from the file: " + TICKERS_FILE);
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(TICKERS_FILE.toFile()))) {
            return (Map<String, Map<String, List<Ticker>>>) ois.readObject();
        }
    }
}
