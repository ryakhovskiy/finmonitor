package org.kr.stocksmonitor.polygon;

import org.json.JSONObject;

public class NewsArticle {

    final String id;
    final String title;
    final String author;
    final String imageUrl;
    final String articleUrl;
    final String description;


    public NewsArticle(JSONObject json) {
        this.id = json.optString("id", "");
        this.title = json.optString("title", "");
        this.author = json.optString("author", "");
        this.imageUrl = json.optString("image_url", "");
        this.articleUrl = json.optString("article_url", "");
        this.description = json.optString("description", "");
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getArticleUrl() {
        return articleUrl;
    }

    public String getDescription() {
        return description;
    }

    /*
    * {"results":
    * [
    *   {
    *       "id":"JWROKjAy6E9pTlS-53UQxlnRpisefZ0Y8QPL64w9XZQ",
    *       "publisher":
    *           {"name":"The Motley Fool","homepage_url":"https://www.fool.com/",
    *               "logo_url":"https://s3.polygon.io/public/assets/news/logos/themotleyfool.svg",
    *               "favicon_url":"https://s3.polygon.io/public/assets/news/favicons/themotleyfool.ico"},
    *       "title":"SoundHound AI Could Have a Serious New Competitor as OpenAI Dives Into Speech Recognition. Here's The Stock I'm Keeping a Close Eye On.",
    *       "author":"newsfeedback@fool.com (Adam Spatacco)",
    *       "published_utc":"2024-04-11T11:20:00Z",
    *       "article_url":"https://www.fool.com/investing/2024/04/11/did-openai-just-say-checkmate-to-soundhound/",
    *       "tickers":["SOUN","NVDA","MSFT","GOOGL","AAPL","AMZN","GOOG"],
    *       "image_url":"https://g.foolcdn.com/editorial/images/772044/gettyimages-1483293781-1.jpg",
    *       "description":"SoundHound AI has attracted a lot of interest from investors this year, but a superior technology may have just emerged.",
    *       "keywords":["investing"]
    *   },
    *   {
    *       "id":"W8vDnfvUmBoypw2BcVrmfMPXbp3o7OqxRQQcu5gBTC8",
    *       "publisher":
    *           {"name":"The Motley Fool","homepage_url":"https://www.fool.com/",
    *               "logo_url":"https://s3.polygon.io/public/assets/news/logos/themotleyfool.svg",
    *               "favicon_url":"https://s3.polygon.io/public/assets/news/favicons/themotleyfool.ico"},
    *       "title":"3 Spectacular Warren Buffett Stocks to Buy Now and Hold Forever",
    *       "author":"newsfeedback@fool.com (Parkev Tatevosian, CFA)",
    *       "published_utc":"2024-04-11T10:05:00Z",
    *       "article_url":"https://www.fool.com/investing/2024/04/11/3-spectacular-warren-buffett-stocks-to-buy-now-and/",
    *       "tickers":["AMZN","BRK.A","BRK.B","AAPL","V"],
    *       "image_url":"https://g.foolcdn.com/editorial/images/772426/brk.jpg",
    *       "description":"Warren Buffett is arguably the greatest investor of all time.","keywords":["investing"]
    *   }
    * ],"status":"OK","request_id":"97150a36ef1d41b14fa0c72dcf812032","count":2}
    * */

}
