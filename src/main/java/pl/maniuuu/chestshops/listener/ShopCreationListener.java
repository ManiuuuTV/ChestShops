package pl.maniuuu.chestshops.listener;

import org.bukkit.block.sign.Side;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import pl.maniuuu.chestshops.ChestShopsPlugin;
import pl.maniuuu.chestshops.shop.Shop;
import pl.maniuuu.chestshops.shop.ShopService;

public final class ShopCreationListener implements Listener {

    private final ChestShopsPlugin plugin;
    private final ShopService shops;

    public ShopCreationListener(ChestShopsPlugin plugin, ShopService shops) {
        this.plugin = plugin;
        this.shops = shops;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event) {
        if (event.getSide() != Side.FRONT || shops.shopAtSign(event.getBlock()) != null) {
            return;
        }
        Shop shop = shops.create(event.getPlayer(), event.getBlock(), event.lines());
        if (shop == null) {
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> shops.render(shop));
    }
}
