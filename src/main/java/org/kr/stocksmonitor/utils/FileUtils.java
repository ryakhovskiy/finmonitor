package org.kr.stocksmonitor.utils;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kr.stocksmonitor.polygon.Ticker;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Set;

public class FileUtils {

    private static final Path TICKERS_FILE = Paths.get(System.getProperty("user.dir"), "tickers.dat");
    private static final Logger logger = LogManager.getLogger(FileUtils.class);

    public static void saveToFile(Set<Ticker> tickersMap) throws IOException {
        logger.debug("saving tickers to the file: {}, total tickers: {}", TICKERS_FILE, tickersMap.size());
        Instant start = Instant.now();
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(TICKERS_FILE.toFile()))) {
            oos.writeObject(tickersMap);
        }
    }

    public static Set<Ticker> readFromFile() throws IOException, ClassNotFoundException {
        if (!Files.exists(TICKERS_FILE))
            throw new FileNotFoundException("Ticker file does not exists: " + TICKERS_FILE.toFile().getAbsoluteFile());
        logger.debug("loading tickers from the file: {}", TICKERS_FILE);
        Instant start = Instant.now();
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(TICKERS_FILE.toFile()))) {
            Set<Ticker> tickers = (Set<Ticker>) ois.readObject();
            logger.debug("total number of tickers loaded from file: {}", tickers.size());
            LogUtils.debugDuration(logger, start, "loading tickers from file");
            return tickers;
        }
    }

    public void saveTickersToCsv(String filename, List<Ticker> tickers) {
        logger.info("saving tickers to csv: " + filename);
        if (!filename.endsWith(".csv")) filename = filename + ".csv";
        try (CSVPrinter printer = new CSVPrinter(new FileWriter(filename),
                CSVFormat.EXCEL.builder().setHeader(
                        "Ticker", "Name", "Market", "Locale", "PrimaryExchange", "Type", "Active", "CurrencyName",
                        "CIK", "CompositeFigi", "ShareClassFigi", "LastUpdatedUtc").build())) {

            for (Ticker ticker : tickers) {
                printer.printRecord(ticker.getTicker(), ticker.getName(), ticker.getMarket(), ticker.getType());
            }

            logger.info("Tickers saved to {}", filename);
        } catch (IOException e) {
            logger.error(e);
        }
    }
}
