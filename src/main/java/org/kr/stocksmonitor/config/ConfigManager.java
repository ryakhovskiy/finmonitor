package org.kr.stocksmonitor.config;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.INIConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kr.stocksmonitor.polygon.Ticker;
import org.kr.stocksmonitor.utils.LogUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;

public class ConfigManager {

    private static final Logger logger = LogManager.getLogger(ConfigManager.class);
    private static final Path CONFIG_FILE_PATH = Paths.get(System.getProperty("user.home"), ".stocksmonitor.config");
    private static final String POLYGON_SECTION = "polygon";
    private static final String API_KEY = "api_key";
    private static final String POLYGON_API_KEY = String.format("%s.%s", POLYGON_SECTION, API_KEY);
    private static final String MAX_CALLS_PER_MINUTE = "max_calls_per_minute";
    private static final String POLYGON_MAX_CALLS_PER_MINUTE = String.format("%s.%s", POLYGON_SECTION, MAX_CALLS_PER_MINUTE);
    private static final String FAVORITE_TICKERS_SECTION = "favorite_tickers";
    private static final String SYMBOL_PREFIX = "symbol_";
    private static final String FAVORITE_TICKERS_SYMBOL_PREFIX = String.format("%s.%s", FAVORITE_TICKERS_SECTION, SYMBOL_PREFIX);

    private static final ConfigManager instance = new ConfigManager();

    public static ConfigManager getInstance() {
        return instance;
    }

    private ConfigManager() {
        logger.debug("ConfigManager instantiated with the file: {}", CONFIG_FILE_PATH);
    }

    public String readPolygonApiKey() {
        logger.debug("loading the polygon api key from the file: {}, property: {}", CONFIG_FILE_PATH, POLYGON_API_KEY);
        FileBasedConfigurationBuilder<FileBasedConfiguration> builder = getConfigBuilder();
        try {
            Configuration config = builder.getConfiguration();
            return config.getString(POLYGON_API_KEY);
        } catch (ConfigurationException e) {
            logger.error(e);
        }
        return "";
    }

    public int readPolygonMaxCallsPerMinute() {
        int value = 5; //default is 5 for free API
        logger.debug("loading the polygon rate limiting max_calls_per_minute from the file: {}, property: {}", CONFIG_FILE_PATH, POLYGON_MAX_CALLS_PER_MINUTE);
        FileBasedConfigurationBuilder<FileBasedConfiguration> builder = getConfigBuilder();
        try {
            Configuration config = builder.getConfiguration();
            return config.getInt(POLYGON_MAX_CALLS_PER_MINUTE);
        } catch (ConfigurationException | NoSuchElementException e) {
            logger.error(e);
        }
        return value;
    }

    public void saveApiKey(String apiKey) {
        logger.debug("saving the new api key to the file: {}", CONFIG_FILE_PATH);
        FileBasedConfigurationBuilder<FileBasedConfiguration> builder = getConfigBuilder();
        try {
            FileBasedConfiguration config = builder.getConfiguration();
            config.setProperty(POLYGON_API_KEY, apiKey);
            try (Writer writer = new FileWriter(CONFIG_FILE_PATH.toFile())) {
                config.write(writer);
            }
        } catch (ConfigurationException | IOException e) {
            logger.error(e);
        }
    }

    public List<String> readFavoriteTickerSymbols() {
        logger.debug("loading favorite ticker symbols from the config file, property {}", FAVORITE_TICKERS_SECTION);
        List<String> symbols = new ArrayList<>();

        FileBasedConfigurationBuilder<FileBasedConfiguration> builder = getConfigBuilder();

        try {
            Configuration config = builder.getConfiguration();
            Iterator<String> keysIterator = config.getKeys(FAVORITE_TICKERS_SECTION);
            while (keysIterator.hasNext()) {
                String key = keysIterator.next();
                String tickerSymbol = config.getProperty(key).toString();
                symbols.add(tickerSymbol);
            }
        } catch (ConfigurationException e) {
            logger.error(e);
        }
        logger.debug("{} ticker symbols loaded from the config file", symbols.size());
        return symbols;
    }

    public void saveFavoriteTickers(List<Ticker> tickers) {
        if (null == tickers || tickers.isEmpty()) return;
        Instant start = Instant.now();
        logger.debug("saving favorite ticker symbols to the config file, total symbols: {}", tickers.size());

        List<String> favoritesFromForm = tickers.stream().map(Ticker::getTicker).sorted().toList();

        FileBasedConfigurationBuilder<FileBasedConfiguration> builder = getConfigBuilder();
        boolean changed = false;
        try {
            FileBasedConfiguration config = builder.getConfiguration();
            // Get the keys in the favorite_tickers section and remove ones, that are not present on the app form
            Iterator<String> keys = config.getKeys(FAVORITE_TICKERS_SECTION);
            Map<String, String> currentConfigFavorites = new HashMap<>();
            while (keys.hasNext()) {
                String key = keys.next();
                String value = config.getString(key);
                // if the favorites from the table on the form does not have this ticker symbol - kill it
                if (!favoritesFromForm.contains(value)) {
                    config.clearProperty(key);
                    changed = true;
                }
                else {
                    currentConfigFavorites.put(key, value);
                }
            }

            //Now add tickers, that are not in config
            for (String favorite : favoritesFromForm) {
                if (!currentConfigFavorites.containsValue(favorite)) {
                    config.setProperty(FAVORITE_TICKERS_SYMBOL_PREFIX + favorite, favorite);
                    changed = true;
                }
            }

            //if no changes made - do not touch the file
            if (!changed) return;
            try (Writer writer = new FileWriter(CONFIG_FILE_PATH.toFile())) {
                config.write(writer);
            }
        } catch (ConfigurationException | IOException e) {
            logger.error(e);
        }
        LogUtils.debugDuration(logger, start, "saving the favorite tickers into the config");
    }

    private FileBasedConfigurationBuilder<FileBasedConfiguration> getConfigBuilder() {
        Parameters params = new Parameters();
        return new FileBasedConfigurationBuilder<FileBasedConfiguration>(INIConfiguration.class)
                        .configure(params.fileBased().setFile(CONFIG_FILE_PATH.toFile()));
    }
}
