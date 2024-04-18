module org.kr.finmonitor {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires com.dlsc.formsfx;
    requires eu.hansolo.tilesfx;
    requires org.json;
    requires org.apache.commons.csv;
    requires java.net.http;
    requires org.apache.commons.configuration2;
    requires org.apache.logging.log4j;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.annotation;
    requires org.apache.httpcomponents.core5.httpcore5;
    requires org.apache.httpcomponents.client5.httpclient5;
    requires org.apache.commons.codec;
    requires htmlunit;
    requires commons.collections;


    opens org.kr.stocksmonitor to javafx.fxml;
    exports org.kr.stocksmonitor;
    exports org.kr.stocksmonitor.polygon;
    opens org.kr.stocksmonitor.polygon to javafx.fxml;
    exports org.kr.stocksmonitor.config;
    opens org.kr.stocksmonitor.config to javafx.fxml;
}