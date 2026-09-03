package pl.maniuuu.chestshops.config;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import pl.maniuuu.chestshops.util.Text;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class Messages {

    private final Map<String, String> raw;
    private final String prefix;

    private Messages(Map<String, String> raw, String prefix) {
        this.raw = raw;
        this.prefix = prefix;
    }

    public static Messages load(JavaPlugin plugin) {
        Path file = plugin.getDataPath().resolve("messages.yml");
        if (!Files.exists(file)) {
            plugin.saveResource("messages.yml", false);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file.toFile());
        try (InputStream defaults = plugin.getResource("messages.yml")) {
            if (defaults != null) {
                config.setDefaults(YamlConfiguration.loadConfiguration(
                        new InputStreamReader(defaults, StandardCharsets.UTF_8)));
            }
        } catch (IOException exception) {
            plugin.getSLF4JLogger().warn("Nie udalo sie wczytac domyslnych wiadomosci", exception);
        }
        Map<String, String> values = new HashMap<>();
        for (String key : config.getKeys(true)) {
            if (config.isString(key)) {
                values.put(key, Objects.requireNonNullElse(config.getString(key), ""));
            }
        }
        return new Messages(Map.copyOf(values), values.getOrDefault("prefix", ""));
    }

    public String rawLine(String key) {
        return raw.getOrDefault(key, "<red>Brak wiadomosci: " + key + "</red>");
    }

    public Component get(String key, TagResolver... resolvers) {
        return Text.parse(rawLine(key), resolvers);
    }

    public Component prefixed(String key, TagResolver... resolvers) {
        return Text.parse(prefix + rawLine(key), resolvers);
    }

    public void send(Audience audience, String key, TagResolver... resolvers) {
        audience.sendMessage(prefixed(key, resolvers));
    }
}
