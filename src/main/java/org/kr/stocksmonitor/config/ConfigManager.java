package org.kr.stocksmonitor.config;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.INIConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kr.stocksmonitor.polygon.Ticker;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ConfigManager {

    private static final ConfigManager instance = new ConfigManager();

    public static ConfigManager getInstance() {
        return instance;
    }

    private static final Log logger = LogFactory.getLog(ConfigManager.class);
    private static final Path CONFIG_FILE_PATH = Paths.get(System.getProperty("user.home"), ".finmonconfig");

    private ConfigManager() {

    }

    public String readApiKey() {
        logger.debug("loading the api key from the file: " + CONFIG_FILE_PATH);
        FileBasedConfigurationBuilder<FileBasedConfiguration> builder = getConfigBuilder();
        try {
            Configuration config = builder.getConfiguration();
            return config.getString("polygon.api_key");
        } catch (ConfigurationException e) {
            logger.error(e);
        }
        return "";
    }

    public void saveApiKey(String apiKey) {
        logger.debug("saving the new api key to the file: " + CONFIG_FILE_PATH);
        FileBasedConfigurationBuilder<FileBasedConfiguration> builder = getConfigBuilder();
        try {
            FileBasedConfiguration config = builder.getConfiguration();
            config.setProperty("polygon.api_key", apiKey);
            try (Writer writer = new FileWriter(CONFIG_FILE_PATH.toFile())) {
                config.write(writer);
            }
        } catch (ConfigurationException | IOException e) {
            logger.error(e);
        }
    }

    public List<String> readFavoriteTickerSymbols() {
        logger.debug("loading favorite ticker symbols from the config file...");
        List<String> symbols = new ArrayList<>();

        FileBasedConfigurationBuilder<FileBasedConfiguration> builder = getConfigBuilder();

        try {
            Configuration config = builder.getConfiguration();
            Iterator<String> keysIterator = config.getKeys("favorite_tickers");
            while (keysIterator.hasNext()) {
                String key = keysIterator.next();
                String tickerSymbol = config.getProperty(key).toString();
                symbols.add(tickerSymbol);
            }
        } catch (ConfigurationException e) {
            logger.error(e);
        }
        logger.debug(String.format("%d ticker symbols loaded from the config file", symbols.size()));
        return symbols;
    }

    public void saveFavoriteTickers(List<Ticker> tickers) {
        if (null == tickers || tickers.isEmpty()) return;
        logger.debug("saving favorite ticker symbols to the config file, total symbols: " + tickers.size());

        List<String> favoritesFromForm = tickers.stream().map(Ticker::getTicker).sorted().toList();

        FileBasedConfigurationBuilder<FileBasedConfiguration> builder = getConfigBuilder();
        boolean changed = false;
        try {
            FileBasedConfiguration config = builder.getConfiguration();
            // Get the keys in the favorite_tickers section and remove ones, that are not present on the app form
            Iterator<String> keys = config.getKeys("favorite_tickers");
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
                    config.setProperty("favorite_tickers.symbol_" + favorite, favorite);
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
    }

    private FileBasedConfigurationBuilder<FileBasedConfiguration> getConfigBuilder() {
        Parameters params = new Parameters();
        return new FileBasedConfigurationBuilder<FileBasedConfiguration>(INIConfiguration.class)
                        .configure(params.fileBased().setFile(CONFIG_FILE_PATH.toFile()));
    }
}
