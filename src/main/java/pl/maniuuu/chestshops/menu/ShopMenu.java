package pl.maniuuu.chestshops.menu;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import pl.maniuuu.chestshops.shop.Shop;
import pl.maniuuu.chestshops.shop.ShopService;
import pl.maniuuu.chestshops.util.Text;

import java.util.ArrayList;
import java.util.List;

/** Chest UI showing a single shop: preview of the goods plus buy and sell buttons. */
public final class ShopMenu implements InventoryHolder {

    public static final int BUY_SLOT = 11;
    public static final int PREVIEW_SLOT = 13;
    public static final int SELL_SLOT = 15;

    private final ShopService shops;
    private final Shop shop;
    private final Inventory inventory;

    public ShopMenu(ShopService shops, Shop shop) {
        this.shops = shops;
        this.shop = shop;
        this.inventory = Bukkit.createInventory(this, 27, shops.messages().get("menu.title",
                Text.text("owner", shop.ownerName()),
                Text.component("item", shops.itemName(shop.item()))));
        refresh();
    }

    public Shop shop() {
        return shop;
    }

    public void refresh() {
        inventory.clear();
        ItemStack filler = decorated(Material.GRAY_STAINED_GLASS_PANE, Component.empty(), List.of());
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }

        TagResolver[] resolvers = {
                Text.text("owner", shop.ownerName()),
                Text.component("item", shops.itemName(shop.item())),
                Text.number("amount", shop.amount()),
                Text.component("buy", shops.priceOrDash(shop.buyPrice())),
                Text.component("sell", shops.priceOrDash(shop.sellPrice())),
                Text.number("stock", shops.stock(shop)),
                Text.number("sold", shop.stats().itemsSold()),
                Text.number("bought", shop.stats().itemsBought())
        };

        ItemStack preview = shop.tradeStack();
        preview.setAmount(Math.min(shop.amount(), preview.getMaxStackSize()));
        inventory.setItem(PREVIEW_SLOT, decorated(preview,
                shops.messages().get("menu.preview-name", resolvers),
                lore("menu.preview-lore", resolvers)));

        inventory.setItem(BUY_SLOT, decorated(
                shop.buyEnabled() ? Material.EMERALD : Material.BARRIER,
                shops.messages().get(shop.buyEnabled() ? "menu.buy-name" : "menu.buy-disabled", resolvers),
                lore("menu.buy-lore", resolvers)));

        inventory.setItem(SELL_SLOT, decorated(
                shop.sellEnabled() ? Material.GOLD_INGOT : Material.BARRIER,
                shops.messages().get(shop.sellEnabled() ? "menu.sell-name" : "menu.sell-disabled", resolvers),
                lore("menu.sell-lore", resolvers)));
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    private List<Component> lore(String key, TagResolver... resolvers) {
        List<Component> lore = new ArrayList<>();
        for (String line : shops.messages().rawLine(key).split("\n")) {
            lore.add(Text.parse(line, resolvers).decoration(TextDecoration.ITALIC, false));
        }
        return lore;
    }

    private ItemStack decorated(Material material, Component name, List<Component> lore) {
        return decorated(ItemStack.of(material), name, lore);
    }

    private ItemStack decorated(ItemStack stack, Component name, List<Component> lore) {
        ItemStack copy = stack.clone();
        copy.editMeta(meta -> {
            meta.displayName(name.decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
        });
        return copy;
    }
}
