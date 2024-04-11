package org.kr.stocksmonitor;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;


public class StocksMonitor extends Application {

    private static final Log logger = LogFactory.getLog(StocksMonitor.class);
    private StocksMonitorController controller;

    @Override
    public void start(Stage stage) throws IOException {
        logger.debug("starting the application...");
        FXMLLoader fxmlLoader = new FXMLLoader(StocksMonitor.class.getResource("main-view.fxml"));
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root);
        stage.setTitle("Hello!");
        stage.setScene(scene);
        stage.show();

        controller = fxmlLoader.getController();
        controller.injectHostServices(getHostServices());
    }

    @Override
    public void stop() throws Exception {
        logger.debug("shutting down the application...");
        if (null != controller) controller.shutdown();
    }

    public static void main(String[] args) {
        launch();
    }
}