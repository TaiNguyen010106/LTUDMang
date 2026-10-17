package vn.edu.nptbrowser.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

/** Persistent quick links shown on every NPT Browser home tab. */
public final class QuickLinkStore {
    public record QuickLink(String title, String url, String glyph, String color) {
        public QuickLink {
            title = Objects.requireNonNull(title).trim();
            url = Objects.requireNonNull(url).trim();
            glyph = glyph == null || glyph.isBlank() ? "◈" : glyph.trim();
            color = color == null || color.isBlank() ? "6F48FF" : color.replace("#", "").trim();
            if (title.isBlank()) throw new IllegalArgumentException("Tên lối tắt không được để trống.");
            if (url.isBlank()) throw new IllegalArgumentException("Địa chỉ lối tắt không được để trống.");
            if (!color.matches("[0-9a-fA-F]{6}")) color = "6F48FF";
        }
    }

    private final Path path;
    private final List<QuickLink> links = new ArrayList<>();

    private QuickLinkStore(Path path) { this.path = path.toAbsolutePath().normalize(); }

    public static QuickLinkStore load(Path path, String homeUrl) throws IOException {
        QuickLinkStore store = new QuickLinkStore(path);
        boolean storeExists = Files.exists(store.path);
        if (storeExists) {
            Properties properties = new Properties();
            try (InputStream input = Files.newInputStream(store.path)) { properties.load(input); }
            int count;
            try { count = Integer.parseInt(properties.getProperty("count", "0")); }
            catch (NumberFormatException ex) { count = 0; }
            for (int i = 0; i < Math.min(Math.max(count, 0), 200); i++) {
                String prefix = "link." + i + ".";
                String title = properties.getProperty(prefix + "title", "").trim();
                String url = properties.getProperty(prefix + "url", "").trim();
                if (title.isBlank() || url.isBlank()) continue;
                store.links.add(new QuickLink(title, url,
                        properties.getProperty(prefix + "glyph", "◈"),
                        properties.getProperty(prefix + "color", "6F48FF")));
            }
        }
        if (!storeExists) {
            store.links.addAll(defaults(homeUrl));
            store.save();
        }
        return store;
    }

    public static List<QuickLink> defaults(String homeUrl) {
        return List.of(
                new QuickLink("Shrimple", homeUrl, "⬡", "7246FF"),
                new QuickLink("GitHub", "https://github.com", "◉", "6883FF"),
                new QuickLink("Java Docs", "https://docs.oracle.com/en/java/", "♨", "FF7A4E"),
                new QuickLink("YouTube", "https://www.youtube.com", "▶", "FF405F")
        );
    }

    public synchronized List<QuickLink> list() { return List.copyOf(links); }
    public Path path() { return path; }

    public synchronized void add(QuickLink link) throws IOException {
        Objects.requireNonNull(link);
        boolean duplicate = links.stream().anyMatch(existing -> existing.url().equalsIgnoreCase(link.url()));
        if (duplicate) throw new IllegalArgumentException("Trang web này đã có trong lối tắt.");
        links.add(link);
        try { save(); }
        catch (IOException ex) { links.remove(link); throw ex; }
    }

    public synchronized void remove(QuickLink link) throws IOException {
        int index = links.indexOf(link);
        if (index < 0) return;
        QuickLink removed = links.remove(index);
        try { save(); }
        catch (IOException ex) { links.add(Math.min(index, links.size()), removed); throw ex; }
    }

    private synchronized void save() throws IOException {
        Properties properties = new Properties();
        properties.setProperty("count", Integer.toString(links.size()));
        for (int i = 0; i < links.size(); i++) {
            QuickLink link = links.get(i);
            String prefix = "link." + i + ".";
            properties.setProperty(prefix + "title", link.title());
            properties.setProperty(prefix + "url", link.url());
            properties.setProperty(prefix + "glyph", link.glyph());
            properties.setProperty(prefix + "color", link.color());
        }
        Path parent = path.getParent();
        if (parent != null) Files.createDirectories(parent);
        try (OutputStream output = Files.newOutputStream(path)) {
            properties.store(output, "NPT Browser - quick links (Unicode values are escaped by Properties)");
        }
    }
}
