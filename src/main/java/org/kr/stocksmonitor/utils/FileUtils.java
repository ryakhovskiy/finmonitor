package org.kr.stocksmonitor.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kr.stocksmonitor.polygon.Ticker;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class FileUtils {

    private static final Path TICKERS_FILE = Paths.get(System.getProperty("user.dir"), "tickers.dat");
    private static final Log logger = LogFactory.getLog(FileUtils.class);

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
