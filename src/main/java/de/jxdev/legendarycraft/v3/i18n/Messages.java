package de.jxdev.legendarycraft.v3.i18n;

import org.bukkit.ChatColor;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Simple messages manager for loading language YAMLs and fetching messages with placeholders.
 *
 * Folder structure:
 * - resources/messages/en_US.yml (default)
 * - plugin data folder: messages/<locale>.yml (editable)
 */
public class Messages {
    private final JavaPlugin plugin;
    private final String defaultLocale;

    private String activeLocale;
    private FileConfiguration defaultConfig; // loaded from jar
    private FileConfiguration activeConfig;  // loaded from data folder

    // Default placeholders applied to every message, can be overridden per-call
    private final Map<String, String> defaultPlaceholders = new LinkedHashMap<>();

    public Messages(JavaPlugin plugin) {
        this(plugin, "en_US");
    }

    public Messages(JavaPlugin plugin, String defaultLocale) {
        this.plugin = plugin;
        this.defaultLocale = defaultLocale;
    }

    /**
     * Initialize messages by loading default and active locale based on config (config.yml: language: <code>).
     */
    public void init() {
        // Ensure default file available in jar
        this.defaultConfig = loadFromJar("messages/" + defaultLocale + ".yml");
        if (this.defaultConfig == null) {
            this.defaultConfig = new YamlConfiguration();
        }

        // Make sure the default file also exists in the data folder for users to edit/reference
        saveResourceIfNotExists("messages/" + defaultLocale + ".yml");

        // Read active locale from config (fallback to defaultLocale)
        this.plugin.saveDefaultConfig();
        this.activeLocale = Objects.toString(this.plugin.getConfig().getString("language"), defaultLocale);

        // Load active locale into data folder and config
        ensureLanguageFilePresent(activeLocale);
        this.activeConfig = loadFromDataFolder("messages/" + activeLocale + ".yml");
        if (this.activeConfig == null) {
            this.activeConfig = new YamlConfiguration();
        }

        // Let active inherit defaults from defaultConfig for missing keys
        this.activeConfig.setDefaults(this.defaultConfig);
        this.activeConfig.options().copyDefaults(true);

        // Initialize default placeholders
        defaultPlaceholders.clear();
        // Provide {prefix} by default for all messages
        defaultPlaceholders.put("prefix", getRawString("prefix"));
    }

    /**
     * Reload messages and config from disk (config.yml and messages files).
     */
    public void reload() {
        this.plugin.reloadConfig();
        init();
    }

    /**
     * Set or replace default placeholder value applied to every message.
     */
    public void putDefaultPlaceholder(String key, String value) {
        if (key == null) return;
        defaultPlaceholders.put(key, value);
    }

    /**
     * Replace the entire default placeholders map. Null clears it.
     */
    public void setDefaultPlaceholders(Map<String, String> defaults) {
        defaultPlaceholders.clear();
        if (defaults != null) {
            defaultPlaceholders.putAll(defaults);
        }
    }

    /**
     * Get an unmodifiable view of current default placeholders.
     */
    public Map<String, String> getDefaultPlaceholders() {
        return Collections.unmodifiableMap(defaultPlaceholders);
    }

    public String getActiveLocale() {
        return activeLocale;
    }

    public void setActiveLocale(String locale) {
        if (locale == null || locale.isBlank()) return;
        this.activeLocale = locale;
        ensureLanguageFilePresent(locale);
        this.activeConfig = loadFromDataFolder("messages/" + locale + ".yml");
        if (this.activeConfig == null) {
            this.activeConfig = new YamlConfiguration();
        }
        this.activeConfig.setDefaults(this.defaultConfig);
        this.activeConfig.options().copyDefaults(true);
        // Refresh default placeholders for new locale
        defaultPlaceholders.put("prefix", getRawString("prefix"));
    }

    /**
     * Get a formatted message string by key, with optional placeholders map.
     * If not found, returns the key itself surrounded by <> for visibility.
     */
    public String msg(String key, Map<String, String> placeholders) {
        String raw = getRawString(key);
        // Merge default placeholders with provided ones, with provided taking precedence
        Map<String, String> merged = new LinkedHashMap<>(defaultPlaceholders);
        if (placeholders != null && !placeholders.isEmpty()) {
            merged.putAll(placeholders);
        }
        if (!merged.isEmpty()) {
            for (Map.Entry<String, String> e : merged.entrySet()) {
                raw = raw.replace("{" + e.getKey() + "}", Objects.toString(e.getValue(), ""));
            }
        }
        return colorize(raw);
    }

