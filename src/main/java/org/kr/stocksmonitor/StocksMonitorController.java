package org.kr.stocksmonitor;

import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.net.*;

import javafx.util.StringConverter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kr.stocksmonitor.config.ConfigManager;
import org.kr.stocksmonitor.utils.LogUtils;
import org.kr.stocksmonitor.yahoo.QuoteItem;
import org.kr.stocksmonitor.yahoo.YahooAPI;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.UnaryOperator;

public class StocksMonitorController implements Initializable {

    private static final Logger log = LogManager.getLogger(StocksMonitorController.class);
    protected static final ExecutorService executorService = Executors.newFixedThreadPool(16);
    public DatePicker yahooStartDatePicker;
    public DatePicker yahooEndDatePicker;
    public Slider yahooDateRangeSlider;
    public Label yahooDateRangeLabel;
    public TableView<QuoteItem> yahooTableFavoriteQuotes;

    protected HostServices hostServices;

    protected void injectHostServices(HostServices hostServices) {
        this.hostServices = hostServices;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        log.debug("initializing the controller...");
        initYahoooSymbolsCombobox();
        initYahoooTableView();
        initDatePickers();
    }

    private void initDatePickers() {
        yahooDateRangeSlider.setValue(1);
        setDatePickersBasedOnSlider();
        yahooDateRangeSlider.valueProperty().addListener((_, _, _) -> setDatePickersBasedOnSlider());
    }

    private void initYahoooTableView() {
        yahooTableFavoriteQuotes.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        yahooTableFavoriteQuotes.getSelectionModel().getSelectedItems().addListener((ListChangeListener<QuoteItem>) change -> {
            List<QuoteItem> selectedQuotes = (List<QuoteItem>)change.getList();
            handleSelectedQuotes(selectedQuotes);
        });

        loadFavoriteQuotes();
        tabPaneData.getSelectionModel().selectedItemProperty().addListener((_, oldValue, newValue) -> {
            if (newValue.equals(tabNews)) handleTabNewsSelection();
        });
        setYahooTableFavoriteQuotesColumns();
    }

    private void setYahooTableFavoriteQuotesColumns() {
        TableColumn<QuoteItem, String> typeDispColumn = new TableColumn<>("Type");
        typeDispColumn.setCellValueFactory(data -> Bindings.createStringBinding(() ->
                data.getValue().getTypeDisp()));
        typeDispColumn.setPrefWidth(60);

        TableColumn<QuoteItem, String> symbolColumn = new TableColumn<>("Symbol");
        symbolColumn.setCellValueFactory(data -> Bindings.createStringBinding(() ->
                data.getValue().getSymbol()));
        symbolColumn.setPrefWidth(60);

        TableColumn<QuoteItem, String> shortnameColumn = new TableColumn<>("Short Name");
        shortnameColumn.setCellValueFactory(data -> Bindings.createStringBinding(() ->
                data.getValue().getShortname()));
        shortnameColumn.setPrefWidth(220);

        TableColumn<QuoteItem, String> longnameColumn = new TableColumn<>("Long Name");
        longnameColumn.setCellValueFactory(data -> Bindings.createStringBinding(() ->
                data.getValue().getLongname()));
        longnameColumn.setPrefWidth(300);

        TableColumn<QuoteItem, String> quoteTypeColumn = new TableColumn<>("Type");
        quoteTypeColumn.setCellValueFactory(data -> Bindings.createStringBinding(() ->
                data.getValue().getQuoteType()));
        quoteTypeColumn.setPrefWidth(80);

        TableColumn<QuoteItem, String> exchangeColumn = new TableColumn<>("Exch");
        exchangeColumn.setCellValueFactory(data -> Bindings.createStringBinding(() ->
                data.getValue().getExchange()));
        exchangeColumn.setPrefWidth(60);

        TableColumn<QuoteItem, String> exchDispColumn = new TableColumn<>("Exchange");
        exchDispColumn.setCellValueFactory(data -> Bindings.createStringBinding(() ->
                data.getValue().getExchDisp()));
        exchDispColumn.setPrefWidth(90);

        TableColumn<QuoteItem, String> indexColumn = new TableColumn<>("Index");
        indexColumn.setCellValueFactory(data -> Bindings.createStringBinding(() ->
                data.getValue().getIndex()));
        indexColumn.setPrefWidth(0);

        TableColumn<QuoteItem, Boolean> isYahooFinanceColumn = new TableColumn<>("Yahoo");
        isYahooFinanceColumn.setCellValueFactory(data -> Bindings.createBooleanBinding(() ->
                data.getValue().isYahooFinance()));
        isYahooFinanceColumn.setPrefWidth(0);

        yahooTableFavoriteQuotes.getColumns().clear();
        yahooTableFavoriteQuotes.getColumns().addAll(typeDispColumn, symbolColumn, shortnameColumn, longnameColumn,
                quoteTypeColumn, exchangeColumn, exchDispColumn, indexColumn, isYahooFinanceColumn);
    }

