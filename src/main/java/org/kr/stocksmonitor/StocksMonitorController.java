package org.kr.stocksmonitor;

import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.util.StringConverter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kr.stocksmonitor.config.ConfigManager;
import org.kr.stocksmonitor.polygon.NewsArticle;
import org.kr.stocksmonitor.polygon.PolygonAPI;
import org.kr.stocksmonitor.polygon.Ticker;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class StocksMonitorController implements Initializable, PolygonAPI.ProgressCallback {

    private static final Log logger = LogFactory.getLog(StocksMonitorController.class);

    private final PolygonAPI api = new PolygonAPI();
    @FXML public TabPane tabPaneData;

    private String lastSelectedAsset = "";
    private Map<String, Map<String, List<Ticker>>> tickers = new HashMap<>();

    private HostServices hostServices;

    protected void injectHostServices(HostServices hostServices) {
        this.hostServices = hostServices;
    }

    private void logDuration(Instant startCall, String action) {
        logger.debug(String.format("%s took %d ms", action, Duration.between(Instant.now(), startCall).toMillis()));
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        logger.debug("initializing the controller...");
        loadTickers();
        initSettings();
        initDatePickers();
        initTickersCombobox();
        initTableView();
    }

    private void initTableView() {
        tblTickers.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tblTickers.getSelectionModel().getSelectedItems().addListener((ListChangeListener<Ticker>) change -> {
            List<Ticker> selectedTickers = (List<Ticker>)change.getList();
            handleSelectedTickers(selectedTickers);
        });

        loadFavoriteTickers();
        tabPaneData.getSelectionModel().selectedItemProperty().addListener((_, oldValue, newValue) -> {
            if (newValue.equals(tabNews)) handleTabNewsSelection();
        });
    }

    private void handleTabNewsSelection() {
        logger.debug("News tab is selected");
        List<Ticker> tickers = tblTickers.getSelectionModel().getSelectedItems();
        handleSelectedTickers(tickers);
    }

    private void handleSelectedTickers(List<Ticker> tickers) {
        logger.debug(tickers);
        loadTickerNews(tickers);
    }

    private void loadTickerNews(List<Ticker> tickers) {
        if (!tabNews.isSelected())
            return;

        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();
        List<NewsArticle> news = new ArrayList<>();
        for (Ticker t : tickers) {
            try {
                news.addAll(api.getTickerNews(t, start, end));
            } catch (IOException e) {
                logger.error(e);
            }
        }
        refreshNewsTab(news);
    }

    private void initSettings() {
        tfApiKey.setText(api.getApiKey());
        tfApiKey.textProperty().addListener((observable, oldValue, newValue) -> {
            api.updateApiKey(newValue);
        });
    }

    private void updateApiKey() {
        String apiKey = tfApiKey.getText();
        if (apiKey.equals(this.api.getApiKey())) return;
        ConfigManager.getInstance().saveApiKey(apiKey);
    }

    private void initDatePickers() {
        dateRangeSlider.setValue(1);
        setDatePickersBasedOnSlider();
        dateRangeSlider.valueProperty().addListener((_, _, _) -> setDatePickersBasedOnSlider());
    }

    private void initTickersCombobox() {
        ComboBoxListViewSkin skin = new ComboBoxListViewSkin(cbxTicker);
        skin.getPopupContent().addEventFilter(KeyEvent.ANY, e -> {
            if (e.getCode() == KeyCode.SPACE) {
                e.consume();
            }
        });
        cbxTicker.setSkin(skin);

        cbxTicker.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (!(newValue instanceof Ticker))
                return;
            var items = tblTickers.getItems();
            if (!items.contains(newValue))
                items.add((Ticker) newValue);
        });
    }
    
    public void shutdown() throws IOException {
        if (!tickers.isEmpty()) Ticker.saveToFile(tickers);
        updateApiKey();
        saveFavoriteTickers();
    }

    private void saveFavoriteTickers() {
        List<Ticker> favoriteTickers = tblTickers.getItems();
        ConfigManager.getInstance().saveFavoriteTickers(favoriteTickers);
    }

    private void loadFavoriteTickers() {
        logger.debug("loading favorite tickers...");
        List<String> favoriteTickerSymbols = ConfigManager.getInstance().readFavoriteTickerSymbols();
        var favoriteTickers = filterTickersBySymbols(favoriteTickerSymbols, tickers);
        logger.debug("adding favorite tickers to the table: " + favoriteTickerSymbols + "; " + favoriteTickers);
        tblTickers.getItems().addAll(favoriteTickers);
    }

    private List<Ticker> filterTickersBySymbols(List<String> symbols, Map<String, Map<String, List<Ticker>>> tickers) {
        return tickers.values().stream() // Stream over the inner maps
                .flatMap(innerMap -> innerMap.values().stream()) // Flatten the values to get a stream of lists of Tickers
                .flatMap(List::stream) // Flatten the lists to get a stream of Tickers
                .filter(ticker -> symbols.contains(ticker.getTicker())) // Filter Tickers based on symbol property
                .collect(Collectors.toList()); // Collect the filtered Tickers into a list
    }

    private void setDatePickersBasedOnSlider() {
        int dateRangeSliderValue = (int) dateRangeSlider.getValue();
        switch (dateRangeSliderValue) {
            case 1:
                dateRangeLabel.setText("1 day");
                startDatePicker.setValue(LocalDate.now().minusDays(1));
                endDatePicker.setValue(LocalDate.now());
                break;

            case 2:
                dateRangeLabel.setText("2 days");
                startDatePicker.setValue(LocalDate.now().minusDays(2));
                endDatePicker.setValue(LocalDate.now());
                break;

            case 3:
                dateRangeLabel.setText("3 days");
                startDatePicker.setValue(LocalDate.now().minusDays(3));
                endDatePicker.setValue(LocalDate.now());
                break;

            case 4:
                dateRangeLabel.setText("1 week");
                startDatePicker.setValue(LocalDate.now().minusDays(7));
                endDatePicker.setValue(LocalDate.now());
                break;

            case 5:
                dateRangeLabel.setText("2 weeks");
                startDatePicker.setValue(LocalDate.now().minusDays(14));
                endDatePicker.setValue(LocalDate.now());
                break;

            case 6:
                dateRangeLabel.setText("1 month");
                startDatePicker.setValue(LocalDate.now().minusMonths(1));
                endDatePicker.setValue(LocalDate.now());
                break;

            case 7:
                dateRangeLabel.setText("2 months");
                startDatePicker.setValue(LocalDate.now().minusMonths(2));
                endDatePicker.setValue(LocalDate.now());
                break;

            case 8:
                dateRangeLabel.setText("3 months");
                startDatePicker.setValue(LocalDate.now().minusMonths(3));
                endDatePicker.setValue(LocalDate.now());
                break;

            case 9:
                dateRangeLabel.setText("6 months");
                startDatePicker.setValue(LocalDate.now().minusMonths(6));
                endDatePicker.setValue(LocalDate.now());
                break;

            case 10:
                dateRangeLabel.setText("1 year");
                startDatePicker.setValue(LocalDate.now().minusYears(1));
                endDatePicker.setValue(LocalDate.now());
                break;

            case 11:
                dateRangeLabel.setText("2 years");
                startDatePicker.setValue(LocalDate.now().minusYears(2));
                endDatePicker.setValue(LocalDate.now());
                break;

            case 12:
                dateRangeLabel.setText("3 years");
                startDatePicker.setValue(LocalDate.now().minusYears(3));
                endDatePicker.setValue(LocalDate.now());
                break;

            case 13:
                dateRangeLabel.setText("5 years");
                startDatePicker.setValue(LocalDate.now().minusYears(5));
                endDatePicker.setValue(LocalDate.now());
                break;

            case 14:
                dateRangeLabel.setText("10 years");
                startDatePicker.setValue(LocalDate.now().minusYears(10));
                endDatePicker.setValue(LocalDate.now());
                break;

            case 15:
                dateRangeLabel.setText("MAX");
                startDatePicker.setValue(LocalDate.now().minusYears(200));
                endDatePicker.setValue(LocalDate.now());
                break;

            default:
                break;
        }
    }

    @FXML
    protected void btnLoadTickersHit() {
        loadTickers();
    }

    private void refreshNewsTab(List<NewsArticle> news) {
        Instant startCall = Instant.now();

        tabNews.setContent(null);
        if (null == news || news.isEmpty()) {
            Label label = new Label("No news found");
            label.setStyle("-fx-font-weight: bold; -fx-font-size: 16;");
            label.setMaxWidth(Double.MAX_VALUE);
            label.setMaxHeight(Double.MAX_VALUE);
            label.setAlignment(Pos.CENTER);
            StackPane pane = new StackPane(label);
            tabNews.setContent(pane);
        } else {
            createNewsTableView(news);
        }
        logDuration(startCall, "refreshNewsTab(List<NewsArticle> news)");
    }

    private void createNewsTableView(List<NewsArticle> news) {
        TableView<NewsArticleFX> tableView = new TableView<>();

        TableColumn<NewsArticleFX, NewsArticleFX.ImageLoadingTask> imageColumn = new TableColumn<>("Image");
        imageColumn.setCellFactory(_ -> new TableCellWithImage());
        imageColumn.setPrefWidth(128);
        imageColumn.setCellValueFactory((new PropertyValueFactory<>("image")));

        TableColumn<NewsArticleFX, Hyperlink> titleColumn = new TableColumn<>("Title");
        titleColumn.setCellFactory(_ -> new TableCell<>() {
            private final Hyperlink hyperlink = new Hyperlink();

            {
                hyperlink.setOnAction(_ -> {
                    NewsArticleFX article = getTableView().getItems().get(getIndex());
                    if (article != null) {
                        if (null != hostServices) hostServices.showDocument(article.getArticleUrl());
                    }
                });
            }

            @Override
            protected void updateItem(Hyperlink item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    NewsArticleFX article = getTableView().getItems().get(getIndex());
                    hyperlink.setText(article.getTitle().getText());
                    setGraphic(hyperlink);
                    setWrapText(true);
                }
            }
        });
        titleColumn.setCellValueFactory((new PropertyValueFactory<>("title")));
        titleColumn.setPrefWidth(300);


        // Create TableColumn for description
        TableColumn<NewsArticleFX, String> descriptionColumn = new TableColumn<>("Description");
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        descriptionColumn.setCellFactory(_ -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null);
                } else {
                    setText(item);
                    setWrapText(true);
                }
            }
        });
        descriptionColumn.setPrefWidth(450);

        // Add columns to TableView
        tableView.getColumns().addAll(imageColumn, titleColumn, descriptionColumn);

        var newsFX = news.stream().map(NewsArticleFX::new).toList();

        tableView.getItems().addAll(newsFX);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        StackPane pane = new StackPane(tableView);
        tabNews.setContent(pane);
    }

    @FXML
    protected void cbxAssetClassChanged() {
        String assetClass = cbxAssetClass.getSelectionModel().getSelectedItem().toString();
        if (assetClass.equals(lastSelectedAsset)) return;
        lastSelectedAsset = assetClass;
        cbxTickerType.getItems().clear();
        Map<String, List<Ticker>> tickerTypes = tickers.get(assetClass);
        if (null == tickerTypes) {
            showAlert("Error", "No ticker types found for: " + assetClass,
                    "Try to reload tickers on the Tab Settings. Available Asset Classes: " +
                            String.join(", ", tickers.keySet()));
            return;
        }
        cbxTickerType.getItems().addAll(tickerTypes.keySet());
        reloadTickerCombobox();
    }

    @FXML
    protected void cbxTickerTypeChanged(ActionEvent actionEvent) {
        reloadTickerCombobox();
    }

    private void reloadTickerCombobox() {
        String assetClass = cbxAssetClass.getSelectionModel().getSelectedItem().toString();

        Object objTickerType = cbxTickerType.getSelectionModel().getSelectedItem();
        String tickerType = null == objTickerType ? "" : objTickerType.toString();

        List<Ticker> data;
        if (tickerType.isEmpty()) {
            data = tickers.getOrDefault(assetClass, Collections.emptyMap())
                    .values()
                    .stream()
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
        } else {
            data = tickers.getOrDefault(assetClass, Collections.emptyMap())
                    .getOrDefault(tickerType, Collections.emptyList());
        }
        if (data.isEmpty()) return;

        ObservableList<Ticker> observableList = FXCollections.observableArrayList(data);
        FilteredList<Ticker> filteredData = new FilteredList<>(observableList);
        cbxTicker.setItems(filteredData);
        updateTickerCombobox(filteredData);
    }

    private void updateTickerCombobox(FilteredList<Ticker> filteredData) {
        UnaryOperator<TextFormatter.Change> filter = change -> {
            if (change.getSelection().getStart() == 0 && change.getSelection().getEnd() == 0) return change;
            String newText = change.getControlNewText();
            if (newText.isEmpty()) {
                cbxTicker.hide();
            } else {
                filteredData.setPredicate(ticker ->
                        ticker.getTicker().toLowerCase().contains(newText.toLowerCase()) ||
                                ticker.getName().toLowerCase().contains(newText.toLowerCase()));
                cbxTicker.show();
            }
            return change;
        };
        TextFormatter<String> textFormatter = new TextFormatter<>(filter);
        cbxTicker.setEditable(true);
        cbxTicker.getEditor().setTextFormatter(textFormatter);

        // Set up string converter for correct display
        cbxTicker.setConverter(new StringConverter<Ticker>() {
            @Override
            public String toString(Ticker object) {
                if (object == null) return null;
                return object.getTicker() + ": " + object.getName();
            }

            @Override
            public Ticker fromString(String string) {
                return null; // Not used for auto-completion
            }
        });

        // Bind the filtered list to the ComboBox's items
        cbxTicker.setItems(filteredData);
    }

    private void loadAssetClassesCombobox() {
        if (tickers.isEmpty()) return;
        Set<String> assetClasses = tickers.keySet();
        cbxAssetClass.getItems().clear();
        cbxAssetClass.getItems().addAll(assetClasses);
    }

    private void showAlert(Exception e, String header) {
        showAlert("Error", e.getMessage(), Arrays.toString(e.getStackTrace()));
    }

    private void showAlert(String header, String message, String content) {
        Alert errorAlert = new Alert(Alert.AlertType.ERROR);
        errorAlert.setHeaderText(header + ": " + message);
        errorAlert.setContentText(content);
        errorAlert.showAndWait();
    }

    public void loadTickers() {
        try {
            tickers = Ticker.readFromFile();
            loadAssetClassesCombobox();
        } catch (Exception e) {
            showErrorAlert();
        }
    }

    private void downloadTickers() {
        Task<Map<String, Map<String, List<Ticker>>>> task = new Task<>() {
            @Override
            protected Map<String, Map<String, List<Ticker>>> call() {
                try {
                    tickers = api.loadAllTickers(StocksMonitorController.this);
                } catch (Exception e) {
                    showAlert(e, "Error while downloading tickers");
                }
                return tickers;
            }
        };

        task.setOnRunning(_ -> {
            progressIndicator.setVisible(true);
            progressLabel.setVisible(true);
            tabSettings.getTabPane().getSelectionModel().select(tabSettings);
        });
        task.setOnSucceeded(_ -> {
            progressIndicator.setVisible(false);
            progressLabel.setVisible(false);
            tickers = task.getValue();
        });

        new Thread(task).start();
    }

    private void showErrorAlert() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Failed to load tickers from file");
        alert.setContentText("Do you want to download tickers from an external source?");
        ButtonType yesButton = new ButtonType("Yes");
        ButtonType noButton = new ButtonType("No");
        alert.getButtonTypes().setAll(yesButton, noButton);
        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == yesButton) {
            startDownload();
        }
    }

    private void startDownload() {
        try {
            downloadTickers();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to load tickers from the external source");
            alert.setContentText(e.getMessage());
            alert.show();
        }
    }

    @Override
    public void onProgressUpdate(String progress) {
        Platform.runLater(() -> {
            progressLabel.setText(progress);
        });
    }


    public void btnLoadDataForAllFavoriteTickersHit(ActionEvent actionEvent) {
    }

    public void btnRemoveSelectedFavoriteTickersHit(ActionEvent actionEvent) {
        ObservableList<Ticker> selectedItems = tblTickers.getSelectionModel().getSelectedItems();
        tblTickers.getItems().removeAll(selectedItems);
    }

    public void btnRemoveAllFavoriteTickersHit(ActionEvent actionEvent) {
        tblTickers.getItems().clear();
    }

    public void btnLoadDataForSelectedFavoriteTickersHit(ActionEvent actionEvent) {
        //refreshNewsTab(null);
    }

    @FXML public Button btnLoadDataForAllFavoriteTickers;
    @FXML public Button btnRemoveSelectedFavoriteTickers;
    @FXML public Button btnRemoveAllFavoriteTickers;
    @FXML public Button btnLoadDataForSelectedFavoriteTickers;
    @FXML public ComboBox cbxTicker;
    @FXML public Tab tabSettings;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Label progressLabel;
    @FXML private ChoiceBox<String> cbxAssetClass;
    @FXML private ComboBox<String> cbxTickerType;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private Slider dateRangeSlider;
    @FXML private Label dateRangeLabel;
    @FXML public TableView<Ticker> tblTickers;
    @FXML public PasswordField tfApiKey;
    @FXML public Tab tabNews;

    public static class TableCellWithImage extends TableCell<NewsArticleFX, NewsArticleFX.ImageLoadingTask> {
        @Override
        protected void updateItem(NewsArticleFX.ImageLoadingTask item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
            } else {

                NewsArticleFX news = getTableView().getItems().get(getIndex());
                var task = news.image;
                task.setOnSucceeded(_ -> {
                    Image loadedImage = task.getValue();
                    ImageView imageView = new ImageView(loadedImage);
                    imageView.setPreserveRatio(true);
                    imageView.setFitWidth(128);
                    imageView.setFitHeight(128);
                    setGraphic(imageView);
                });
                new Thread(task).start();
            }
        }
    }
}
