package org.kr.stocksmonitor.polygon;

public class TickerType {

    private final String code;
    private final String description;
    private final String assetClass;
    private final String locale;

    public TickerType(String code, String description, String assetClass, String locale) {
        this.code = code;
        this.description = description;
        this.assetClass = assetClass;
        this.locale = locale;
    }

    @Override
    public String toString() {
        return code + ": " + description + " | " + assetClass + " | " + locale;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public String getAssetClass() {
        return assetClass;
    }

    public String getLocale() {
        return locale;
    }
}
