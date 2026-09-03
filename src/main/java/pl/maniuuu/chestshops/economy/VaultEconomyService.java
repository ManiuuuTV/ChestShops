package pl.maniuuu.chestshops.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public final class VaultEconomyService implements EconomyService {

    private final Economy economy;

    private VaultEconomyService(Economy economy) {
        this.economy = economy;
    }

    public static VaultEconomyService tryHook(JavaPlugin plugin) {
        try {
            RegisteredServiceProvider<Economy> provider = Bukkit.getServicesManager().getRegistration(Economy.class);
            return provider == null ? null : new VaultEconomyService(provider.getProvider());
        } catch (NoClassDefFoundError error) {
            plugin.getSLF4JLogger().warn("Vault API niedostepne w classpath.");
            return null;
        }
    }

    @Override
    public String name() {
        return "Vault (" + economy.getName() + ")";
    }

    @Override
    public double balance(UUID player) {
        return economy.getBalance(offline(player));
    }

    @Override
    public boolean has(UUID player, double amount) {
        return economy.has(offline(player), amount);
    }

    @Override
    public boolean withdraw(UUID player, double amount) {
        return economy.withdrawPlayer(offline(player), amount).transactionSuccess();
    }

    @Override
    public boolean deposit(UUID player, double amount) {
        return economy.depositPlayer(offline(player), amount).transactionSuccess();
    }

    private OfflinePlayer offline(UUID player) {
        return Bukkit.getOfflinePlayer(player);
    }
}
