package net.questforge.prefabs;

/**
 * Represents a single prefab file discovered on the server.
 */
public class PrefabEntry {

    private final String name;
    private final String relativePath;
    private final String category;

    public PrefabEntry(String name, String relativePath, String category) {
        this.name = name;
        this.relativePath = relativePath;
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public String getCategory() {
        return category;
    }

    @Override
    public String toString() {
        return category.isEmpty() ? name : category + "/" + name;
    }
}
