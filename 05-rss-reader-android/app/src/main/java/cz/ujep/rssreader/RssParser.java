package cz.ujep.rssreader;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

public class RssParser {
    public List<RssItem> parse(InputStream stream, long fetchedAt) throws Exception {
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream);
        NodeList nodes = document.getElementsByTagName("item");
        List<RssItem> items = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            Element item = (Element) nodes.item(i);
            items.add(new RssItem(
                    0,
                    text(item, "title"),
                    text(item, "link"),
                    text(item, "description"),
                    parseDate(text(item, "pubDate"), fetchedAt),
                    fetchedAt
            ));
        }
        return items;
    }

    private static String text(Element item, String tag) {
        NodeList nodes = item.getElementsByTagName(tag);
        if (nodes.getLength() == 0 || nodes.item(0).getTextContent() == null) {
            return "";
        }
        return nodes.item(0).getTextContent().trim();
    }

    private static long parseDate(String raw, long fallback) {
        try {
            return ZonedDateTime.parse(raw, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli();
        } catch (Exception ignored) {
            return fallback;
        }
    }
}