    private void handleSelectedQuotes(List<QuoteItem> selectedQuotes) {
        log.debug(selectedQuotes);

    }

    private void handleTabNewsSelection() {
        log.debug("handleTabNewsSelection()");
    }

    private void loadFavoriteQuotes() {
        log.debug("loading favorite quotes...");
        List<QuoteItem> favQuotes = ConfigManager.getInstance().readFavoriteQuotes();
        if (favQuotes.isEmpty()) return;
        log.debug("adding favorite quotes to the table: {}", favQuotes);
        yahooTableFavoriteQuotes.getItems().addAll(favQuotes);
    }

    private void initYahoooSymbolsCombobox() {
        ComboBoxListViewSkin skin = new ComboBoxListViewSkin(cbxYahooQuote);
        skin.getPopupContent().addEventFilter(KeyEvent.ANY, e -> {
            if (e.getCode() == KeyCode.UNDEFINED) {
                log.debug("initYahoooTickersCombobox() {}", e);
                return;
            }
            if (e.getCode() == KeyCode.SPACE) {
                log.debug(e.getText());
                e.consume();
            }
        });
        cbxYahooQuote.setSkin(skin);
        cbxYahooQuote.setEditable(true);

        cbxYahooQuote.getEditor().textProperty().addListener((_, _, newValue) -> {
            reloadYahooTickerCombobox(newValue);
        });
        cbxYahooQuote.setOnAction(_ -> {
            final Object value = cbxYahooQuote.getSelectionModel().getSelectedItem();
            log.debug("selected: {}", value);
            if (!(value instanceof QuoteItem selected))
                return;
            var items = yahooTableFavoriteQuotes.getItems();
            if (items.contains(selected)) return;
            items.add(selected);
        });
        executorService.submit(() -> reloadYahooTickerCombobox("ACN"));
    }

    private void reloadYahooTickerCombobox(String value) {
        if (value.isEmpty())
            return;
        log.debug("reloading yahoo quotes data: {}", value);
        executorService.submit(() -> {
            Platform.runLater(() -> {
                Instant start = Instant.now();
                try {
                    reloadYahooQuotesCombobox(YahooAPI.getInstance().getQuotes(value));
                } catch (Exception e) {
                    log.error("Error while reloading the yahoo quotes", e);
                } finally {
                    LogUtils.debugDuration(log, start, "reloading yahoo quotes");
                }
            });
        });
    }

    private void reloadYahooQuotesCombobox(List<QuoteItem> quotes) {
        if (quotes.isEmpty()) return;
        log.debug("reloading yahoo quotes: {}", quotes);
        final ObservableList<QuoteItem> observableList = FXCollections.observableArrayList(quotes);
        final FilteredList<QuoteItem> filteredData = new FilteredList<>(observableList);

        UnaryOperator<TextFormatter.Change> filter = change -> {
            if (null == change) return null;
            if (0 == change.getSelection().getLength()) return change;
            if (change.getSelection().getLength() == change.getSelection().getEnd()) return change; //everything selected
            if (change.getSelection().getStart() == 0 && change.getSelection().getEnd() == 0) return change;
            final String newText = change.getControlNewText();
            if (newText.isEmpty()) {
                cbxYahooQuote.hide();
            } else {
                filteredData.setPredicate(quoteItem ->
                        quoteItem.getShortname().toLowerCase().contains(newText.toLowerCase()) ||
                                quoteItem.getSymbol().toLowerCase().contains(newText.toLowerCase()));
                cbxYahooQuote.show();
            }
            return change;
        };
        final TextFormatter<String> textFormatter = new TextFormatter<>(filter);
        cbxYahooQuote.setEditable(true);
        cbxYahooQuote.getEditor().setTextFormatter(textFormatter);

        // Set up string converter for correct display
        cbxYahooQuote.setConverter(new StringConverter<QuoteItem>() {
            @Override
            public String toString(QuoteItem o) {
                if (o == null) return null;
                return String.format("%s | %s | %s | Exch: %s",
                        o.getQuoteType(), o.getSymbol(), o.getShortname(), o.getExchDisp());
            }

            @Override
            public QuoteItem fromString(String string) { return null; }
        });

        // Bind the filtered list to the ComboBox's items
        cbxYahooQuote.setItems(filteredData);
        cbxYahooQuote.hide();
        cbxYahooQuote.setVisibleRowCount(filteredData.size());
        cbxYahooQuote.show();
    }
    
    public void shutdown() throws IOException {
        log.debug("shutting down the controller");
        executorService.shutdown();
        var favoriteQuotes = yahooTableFavoriteQuotes.getItems();
        ConfigManager.getInstance().saveFavoriteQuotes(favoriteQuotes);
    }

