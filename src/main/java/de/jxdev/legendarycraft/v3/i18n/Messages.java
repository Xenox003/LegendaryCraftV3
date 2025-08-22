package de.jxdev.legendarycraft.v3.i18n;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Very small YAML-based i18n system.
 *
 * Files are named messages_<locale>.yml (e.g., messages_en_US.yml).
 * They are searched in the plugin data folder, default versions are bundled in resources and
 * copied over on first run via JavaPlugin#saveResource.
 */
public class Messages {
    private final JavaPlugin plugin;
    private final String defaultLocale;
    private String activeLocale;

    private FileConfiguration active;
    private FileConfiguration fallback;

    public Messages(JavaPlugin plugin, String activeLocale, String defaultLocale) {
        this.plugin = plugin;
        this.activeLocale = activeLocale;
        this.defaultLocale = defaultLocale;
        ensureDefaultFilesExist(activeLocale);
        if (!Objects.equals(activeLocale, defaultLocale)) {
            ensureDefaultFilesExist(defaultLocale);
        }
        reload();
    }

    private void ensureDefaultFilesExist(String locale) {
        String name = fileName(locale);
        File out = new File(plugin.getDataFolder(), name);
        if (!out.exists()) {
            // Only save if we actually have this resource packaged
            InputStream in = plugin.getResource(name);
            if (in != null) {
                plugin.saveResource(name, false);
            }
        }
    }

    private static String fileName(String locale) {
        return "messages_" + locale + ".yml";
    }

    public void setActiveLocale(String locale) {
        if (!Objects.equals(this.activeLocale, locale)) {
            this.activeLocale = locale;
            ensureDefaultFilesExist(locale);
            reload();
        }
    }

    public void reload() {
        this.active = loadForLocale(activeLocale);
        this.fallback = Objects.equals(activeLocale, defaultLocale) ? active : loadForLocale(defaultLocale);
    }

    private FileConfiguration loadForLocale(String locale) {
        File file = new File(plugin.getDataFolder(), fileName(locale));
        if (!file.exists()) {
            // Return empty config to allow fallback
            return new YamlConfiguration();
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    /**
     * Fetches a message by key. If missing in active locale, falls back to default locale; if still
     * missing, returns the key itself.
     */
    public String get(String key) {
        String val = active != null ? active.getString(key) : null;
        if (val == null && fallback != null) {
            val = fallback.getString(key);
        }
        return val != null ? val : key;
    }

    /**
     * Fetch and format a message, replacing placeholders like {name} in the value map.
     */
    public String get(String key, Map<String, Object> placeholders) {
        String base = get(key);
        if (placeholders == null || placeholders.isEmpty()) return base;
        String result = base;
        for (Map.Entry<String, Object> e : placeholders.entrySet()) {
            String token = "{" + e.getKey() + "}";
            String replacement = String.valueOf(e.getValue());
            result = result.replace(token, replacement);
        }
        return result;
    }

    /**
     * Convenience overload: key + even number of args as key1, val1, key2, val2 ...
     */
    public String get(String key, Object... kvPairs) {
        if (kvPairs == null || kvPairs.length == 0) return get(key);
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i + 1 < kvPairs.length; i += 2) {
            Object k = kvPairs[i];
            Object v = kvPairs[i + 1];
            if (k != null) map.put(String.valueOf(k), v);
        }
        return get(key, map);
    }
}
