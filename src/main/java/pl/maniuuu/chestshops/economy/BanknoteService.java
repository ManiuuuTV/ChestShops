package pl.maniuuu.chestshops.economy;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import pl.maniuuu.chestshops.ChestShopsPlugin;
import pl.maniuuu.chestshops.shop.ShopService;
import pl.maniuuu.chestshops.util.Text;

import java.util.ArrayList;
import java.util.List;

/** Turns account balance into physical banknotes (paper) and back. */
public final class BanknoteService {

    private final NamespacedKey valueKey;
    private final NamespacedKey issuerKey;
    private final ShopService shops;

    public BanknoteService(ChestShopsPlugin plugin, ShopService shops) {
        this.valueKey = new NamespacedKey(plugin, "banknote-value");
        this.issuerKey = new NamespacedKey(plugin, "banknote-issuer");
        this.shops = shops;
    }

    /** Withdraws {@code amount} from the account and puts a banknote in the player's hands. */
    public boolean withdraw(Player player, double amount) {
        double min = shops.settings().banknoteMinAmount();
        if (amount < min) {
            shops.messages().send(player, "banknote.too-small", Text.component("price", shops.money(min)));
            return false;
        }
        if (!shops.economy().has(player.getUniqueId(), amount)) {
            shops.messages().send(player, "error.no-money", Text.component("price", shops.money(amount)));
            return false;
        }
        if (player.getInventory().firstEmpty() == -1) {
            shops.messages().send(player, "error.player-inventory-full");
            return false;
        }
        if (!shops.economy().withdraw(player.getUniqueId(), amount)) {
            shops.messages().send(player, "error.no-money", Text.component("price", shops.money(amount)));
            return false;
        }
        player.getInventory().addItem(create(amount, player.getName()));
        shops.messages().send(player, "banknote.withdrawn", Text.component("price", shops.money(amount)));
        return true;
    }

    /** Deposits every banknote held in the main hand back onto the account. */
    public boolean deposit(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        double unit = valueOf(hand);
        if (unit <= 0) {
            shops.messages().send(player, "banknote.not-a-note");
            return false;
        }
        double total = unit * hand.getAmount();
        if (!shops.economy().deposit(player.getUniqueId(), total)) {
            shops.messages().send(player, "banknote.deposit-failed");
            return false;
        }
        player.getInventory().setItemInMainHand(null);
        shops.messages().send(player, "banknote.deposited", Text.component("price", shops.money(total)));
        return true;
    }

    public ItemStack create(double amount, String issuer) {
        ItemStack note = ItemStack.of(Material.PAPER);
        TagResolver[] resolvers = {
                Text.component("price", shops.money(amount)),
                Text.text("player", issuer)
        };
        note.editMeta(meta -> {
            meta.displayName(shops.messages().get("banknote.item-name", resolvers)
                    .decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            for (String line : shops.messages().rawLine("banknote.item-lore").split("\n")) {
                lore.add(Text.parse(line, resolvers).decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(lore);
            meta.setEnchantmentGlintOverride(true);
            meta.getPersistentDataContainer().set(valueKey, PersistentDataType.DOUBLE, amount);
            meta.getPersistentDataContainer().set(issuerKey, PersistentDataType.STRING, issuer);
        });
        return note;
    }

    /** Value of a single banknote, or {@code 0} when the item is not one. */
    public double valueOf(ItemStack item) {
        if (item == null || item.getType() != Material.PAPER || !item.hasItemMeta()) {
            return 0;
        }
        Double value = item.getItemMeta().getPersistentDataContainer().get(valueKey, PersistentDataType.DOUBLE);
        return value == null || value <= 0 ? 0 : value;
    }
}
