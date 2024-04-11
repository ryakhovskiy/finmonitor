package org.kr.stocksmonitor.polygon;

public enum AssetClass {

    STOCKS("stocks"),
    OPTIONS("options"),
    CRYPTO("crypto"),
    FX("fx"),
    INDICIES("indicies");


    private final String text;

    AssetClass(final String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
