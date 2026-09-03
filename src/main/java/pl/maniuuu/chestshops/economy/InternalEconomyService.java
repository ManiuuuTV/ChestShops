package pl.maniuuu.chestshops.economy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Fallback economy used when no Vault provider is available. */
public final class InternalEconomyService implements EconomyService {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Double>>() {
    }.getType();

    private final JavaPlugin plugin;
    private final Map<UUID, Double> balances = new ConcurrentHashMap<>();
    private volatile double startingBalance = 500;
    private volatile boolean loaded;

    public InternalEconomyService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        this.startingBalance = plugin.getConfig().getDouble("economy.internal.starting-balance", 500);
        Path file = file();
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                Map<String, Double> stored = GSON.fromJson(reader, MAP_TYPE);
                if (stored != null) {
                    stored.forEach((key, value) -> balances.put(UUID.fromString(key), value));
                }
            } catch (IOException | IllegalArgumentException exception) {
                plugin.getSLF4JLogger().error("Nie udalo sie wczytac wbudowanej ekonomii", exception);
            }
        }
        loaded = true;
    }

    public void save() {
        if (!loaded) {
            return;
        }
        Map<String, Double> stored = new HashMap<>();
        balances.forEach((key, value) -> stored.put(key.toString(), value));
        try {
            Files.createDirectories(file().getParent());
            Path temp = file().resolveSibling("economy.json.tmp");
            try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                GSON.toJson(stored, MAP_TYPE, writer);
            }
            Files.move(temp, file(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            plugin.getSLF4JLogger().error("Nie udalo sie zapisac wbudowanej ekonomii", exception);
        }
    }

    @Override
    public String name() {
        return "wbudowana";
    }

    @Override
    public double balance(UUID player) {
        return balances.computeIfAbsent(player, ignored -> startingBalance);
    }

    @Override
    public boolean has(UUID player, double amount) {
        return balance(player) >= amount;
    }

    @Override
    public boolean withdraw(UUID player, double amount) {
        if (!has(player, amount)) {
            return false;
        }
        balances.merge(player, -amount, Double::sum);
        return true;
    }

    @Override
    public boolean deposit(UUID player, double amount) {
        balance(player);
        balances.merge(player, amount, Double::sum);
        return true;
    }

    private Path file() {
        return plugin.getDataPath().resolve("economy.json");
    }
}
