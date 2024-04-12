package org.kr.stocksmonitor.polygon;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Ticker implements Serializable {
    private static final Log logger = LogFactory.getLog(Ticker.class);

    @Serial
    private static final long serialVersionUID = 1L;

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


}