    /** Convenience overload using varargs: key, k1, v1, k2, v2, ... */
    public String msg(String key, String... kv) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            map.put(kv[i], kv[i + 1]);
        }
        return msg(key, map);
    }

    /** Get a list of strings (e.g., multi-line messages). */
    public List<String> msgList(String key, Map<String, String> placeholders) {
        List<String> list = getRawStringList(key);
        List<String> out = new ArrayList<>(list.size());
        // Merge default placeholders first
        Map<String, String> merged = new LinkedHashMap<>(defaultPlaceholders);
        if (placeholders != null && !placeholders.isEmpty()) {
            merged.putAll(placeholders);
        }
        for (String line : list) {
            String processed = line;
            if (!merged.isEmpty()) {
                for (Map.Entry<String, String> e : merged.entrySet()) {
                    processed = processed.replace("{" + e.getKey() + "}", Objects.toString(e.getValue(), ""));
                }
            }
            out.add(colorize(processed));
        }
        return out;
    }

    public List<String> msgList(String key, String... kv) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            map.put(kv[i], kv[i + 1]);
        }
        return msgList(key, map);
    }

    private String getRawString(String key) {
        String value = activeConfig.getString(key);
        if (value == null) {
            value = defaultConfig.getString(key);
        }
        if (value == null) {
            return "<" + key + ">";
        }
        return value;
    }

    private List<String> getRawStringList(String key) {
        List<String> value = activeConfig.getStringList(key);
        if (value == null || value.isEmpty()) {
            value = defaultConfig.getStringList(key);
        }
        if (value == null || value.isEmpty()) {
            return Collections.singletonList("<" + key + ">");
        }
        return value;
    }

    private String colorize(String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    private void ensureLanguageFilePresent(String locale) {
        String path = "messages/" + locale + ".yml";
        File outFile = new File(plugin.getDataFolder(), path);
        if (outFile.exists()) return;
        // If resource exists, copy it, otherwise create empty file
        if (plugin.getResource(path) != null) {
            saveResourceIfNotExists(path);
        } else {
            outFile.getParentFile().mkdirs();
            try {
                if (outFile.createNewFile()) {
                    // prefill with comment header
                    try (FileWriter fw = new FileWriter(outFile, StandardCharsets.UTF_8)) {
                        fw.write("# Language file " + locale + "\n");
                    }
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create language file: " + outFile.getAbsolutePath());
            }
        }
    }

    private void saveResourceIfNotExists(String resourcePath) {
        File outFile = new File(plugin.getDataFolder(), resourcePath);
        if (outFile.exists()) return;
        outFile.getParentFile().mkdirs();
        try (InputStream in = plugin.getResource(resourcePath)) {
            if (in == null) return;
            try (OutputStream out = new FileOutputStream(outFile)) {
                in.transferTo(out);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save resource: " + resourcePath + " - " + e.getMessage());
        }
    }

    private FileConfiguration loadFromJar(String resourcePath) {
        try (InputStream in = plugin.getResource(resourcePath)) {
            if (in == null) return null;
            InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
            YamlConfiguration cfg = new YamlConfiguration();
            cfg.load(reader);
            return cfg;
        } catch (IOException | InvalidConfigurationException e) {
            plugin.getLogger().warning("Failed to load internal resource: " + resourcePath + ": " + e.getMessage());
            return null;
        }
    }

    private FileConfiguration loadFromDataFolder(String relativePath) {
        File file = new File(plugin.getDataFolder(), relativePath);
        if (!file.exists()) return null;
        YamlConfiguration cfg = new YamlConfiguration();
        try {
            cfg.load(file);
            return cfg;
        } catch (IOException | InvalidConfigurationException e) {
            plugin.getLogger().warning("Failed to load messages file: " + file.getAbsolutePath() + ": " + e.getMessage());
            return null;
        }
    }
}
