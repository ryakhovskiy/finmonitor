package org.kr.stocksmonitor;

import javafx.concurrent.Task;
import javafx.scene.control.Hyperlink;
import javafx.scene.image.Image;
import org.kr.stocksmonitor.polygon.NewsArticle;

public class NewsArticleFX {

    final ImageLoadingTask image;
    final Hyperlink title;
    final String description;
    final String articleUrl;

    public NewsArticleFX(NewsArticle news) {
        this.image = new ImageLoadingTask(news.getImageUrl());
        this.title = new Hyperlink(news.getTitle());
        this.description = news.getDescription();
        this.articleUrl = news.getArticleUrl();
    }

    public ImageLoadingTask getImage() {
        return image;
    }

    public Hyperlink getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getArticleUrl() {
        return articleUrl;
    }

    public static class ImageLoadingTask extends Task<Image> {
        private final String imageUrl;

        public ImageLoadingTask(String imageUrl) {
            this.imageUrl = imageUrl;
        }

        @Override
        protected Image call() {
            return new Image(imageUrl);
        }
    }
}


