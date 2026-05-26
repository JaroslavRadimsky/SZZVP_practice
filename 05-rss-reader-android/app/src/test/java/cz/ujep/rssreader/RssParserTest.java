package cz.ujep.rssreader;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class RssParserTest {
    @Test
    public void parsesRssItems() throws Exception {
        String xml = """
                <rss><channel>
                    <item>
                        <title>Prvni zprava</title>
                        <link>https://example.com/1</link>
                        <description>Detail zpravy</description>
                        <pubDate>Tue, 26 May 2026 10:00:00 GMT</pubDate>
                    </item>
                </channel></rss>
                """;

        List<RssItem> items = new RssParser().parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)),
                0
        );

        assertEquals(1, items.size());
        assertEquals("Prvni zprava", items.get(0).title);
        assertEquals("https://example.com/1", items.get(0).link);
    }
}

