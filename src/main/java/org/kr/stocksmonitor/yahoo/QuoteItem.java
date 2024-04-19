package org.kr.stocksmonitor.yahoo;

import org.json.JSONObject;

public class QuoteItem {
    private final String exchange;
    private final String shortname;
    private final String quoteType;
    private final String symbol;
    private final String index;
    private final String typeDisp;
    private final String longname;
    private final String exchDisp;
    private final boolean isYahooFinance;

    public QuoteItem(String jsonString) {
        this(new JSONObject(jsonString));
    }

    public QuoteItem(JSONObject json) {
        this.exchange = json.optString("exchange", "");
        this.shortname = json.optString("shortname", "");
        this.quoteType = json.optString("quoteType", "");;
        this.symbol = json.optString("symbol", "");;
        this.index = json.optString("index", "");;
        this.typeDisp = json.optString("typeDisp", "");;
        this.longname = json.optString("longname", "");;
        this.exchDisp = json.optString("exchDisp", "");;
        this.isYahooFinance = json.optBoolean("isYahooFinance", false);;
    }

    public QuoteItem(String exchange, String shortname, String quoteType, String symbol, String index,
                     String typeDisp, String longname, String exchDisp, boolean isYahooFinance) {
        this.exchange = exchange;
        this.shortname = shortname;
        this.quoteType = quoteType;
        this.symbol = symbol;
        this.index = index;
        this.typeDisp = typeDisp;
        this.longname = longname;
        this.exchDisp = exchDisp;
        this.isYahooFinance = isYahooFinance;
    }

    public String getExchange() {
        return exchange;
    }

    public String getShortname() {
        return shortname;
    }

    public String getQuoteType() {
        return quoteType;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getIndex() {
        return index;
    }

    public String getTypeDisp() {
        return typeDisp;
    }

    public String getLongname() {
        return longname;
    }

    public String getExchDisp() {
        return exchDisp;
    }

    public boolean isYahooFinance() {
        return isYahooFinance;
    }

    @Override
    public String toString() {
        return String.format("{ %s | %s | %s | %s | %s | %s }", symbol, quoteType, shortname, exchange, typeDisp, index);
    }

    public JSONObject toJsonObject() {
        final JSONObject obj = new JSONObject();
        obj.put("exchange", exchange);
        obj.put("shortname", shortname);
        obj.put("quoteType", quoteType);
        obj.put("symbol", symbol);
        obj.put("index", index);
        obj.put("typeDisp", typeDisp);
        obj.put("longname", longname);
        obj.put("exchDisp", exchDisp);
        obj.put("isYahooFinance", isYahooFinance);
        return obj;
    }

    public String toJsonString() {
        return toJsonObject().toString();
    }

    @Override
    public int hashCode() {
        return symbol.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (null == o) return false;
        if (!(o instanceof QuoteItem)) return false;
        final QuoteItem quote = (QuoteItem)o;
        return this.symbol.equals(quote.symbol);
    }
}

