package net.questforge.prefabs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Scans a directory for .prefab.json files and provides search/filter capabilities.
 */
public class PrefabRegistry {

    private static final Logger LOGGER = Logger.getLogger("PrefabBrowser");
    private static final String PREFAB_EXTENSION = ".prefab.json";

    private final Path prefabDirectory;
    private List<PrefabEntry> entries = Collections.emptyList();

    public PrefabRegistry(Path prefabDirectory) {
        this.prefabDirectory = prefabDirectory;
    }

    /**
     * Scans the prefab directory and rebuilds the entry list.
     */
    public void reload() {
        if (!Files.isDirectory(prefabDirectory)) {
            LOGGER.warning("Prefab directory not found: " + prefabDirectory);
            entries = Collections.emptyList();
            return;
        }

        List<PrefabEntry> found = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(prefabDirectory)) {
            walk.filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().endsWith(PREFAB_EXTENSION))
                .sorted()
                .forEach(p -> {
                    Path relative = prefabDirectory.relativize(p);
                    String fileName = p.getFileName().toString();
                    String name = fileName.substring(0, fileName.length() - PREFAB_EXTENSION.length());
                    String category = relative.getParent() != null
                        ? relative.getParent().toString().replace('\\', '/')
                        : "";
                    found.add(new PrefabEntry(name, relative.toString().replace('\\', '/'), category));
                });
        } catch (IOException e) {
            LOGGER.severe("Failed to scan prefab directory: " + e.getMessage());
        }

        entries = Collections.unmodifiableList(found);
        LOGGER.info("Found " + entries.size() + " prefabs in " + prefabDirectory);
    }

    /**
     * Returns all discovered prefab entries.
     */
    public List<PrefabEntry> getAll() {
        return entries;
    }

    /**
     * Returns entries whose name contains the search term (case-insensitive).
     */
    public List<PrefabEntry> search(String query) {
        if (query == null || query.isBlank()) {
            return entries;
        }
        String lower = query.toLowerCase();
        List<PrefabEntry> results = new ArrayList<>();
        for (PrefabEntry entry : entries) {
            if (entry.getName().toLowerCase().contains(lower)
                    || entry.getCategory().toLowerCase().contains(lower)) {
                results.add(entry);
            }
        }
        return results;
    }

    /**
     * Returns entries grouped by category (folder), preserving discovery order.
     */
    public Map<String, List<PrefabEntry>> getGroupedByCategory() {
        Map<String, List<PrefabEntry>> grouped = new LinkedHashMap<>();
        for (PrefabEntry entry : entries) {
            String cat = entry.getCategory().isEmpty() ? "(root)" : entry.getCategory();
            grouped.computeIfAbsent(cat, k -> new ArrayList<>()).add(entry);
        }
        return grouped;
    }

    public Path getPrefabDirectory() {
        return prefabDirectory;
    }
}
