package pl.maniuuu.chestshops;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import pl.maniuuu.chestshops.command.ChestShopsCommand;
import pl.maniuuu.chestshops.config.Messages;
import pl.maniuuu.chestshops.config.ShopSettings;
import pl.maniuuu.chestshops.command.BanknoteCommand;
import pl.maniuuu.chestshops.economy.BanknoteService;
import pl.maniuuu.chestshops.economy.EconomyService;
import pl.maniuuu.chestshops.economy.InternalEconomyService;
import pl.maniuuu.chestshops.economy.VaultEconomyService;
import pl.maniuuu.chestshops.listener.ShopCreationListener;
import pl.maniuuu.chestshops.listener.ShopInteractionListener;
import pl.maniuuu.chestshops.listener.ShopMenuListener;
import pl.maniuuu.chestshops.listener.ShopProtectionListener;
import pl.maniuuu.chestshops.shop.ShopManager;
import pl.maniuuu.chestshops.shop.ShopService;
import pl.maniuuu.chestshops.storage.JsonShopStorage;
import pl.maniuuu.chestshops.storage.ShopStorage;

public final class ChestShopsPlugin extends JavaPlugin {

    private ShopSettings settings;
    private Messages messages;
    private ShopManager shopManager;
    private ShopService shopService;
    private EconomyService economy;
    private InternalEconomyService internalEconomy;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.settings = ShopSettings.load(getConfig());
        this.messages = Messages.load(this);

        this.internalEconomy = new InternalEconomyService(this);
        this.economy = resolveEconomy();
        getSLF4JLogger().info("Ekonomia: {}", economy.name());

        ShopStorage storage = new JsonShopStorage(getDataPath().resolve("shops.json"), getSLF4JLogger());
        this.shopManager = new ShopManager(storage, getSLF4JLogger());
        this.shopManager.load();
        this.shopService = new ShopService(this, shopManager, economy, messages, settings);

        Bukkit.getPluginManager().registerEvents(new ShopCreationListener(this, shopService), this);
        Bukkit.getPluginManager().registerEvents(new ShopInteractionListener(shopService), this);
        Bukkit.getPluginManager().registerEvents(new ShopProtectionListener(shopService), this);
        Bukkit.getPluginManager().registerEvents(new ShopMenuListener(shopService), this);

        BanknoteService banknotes = new BanknoteService(this, shopService);
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            new ChestShopsCommand(this, shopService).register(event.registrar());
            new BanknoteCommand(shopService, banknotes).register(event.registrar());
        });

        getServer().getAsyncScheduler().runAtFixedRate(this, task -> shopManager.saveIfDirty(),
                settings.autoSaveSeconds(), settings.autoSaveSeconds(), java.util.concurrent.TimeUnit.SECONDS);
    }

    @Override
    public void onDisable() {
        if (shopManager != null) {
            shopManager.save();
        }
        if (internalEconomy != null) {
            internalEconomy.save();
        }
    }

    public void reload() {
        reloadConfig();
        this.settings = ShopSettings.load(getConfig());
        this.messages = Messages.load(this);
        this.shopService.reload(settings, messages);
    }

    private EconomyService resolveEconomy() {
        if (settings.useVault() && Bukkit.getPluginManager().getPlugin("Vault") != null) {
            EconomyService vault = VaultEconomyService.tryHook(this);
            if (vault != null) {
                return vault;
            }
            getSLF4JLogger().warn("Vault jest zainstalowany, ale zaden plugin ekonomii nie zarejestrowal uslugi - uzywam wbudowanej ekonomii.");
        }
        internalEconomy.load();
        return internalEconomy;
    }

    public ShopSettings settings() {
        return settings;
    }

    public Messages messages() {
        return messages;
    }

    public ShopManager shopManager() {
        return shopManager;
    }

    public EconomyService economy() {
        return economy;
    }

    public InternalEconomyService internalEconomy() {
        return internalEconomy;
    }
}
