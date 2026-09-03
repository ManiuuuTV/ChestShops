package pl.maniuuu.chestshops.config;

import org.bukkit.configuration.file.FileConfiguration;
import pl.maniuuu.chestshops.shop.ClickAction;

import java.util.List;
import java.util.Locale;

public record ShopSettings(
        List<String> shopKeywords,
        List<String> adminKeywords,
        boolean useVault,
        double creationFee,
        int maxShopsPerPlayer,
        boolean protectContainers,
        boolean protectHoppers,
        boolean protectExplosions,
        ClickAction buyAction,
        ClickAction sellAction,
        ClickAction infoAction,
        ClickAction menuAction,
        int autoSaveSeconds,
        boolean logTransactions,
        String currencyFormat,
        List<String> signLayout,
        List<String> adminSignLayout,
        String disabledPriceText,
        int internalStartingBalance
) {

    public static ShopSettings load(FileConfiguration config) {
        return new ShopSettings(
                lowercase(config.getStringList("creation.shop-keywords")),
                lowercase(config.getStringList("creation.admin-keywords")),
                config.getBoolean("economy.use-vault", true),
                config.getDouble("creation.fee", 0.0D),
                config.getInt("creation.max-shops-per-player", 0),
                config.getBoolean("protection.containers", true),
                config.getBoolean("protection.hoppers", true),
                config.getBoolean("protection.explosions", true),
                ClickAction.parse(config.getString("interaction.buy", "LEFT_CLICK")),
                ClickAction.parse(config.getString("interaction.sell", "RIGHT_CLICK")),
                ClickAction.parse(config.getString("interaction.info", "SHIFT_LEFT_CLICK")),
                ClickAction.parse(config.getString("interaction.menu", "SHIFT_RIGHT_CLICK")),
                Math.max(30, config.getInt("storage.auto-save-seconds", 300)),
                config.getBoolean("storage.log-transactions", true),
                config.getString("economy.currency-format", "<gold>%.2f$</gold>"),
                config.getStringList("sign.player"),
                config.getStringList("sign.admin"),
                config.getString("sign.disabled-price", "<dark_gray>-</dark_gray>"),
                config.getInt("economy.internal.starting-balance", 500)
        );
    }

    private static List<String> lowercase(List<String> input) {
        return input.stream().map(value -> value.toLowerCase(Locale.ROOT)).toList();
    }

    public boolean isShopKeyword(String raw) {
        return shopKeywords.contains(raw.toLowerCase(Locale.ROOT));
    }

    public boolean isAdminKeyword(String raw) {
        return adminKeywords.contains(raw.toLowerCase(Locale.ROOT));
    }
}
