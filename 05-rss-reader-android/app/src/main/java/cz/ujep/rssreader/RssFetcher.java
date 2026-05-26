package cz.ujep.rssreader;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class RssFetcher {
    public List<RssItem> fetch(String url) throws Exception {
        long fetchedAt = System.currentTimeMillis();
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("User-Agent", "UJEP-RssReader/1.0");
        try (InputStream stream = connection.getInputStream()) {
            return new RssParser().parse(stream, fetchedAt);
        } finally {
            connection.disconnect();
        }
    }
}
