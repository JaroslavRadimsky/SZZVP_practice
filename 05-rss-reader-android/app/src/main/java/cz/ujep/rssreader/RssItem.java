package cz.ujep.rssreader;

public class RssItem {
    public long id;
    public final String title;
    public final String link;
    public final String description;
    public final long publishedAt;
    public final long fetchedAt;

    public RssItem(long id, String title, String link, String description, long publishedAt, long fetchedAt) {
        this.id = id;
        this.title = title;
        this.link = link;
        this.description = description;
        this.publishedAt = publishedAt;
        this.fetchedAt = fetchedAt;
    }
}

