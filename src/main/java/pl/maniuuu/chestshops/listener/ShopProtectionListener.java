package pl.maniuuu.chestshops.listener;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import pl.maniuuu.chestshops.shop.Shop;
import pl.maniuuu.chestshops.shop.ShopService;
import pl.maniuuu.chestshops.util.Containers;

import java.util.List;

public final class ShopProtectionListener implements Listener {

    private final ShopService shops;

    public ShopProtectionListener(ShopService shops) {
        this.shops = shops;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        Shop signShop = shops.shopAtSign(block);
        if (signShop != null) {
            if (!shops.canModify(player, signShop)) {
                event.setCancelled(true);
                shops.messages().send(player, "error.not-your-shop");
                return;
            }
            shops.manager().remove(signShop);
            shops.messages().send(player, "shop.removed");
            return;
        }

        if (!shops.settings().protectContainers()) {
            return;
        }
        Shop containerShop = shops.shopAtContainer(block);
        if (containerShop != null && !shops.canModify(player, containerShop)) {
            event.setCancelled(true);
            shops.messages().send(player, "error.protected-container");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!shops.settings().protectContainers()) {
            return;
        }
        Block placed = event.getBlock();
        if (!Containers.isContainer(placed) && placed.getType() != org.bukkit.Material.HOPPER) {
            return;
        }
        for (Block neighbour : neighbours(placed)) {
            Shop shop = shops.shopAtContainer(neighbour);
            if (shop != null && !shops.canModify(event.getPlayer(), shop)) {
                event.setCancelled(true);
                shops.messages().send(event.getPlayer(), "error.protected-container");
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHopperMove(InventoryMoveItemEvent event) {
        if (!shops.settings().protectHoppers()) {
            return;
        }
        if (isShopInventory(event.getSource()) || isShopInventory(event.getDestination())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (shops.settings().protectExplosions()) {
            event.blockList().removeIf(this::isShopBlock);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (shops.settings().protectExplosions()) {
            event.blockList().removeIf(this::isShopBlock);
        }
    }

    private boolean isShopBlock(Block block) {
        return shops.shopAtSign(block) != null || shops.shopAtContainer(block) != null;
    }

    private boolean isShopInventory(Inventory inventory) {
        return inventory.getLocation() != null
                && shops.shopAtContainer(inventory.getLocation().getBlock()) != null;
    }

    private List<Block> neighbours(Block block) {
        return List.of(
                block.getRelative(1, 0, 0),
                block.getRelative(-1, 0, 0),
                block.getRelative(0, 0, 1),
                block.getRelative(0, 0, -1),
                block.getRelative(0, 1, 0),
                block.getRelative(0, -1, 0));
    }
}
