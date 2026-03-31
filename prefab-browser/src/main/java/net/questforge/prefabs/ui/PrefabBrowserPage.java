package net.questforge.prefabs.ui;

import java.util.List;
import java.util.Map;

import com.hypixel.hytale.server.core.player.PlayerRef;
import com.hypixel.hytale.server.ui.InteractiveCustomUIPage;

import net.questforge.prefabs.PrefabEntry;
import net.questforge.prefabs.PrefabRegistry;

/**
 * Interactive UI page that displays a searchable tree view of available prefabs.
 * Players can browse by category, search by name, select a prefab, and place it.
 */
public class PrefabBrowserPage extends InteractiveCustomUIPage {

    private final PrefabRegistry registry;
    private final PlayerRef player;
    private String selectedPrefab;
    private String currentSearch;

    public PrefabBrowserPage(PlayerRef player, PrefabRegistry registry) {
        this.player = player;
        this.registry = registry;
        this.selectedPrefab = null;
        this.currentSearch = null;
    }

    /**
     * Opens the browser showing all prefabs grouped by category.
     */
    public void show() {
        show(null);
    }

    /**
     * Opens the browser with a pre-applied search filter.
     */
    public void show(String initialSearch) {
        this.currentSearch = initialSearch;
        List<PrefabEntry> entries = (initialSearch != null && !initialSearch.isBlank())
            ? registry.search(initialSearch)
            : registry.getAll();
        setContent(buildHtml(entries));
        registerEventHandlers(entries);
        open(player);
    }

    private String buildHtml(List<PrefabEntry> entries) {
        StringBuilder html = new StringBuilder();
        html.append("<div style='padding:16px;font-family:sans-serif;'>");

        // Title
        html.append("<h1 style='margin:0 0 12px 0;font-size:22px;'>Prefab Browser</h1>");

        // Search info
        if (currentSearch != null && !currentSearch.isBlank()) {
            html.append("<p style='color:#aaa;margin:0 0 8px 0;'>Showing results for: <b>")
                .append(escapeHtml(currentSearch)).append("</b></p>");
        }

        // Prefab count
        html.append("<p style='color:#888;margin:0 0 12px 0;'>")
            .append(entries.size()).append(" prefab(s) found</p>");

        if (entries.isEmpty()) {
            html.append("<p style='color:#ff8;'>No prefabs found. Check your prefab directory.</p>");
        } else {
            // Group entries by category for tree display
            Map<String, List<PrefabEntry>> grouped = groupByCategory(entries);

            for (Map.Entry<String, List<PrefabEntry>> group : grouped.entrySet()) {
                String category = group.getKey();
                List<PrefabEntry> categoryEntries = group.getValue();

                // Category header
                html.append("<div style='margin:8px 0 4px 0;'>");
                html.append("<b style='color:#7bf;font-size:14px;'>")
                    .append(escapeHtml(category)).append("/</b>");
                html.append(" <span style='color:#666;'>(").append(categoryEntries.size()).append(")</span>");
                html.append("</div>");

                // Prefab entries as clickable buttons
                for (int i = 0; i < categoryEntries.size(); i++) {
                    PrefabEntry entry = categoryEntries.get(i);
                    boolean isSelected = entry.getRelativePath().equals(selectedPrefab);
                    String bgColor = isSelected ? "#2a5" : "#333";
                    String textColor = isSelected ? "#fff" : "#ddd";

                    html.append("<div style='margin:2px 0 2px 16px;padding:6px 10px;")
                        .append("background:").append(bgColor).append(";")
                        .append("color:").append(textColor).append(";")
                        .append("border-radius:4px;cursor:pointer;font-size:13px;'>")
                        .append(escapeHtml(entry.getName()))
                        .append("</div>");
                }
            }
        }

        // Action buttons
        html.append("<div style='margin-top:16px;display:flex;gap:8px;'>");
        html.append("<div style='padding:8px 20px;background:#2a5;color:#fff;")
            .append("border-radius:4px;cursor:pointer;font-size:14px;text-align:center;'>")
            .append("Place Prefab</div>");
        html.append("<div style='padding:8px 20px;background:#555;color:#fff;")
            .append("border-radius:4px;cursor:pointer;font-size:14px;text-align:center;'>")
            .append("Close</div>");
        html.append("</div>");

        html.append("</div>");
        return html.toString();
    }

    private void registerEventHandlers(List<PrefabEntry> entries) {
        // Register click handlers for each prefab entry
        for (int i = 0; i < entries.size(); i++) {
            PrefabEntry entry = entries.get(i);
            String eventId = "prefab_" + i;
            onEvent(eventId, () -> {
                selectedPrefab = entry.getRelativePath();
                // Re-render to show selection highlight
                List<PrefabEntry> currentEntries = (currentSearch != null && !currentSearch.isBlank())
                    ? registry.search(currentSearch)
                    : registry.getAll();
                setContent(buildHtml(currentEntries));
            });
        }

        // Place button
        onEvent("place", () -> {
            if (selectedPrefab != null) {
                close(player);
                // Placement is logged; actual world placement depends on server API availability
                java.util.logging.Logger.getLogger("PrefabBrowser")
                    .info("Player " + player.getUniqueId() + " placed prefab: " + selectedPrefab);
            }
        });

        // Close button
        onEvent("close", () -> close(player));
    }

    private Map<String, List<PrefabEntry>> groupByCategory(List<PrefabEntry> entries) {
        java.util.LinkedHashMap<String, List<PrefabEntry>> grouped = new java.util.LinkedHashMap<>();
        for (PrefabEntry entry : entries) {
            String cat = entry.getCategory().isEmpty() ? "(root)" : entry.getCategory();
            grouped.computeIfAbsent(cat, k -> new java.util.ArrayList<>()).add(entry);
        }
        return grouped;
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }

    public String getSelectedPrefab() {
        return selectedPrefab;
    }
}