    protected void showAlert(Exception e, String header) {
        showAlert("Error", e.getMessage(), Arrays.toString(e.getStackTrace()));
    }

    protected void showAlert(String header, String message, String content) {
        Alert errorAlert = new Alert(Alert.AlertType.ERROR);
        errorAlert.setHeaderText(header + ": " + message);
        errorAlert.setContentText(content);
        errorAlert.showAndWait();
    }

    private void setDatePickersBasedOnSlider() {
        int dateRangeSliderValue = (int) yahooDateRangeSlider.getValue();
        switch (dateRangeSliderValue) {
            case 1:
                yahooDateRangeLabel.setText("1 day");
                yahooStartDatePicker.setValue(LocalDate.now().minusDays(1));
                yahooEndDatePicker.setValue(LocalDate.now());
                break;

            case 2:
                yahooDateRangeLabel.setText("2 days");
                yahooStartDatePicker.setValue(LocalDate.now().minusDays(2));
                yahooEndDatePicker.setValue(LocalDate.now());
                break;

            case 3:
                yahooDateRangeLabel.setText("3 days");
                yahooStartDatePicker.setValue(LocalDate.now().minusDays(3));
                yahooEndDatePicker.setValue(LocalDate.now());
                break;

            case 4:
                yahooDateRangeLabel.setText("1 week");
                yahooStartDatePicker.setValue(LocalDate.now().minusDays(7));
                yahooEndDatePicker.setValue(LocalDate.now());
                break;

            case 5:
                yahooDateRangeLabel.setText("2 weeks");
                yahooStartDatePicker.setValue(LocalDate.now().minusDays(14));
                yahooEndDatePicker.setValue(LocalDate.now());
                break;

            case 6:
                yahooDateRangeLabel.setText("1 month");
                yahooStartDatePicker.setValue(LocalDate.now().minusMonths(1));
                yahooEndDatePicker.setValue(LocalDate.now());
                break;

            case 7:
                yahooDateRangeLabel.setText("2 months");
                yahooStartDatePicker.setValue(LocalDate.now().minusMonths(2));
                yahooEndDatePicker.setValue(LocalDate.now());
                break;

            case 8:
                yahooDateRangeLabel.setText("3 months");
                yahooStartDatePicker.setValue(LocalDate.now().minusMonths(3));
                yahooEndDatePicker.setValue(LocalDate.now());
                break;

            case 9:
                yahooDateRangeLabel.setText("6 months");
                yahooStartDatePicker.setValue(LocalDate.now().minusMonths(6));
                yahooEndDatePicker.setValue(LocalDate.now());
                break;

            case 10:
                yahooDateRangeLabel.setText("1 year");
                yahooStartDatePicker.setValue(LocalDate.now().minusYears(1));
                yahooEndDatePicker.setValue(LocalDate.now());
                break;

            case 11:
                yahooDateRangeLabel.setText("2 years");
                yahooStartDatePicker.setValue(LocalDate.now().minusYears(2));
                yahooEndDatePicker.setValue(LocalDate.now());
                break;

            case 12:
                yahooDateRangeLabel.setText("3 years");
                yahooStartDatePicker.setValue(LocalDate.now().minusYears(3));
                yahooEndDatePicker.setValue(LocalDate.now());
                break;

            case 13:
                yahooDateRangeLabel.setText("5 years");
                yahooStartDatePicker.setValue(LocalDate.now().minusYears(5));
                yahooEndDatePicker.setValue(LocalDate.now());
                break;

            case 14:
                yahooDateRangeLabel.setText("10 years");
                yahooStartDatePicker.setValue(LocalDate.now().minusYears(10));
                yahooEndDatePicker.setValue(LocalDate.now());
                break;

            case 15:
                yahooDateRangeLabel.setText("MAX");
                yahooStartDatePicker.setValue(LocalDate.now().minusYears(200));
                yahooEndDatePicker.setValue(LocalDate.now());
                break;

            default:
                break;
        }
    }

    @FXML protected Tab tabNews;
    @FXML protected TabPane tabPaneData;
    @FXML protected ComboBox cbxYahooQuote;

    
    //polygon controls
    @FXML protected PasswordField tfApiKey;
    @FXML protected ComboBox cbxTicker;
    @FXML protected TableView tblTickers;
    @FXML protected DatePicker startDatePicker;
    @FXML protected DatePicker endDatePicker;
    @FXML protected Slider dateRangeSlider;
    @FXML protected Label dateRangeLabel;
    @FXML protected Label progressLabel;
    @FXML protected Tab tabSettings;
    @FXML protected ProgressIndicator progressIndicator;
    @FXML protected ChoiceBox<String> cbxAssetClass;
    @FXML protected ComboBox<String> cbxTickerType;
}