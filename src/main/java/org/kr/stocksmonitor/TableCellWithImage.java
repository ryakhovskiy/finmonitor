package org.kr.stocksmonitor;

import javafx.scene.control.TableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

class TableCellWithImage extends TableCell<NewsArticleFX, NewsArticleFX.ImageLoadingTask> {
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
