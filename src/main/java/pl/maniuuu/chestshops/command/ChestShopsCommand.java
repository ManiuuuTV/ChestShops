package pl.maniuuu.chestshops.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.maniuuu.chestshops.ChestShopsPlugin;
import pl.maniuuu.chestshops.shop.Shop;
import pl.maniuuu.chestshops.shop.ShopService;
import pl.maniuuu.chestshops.util.Text;

import java.util.List;

public final class ChestShopsCommand {

    private final ChestShopsPlugin plugin;
    private final ShopService shops;

    public ChestShopsCommand(ChestShopsPlugin plugin, ShopService shops) {
        this.plugin = plugin;
        this.shops = shops;
    }

    public void register(Commands registrar) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("chestshops")
                .then(Commands.literal("reload")
                        .requires(source -> source.getSender().hasPermission("chestshops.admin"))
                        .executes(this::reload))
                .then(Commands.literal("info").executes(context -> withTargetShop(context, (player, shop) -> {
                    shops.info(player, shop);
                    return 1;
                })))
                .then(Commands.literal("remove").executes(context -> withTargetShop(context, (player, shop) -> {
                    if (!shops.canModify(player, shop)) {
                        shops.messages().send(player, "error.not-your-shop");
                        return 0;
                    }
                    shops.manager().remove(shop);
                    shops.messages().send(player, "shop.removed");
                    return 1;
                })))
                .then(Commands.literal("list").executes(this::list))
                .then(Commands.literal("price")
                        .then(Commands.argument("buy", DoubleArgumentType.doubleArg(-1))
                                .then(Commands.argument("sell", DoubleArgumentType.doubleArg(-1))
                                        .executes(this::price))))
                .then(Commands.literal("amount")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 2304))
                                .executes(this::amount)))
                .then(Commands.literal("menu").executes(context -> withTargetShop(context, (player, shop) -> {
                    shops.openMenu(player, shop);
                    return 1;
                })))
                .then(Commands.literal("stats").executes(this::stats))
                .then(Commands.literal("find")
                        .then(Commands.argument("przedmiot", ArgumentTypes.itemStack())
                                .executes(this::find)))
                .then(Commands.literal("balance").executes(this::balance))
                .executes(this::help);
        registrar.register(root.build(), "Sklepy graczy na skrzynkach", List.of("cshop", "cs"));
    }

    private int help(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        for (String line : List.of("help.header", "help.create", "help.info", "help.menu", "help.remove",
                "help.price", "help.amount", "help.list", "help.find", "help.stats", "help.balance",
                "help.reload")) {
            sender.sendMessage(shops.messages().get(line));
        }
        return 1;
    }

    private int reload(CommandContext<CommandSourceStack> context) {
        plugin.reload();
        shops.messages().send(context.getSource().getSender(), "plugin.reloaded");
        return 1;
    }

    private int list(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getSender() instanceof Player player)) {
            return 0;
        }
        List<Shop> owned = shops.manager().byOwner(player.getUniqueId());
        shops.messages().send(player, "shop.list-header", Text.number("count", owned.size()));
        for (Shop shop : owned) {
            player.sendMessage(shops.messages().get("shop.list-entry",
                    Text.component("item", shops.itemName(shop.item())),
                    Text.number("amount", shop.amount()),
                    Text.component("buy", price(shop.buyPrice())),
                    Text.component("sell", price(shop.sellPrice())),
                    Text.text("location", shop.sign().toString()),
                    Text.number("stock", shops.stock(shop))));
        }
        return 1;
    }

    private int find(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getSender() instanceof Player player)) {
            return 0;
        }
        Material material = context.getArgument("przedmiot", ItemStack.class).getType();
        List<Shop> selling = shops.cheapestOffers(material, 5);
        List<Shop> buying = shops.bestBuyers(material, 5);
        if (selling.isEmpty() && buying.isEmpty()) {
            shops.messages().send(player, "find.empty",
                    Text.component("item", shops.itemName(ItemStack.of(material))));
            return 0;
        }
        shops.messages().send(player, "find.header",
                Text.component("item", shops.itemName(ItemStack.of(material))));
        if (!selling.isEmpty()) {
            player.sendMessage(shops.messages().get("find.selling-header"));
            selling.forEach(shop -> player.sendMessage(offerLine(shop, shop.buyPrice(), shop.unitBuyPrice())));
        }
        if (!buying.isEmpty()) {
            player.sendMessage(shops.messages().get("find.buying-header"));
            buying.forEach(shop -> player.sendMessage(offerLine(shop, shop.sellPrice(), shop.unitSellPrice())));
        }
        return 1;
    }

    private Component offerLine(Shop shop, double price, double unitPrice) {
        return shops.messages().get("find.entry",
                Text.text("owner", shop.ownerName()),
                Text.number("amount", shop.amount()),
                Text.component("price", shops.money(price)),
                Text.component("unit", shops.money(unitPrice)),
                Text.number("stock", shops.stock(shop)),
                Text.text("location", shop.sign().toString()));
    }

    private int stats(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getSender() instanceof Player player)) {
            return 0;
        }
        Block target = player.getTargetBlockExact(6);
        Shop shop = target == null ? null : shops.shopAtSign(target);
        if (shop != null) {
            shops.messages().send(player, "stats.shop",
                    Text.component("item", shops.itemName(shop.item())),
                    Text.number("sold", shop.stats().itemsSold()),
                    Text.number("bought", shop.stats().itemsBought()),
                    Text.component("earned", shops.money(shop.stats().earned())),
                    Text.component("spent", shops.money(shop.stats().spent())),
                    Text.component("profit", shops.money(shop.stats().profit())),
                    Text.number("transactions", shop.stats().transactions()));
            return 1;
        }

        List<Shop> owned = shops.manager().byOwner(player.getUniqueId());
        long sold = owned.stream().mapToLong(entry -> entry.stats().itemsSold()).sum();
        long bought = owned.stream().mapToLong(entry -> entry.stats().itemsBought()).sum();
        double earned = owned.stream().mapToDouble(entry -> entry.stats().earned()).sum();
        double spent = owned.stream().mapToDouble(entry -> entry.stats().spent()).sum();
        long transactions = owned.stream().mapToLong(entry -> entry.stats().transactions()).sum();
        shops.messages().send(player, "stats.total",
                Text.number("shops", owned.size()),
                Text.number("sold", sold),
                Text.number("bought", bought),
                Text.component("earned", shops.money(earned)),
                Text.component("spent", shops.money(spent)),
                Text.component("profit", shops.money(earned - spent)),
                Text.number("transactions", transactions));
        return 1;
    }

    private int balance(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getSender() instanceof Player player)) {
            return 0;
        }
        shops.messages().send(player, "economy.balance",
                Text.component("balance", shops.money(shops.economy().balance(player.getUniqueId()))));
        return 1;
    }

    private int price(CommandContext<CommandSourceStack> context) {
        return withTargetShop(context, (player, shop) -> {
            if (!shops.canModify(player, shop)) {
                shops.messages().send(player, "error.not-your-shop");
                return 0;
            }
            double buy = context.getArgument("buy", Double.class);
            double sell = context.getArgument("sell", Double.class);
            if (buy < 0 && sell < 0) {
                shops.messages().send(player, "error.invalid-price");
                return 0;
            }
            shop.buyPrice(buy);
            shop.sellPrice(sell);
            shops.manager().markDirty();
            shops.render(shop);
            shops.messages().send(player, "shop.updated");
            return 1;
        });
    }

    private int amount(CommandContext<CommandSourceStack> context) {
        return withTargetShop(context, (player, shop) -> {
            if (!shops.canModify(player, shop)) {
                shops.messages().send(player, "error.not-your-shop");
                return 0;
            }
            shop.amount(context.getArgument("amount", Integer.class));
            shops.manager().markDirty();
            shops.render(shop);
            shops.messages().send(player, "shop.updated");
            return 1;
        });
    }

    private int withTargetShop(CommandContext<CommandSourceStack> context, ShopAction action) {
        if (!(context.getSource().getSender() instanceof Player player)) {
            return 0;
        }
        Block target = player.getTargetBlockExact(6);
        Shop shop = target == null ? null : shops.shopAtSign(target);
        if (shop == null) {
            shops.messages().send(player, "error.no-shop-in-sight");
            return 0;
        }
        return action.run(player, shop);
    }

    private Component price(double value) {
        return value < 0 ? Text.parse(shops.settings().disabledPriceText()) : shops.money(value);
    }

    @FunctionalInterface
    private interface ShopAction {
        int run(Player player, Shop shop);
    }
}
