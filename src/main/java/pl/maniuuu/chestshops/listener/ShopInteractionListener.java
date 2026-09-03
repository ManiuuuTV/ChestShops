package pl.maniuuu.chestshops.listener;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import pl.maniuuu.chestshops.shop.ClickAction;
import pl.maniuuu.chestshops.shop.Shop;
import pl.maniuuu.chestshops.shop.ShopService;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ShopInteractionListener implements Listener {

    private static final long CLICK_COOLDOWN_MILLIS = 250;

    private final ShopService shops;
    private final Map<UUID, Long> lastClick = new ConcurrentHashMap<>();

    public ShopInteractionListener(ShopService shops) {
        this.shops = shops;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null || (event.getAction() != Action.LEFT_CLICK_BLOCK
                && event.getAction() != Action.RIGHT_CLICK_BLOCK)) {
            return;
        }
        Shop shop = shops.shopAtSign(block);
        if (shop == null) {
            return;
        }
        event.setCancelled(true);

        Player player = event.getPlayer();
        if (!player.hasPermission("chestshops.use")) {
            shops.messages().send(player, "error.no-permission");
            return;
        }
        long now = System.currentTimeMillis();
        Long previous = lastClick.put(player.getUniqueId(), now);
        if (previous != null && now - previous < CLICK_COOLDOWN_MILLIS) {
            return;
        }

        ClickAction action = ClickAction.of(event.getAction(), player.isSneaking());
        if (action == shops.settings().menuAction()) {
            shops.openMenu(player, shop);
        } else if (action == shops.settings().infoAction()) {
            shops.info(player, shop);
        } else if (action == shops.settings().buyAction()) {
            shops.buy(player, shop);
        } else if (action == shops.settings().sellAction()) {
            shops.sell(player, shop);
        }
    }

    @EventHandler
    public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        lastClick.remove(event.getPlayer().getUniqueId());
    }
}
