package pl.maniuuu.chestshops.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import pl.maniuuu.chestshops.menu.ShopMenu;
import pl.maniuuu.chestshops.shop.ShopService;

public final class ShopMenuListener implements Listener {

    private final ShopService shops;

    public ShopMenuListener(ShopService shops) {
        this.shops = shops;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder(false) instanceof ShopMenu menu)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || event.getClickedInventory() != event.getInventory()) {
            return;
        }
        switch (event.getSlot()) {
            case ShopMenu.BUY_SLOT -> shops.buy(player, menu.shop());
            case ShopMenu.SELL_SLOT -> shops.sell(player, menu.shop());
            default -> {
                return;
            }
        }
        menu.refresh();
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder(false) instanceof ShopMenu) {
            event.setCancelled(true);
        }
    }
}
