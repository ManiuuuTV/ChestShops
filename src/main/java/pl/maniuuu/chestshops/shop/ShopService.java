package pl.maniuuu.chestshops.shop;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import pl.maniuuu.chestshops.ChestShopsPlugin;
import pl.maniuuu.chestshops.config.Messages;
import pl.maniuuu.chestshops.config.ShopSettings;
import pl.maniuuu.chestshops.economy.EconomyService;
import pl.maniuuu.chestshops.util.Containers;
import pl.maniuuu.chestshops.util.Text;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ShopService {

    private static final Pattern BUY_PATTERN = Pattern.compile("(?i)b\\s*:?\\s*(\\d+(?:[.,]\\d+)?)");
    private static final Pattern SELL_PATTERN = Pattern.compile("(?i)s\\s*:?\\s*(\\d+(?:[.,]\\d+)?)");
    private static final Pattern PLAIN_PRICE = Pattern.compile("^\\s*(\\d+(?:[.,]\\d+)?)\\s*$");
    private static final DateTimeFormatter LOG_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ChestShopsPlugin plugin;
    private final ShopManager manager;
    private final EconomyService economy;
    private Messages messages;
    private ShopSettings settings;

    public ShopService(ChestShopsPlugin plugin, ShopManager manager, EconomyService economy,
                       Messages messages, ShopSettings settings) {
        this.plugin = plugin;
        this.manager = manager;
        this.economy = economy;
        this.messages = messages;
        this.settings = settings;
    }

    public void reload(ShopSettings settings, Messages messages) {
        this.settings = settings;
        this.messages = messages;
        manager.all().forEach(this::render);
    }

    public ShopManager manager() {
        return manager;
    }

    public Messages messages() {
        return messages;
    }

    public ShopSettings settings() {
        return settings;
    }

    public EconomyService economy() {
        return economy;
    }

    public Shop shopAtSign(Block block) {
        return manager.bySign(BlockKey.of(block));
    }

    public Shop shopAtContainer(Block block) {
        Shop shop = manager.byContainer(BlockKey.of(block));
        if (shop != null) {
            return shop;
        }
        for (Block half : Containers.doubleChestHalves(block)) {
            Shop paired = manager.byContainer(BlockKey.of(half));
            if (paired != null) {
                return paired;
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- creation

    /** Parses the freshly written sign; returns the created shop or {@code null} when creation failed. */
    public Shop create(Player player, Block signBlock, List<Component> lines) {
        String keyword = Text.plain(lines.get(0)).replace("[", "").replace("]", "").trim();
        boolean admin = settings.isAdminKeyword(keyword);
        if (!admin && !settings.isShopKeyword(keyword)) {
            return null;
        }
        if (admin && !player.hasPermission("chestshops.admin")) {
            messages.send(player, "error.no-permission");
            return null;
        }
        if (!player.hasPermission("chestshops.create")) {
            messages.send(player, "error.no-permission");
            return null;
        }

        Block containerBlock = admin ? null : Containers.attachedContainer(signBlock);
        if (!admin && containerBlock == null) {
            messages.send(player, "error.no-container");
            return null;
        }
        if (!admin) {
            Shop existing = manager.byContainer(BlockKey.of(containerBlock));
            if (existing != null) {
                messages.send(player, "error.container-taken");
                return null;
            }
        }

        int limit = settings.maxShopsPerPlayer();
        if (!admin && limit > 0 && manager.countOwned(player.getUniqueId()) >= limit
                && !player.hasPermission("chestshops.limit.bypass")) {
            messages.send(player, "error.limit-reached", Text.number("limit", limit));
            return null;
        }

        ItemStack item = resolveItem(player, Text.plain(lines.get(3)));
        if (item == null) {
            messages.send(player, "error.unknown-item", Text.text("input", Text.plain(lines.get(3))));
            return null;
        }

        int amount = parseAmount(Text.plain(lines.get(1)), item);
        if (amount <= 0) {
            messages.send(player, "error.invalid-amount");
            return null;
        }

        String priceLine = Text.plain(lines.get(2));
        double buyPrice = parsePrice(priceLine, BUY_PATTERN);
        double sellPrice = parsePrice(priceLine, SELL_PATTERN);
        Matcher plain = PLAIN_PRICE.matcher(priceLine);
        if (buyPrice < 0 && sellPrice < 0 && plain.matches()) {
            buyPrice = Double.parseDouble(plain.group(1).replace(',', '.'));
        }
        if (buyPrice < 0 && sellPrice < 0) {
            messages.send(player, "error.invalid-price");
            return null;
        }

        double fee = admin ? 0 : settings.creationFee();
        if (fee > 0 && !economy.withdraw(player.getUniqueId(), fee)) {
            messages.send(player, "error.no-money-fee", Text.component("price", money(fee)));
            return null;
        }

        Shop shop = new Shop(UUID.randomUUID(), player.getUniqueId(), player.getName(),
                admin ? ShopKind.ADMIN : ShopKind.PLAYER, BlockKey.of(signBlock),
                containerBlock == null ? null : BlockKey.of(containerBlock),
                item, amount, buyPrice, sellPrice);
        manager.add(shop);

        messages.send(player, "shop.created",
                Text.component("item", itemName(item)),
                Text.number("amount", amount),
                Text.component("buy", priceOrDash(buyPrice)),
                Text.component("sell", priceOrDash(sellPrice)));
        if (fee > 0) {
            messages.send(player, "shop.fee-charged", Text.component("price", money(fee)));
        }
        return shop;
    }

    public void render(Shop shop) {
        Block block = shop.sign().toBlock();
        if (block == null || !(block.getState(false) instanceof Sign sign)) {
            return;
        }
        List<String> layout = shop.admin() ? settings.adminSignLayout() : settings.signLayout();
        TagResolver[] resolvers = signResolvers(shop);
        for (int index = 0; index < 4; index++) {
            String line = index < layout.size() ? layout.get(index) : "";
            sign.getSide(Side.FRONT).line(index, Text.parse(line, resolvers));
        }
        Containers.updateSign(sign);
    }

    private TagResolver[] signResolvers(Shop shop) {
        return new TagResolver[]{
                Text.text("owner", shop.ownerName()),
                Text.component("item", itemName(shop.item())),
                Text.number("amount", shop.amount()),
                Text.component("buy", priceOrDash(shop.buyPrice())),
                Text.component("sell", priceOrDash(shop.sellPrice())),
                Text.number("stock", stock(shop))
        };
    }

    // ------------------------------------------------------------- transactions

    public void buy(Player player, Shop shop) {
        if (!shop.buyEnabled()) {
            messages.send(player, "error.buy-disabled");
            return;
        }
        if (isOwner(player, shop) && !shop.admin()) {
            messages.send(player, "error.own-shop");
            return;
        }
        ItemStack template = shop.item();
        int amount = shop.amount();
        Inventory container = shop.admin() ? null : Containers.inventoryOf(containerBlock(shop));
        if (!shop.admin() && container == null) {
            messages.send(player, "error.container-missing");
            return;
        }
        if (!shop.admin() && Containers.count(container, template) < amount) {
            messages.send(player, "error.out-of-stock");
            return;
        }
        if (Containers.freeSpace(player.getInventory(), template) < amount) {
            messages.send(player, "error.player-inventory-full");
            return;
        }
        double price = shop.buyPrice();
        if (!economy.withdraw(player.getUniqueId(), price)) {
            messages.send(player, "error.no-money", Text.component("price", money(price)));
            return;
        }
        if (!shop.admin() && !Containers.removeExact(container, template, amount)) {
            economy.deposit(player.getUniqueId(), price);
            messages.send(player, "error.out-of-stock");
            return;
        }
        if (!shop.admin()) {
            economy.deposit(shop.owner(), price);
        }
        player.getInventory().addItem(Containers.split(template, amount));

        messages.send(player, "transaction.bought",
                Text.number("amount", amount),
                Text.component("item", itemName(template)),
                Text.component("price", money(price)),
                Text.text("owner", shop.ownerName()));
        notifyOwner(shop, "transaction.owner-sold",
                Text.text("player", player.getName()),
                Text.number("amount", amount),
                Text.component("item", itemName(template)),
                Text.component("price", money(price)));
        render(shop);
        manager.markDirty();
        log(shop, player, "BUY", amount, price);
    }

    public void sell(Player player, Shop shop) {
        if (!shop.sellEnabled()) {
            messages.send(player, "error.sell-disabled");
            return;
        }
        if (isOwner(player, shop) && !shop.admin()) {
            messages.send(player, "error.own-shop");
            return;
        }
        ItemStack template = shop.item();
        int amount = shop.amount();
        if (Containers.count(player.getInventory(), template) < amount) {
            messages.send(player, "error.player-no-items",
                    Text.number("amount", amount),
                    Text.component("item", itemName(template)));
            return;
        }
        Inventory container = shop.admin() ? null : Containers.inventoryOf(containerBlock(shop));
        if (!shop.admin() && container == null) {
            messages.send(player, "error.container-missing");
            return;
        }
        if (!shop.admin() && Containers.freeSpace(container, template) < amount) {
            messages.send(player, "error.shop-full");
            return;
        }
        double price = shop.sellPrice();
        if (!shop.admin() && !economy.has(shop.owner(), price)) {
            messages.send(player, "error.owner-no-money");
            return;
        }
        if (!Containers.removeExact(player.getInventory(), template, amount)) {
            messages.send(player, "error.player-no-items",
                    Text.number("amount", amount),
                    Text.component("item", itemName(template)));
            return;
        }
        if (!shop.admin()) {
            economy.withdraw(shop.owner(), price);
            container.addItem(Containers.split(template, amount));
        }
        economy.deposit(player.getUniqueId(), price);

        messages.send(player, "transaction.sold",
                Text.number("amount", amount),
                Text.component("item", itemName(template)),
                Text.component("price", money(price)),
                Text.text("owner", shop.ownerName()));
        notifyOwner(shop, "transaction.owner-bought",
                Text.text("player", player.getName()),
                Text.number("amount", amount),
                Text.component("item", itemName(template)),
                Text.component("price", money(price)));
        render(shop);
        manager.markDirty();
        log(shop, player, "SELL", amount, price);
    }

    public void info(Player player, Shop shop) {
        messages.send(player, "shop.info-header");
        player.sendMessage(messages.get("shop.info-body",
                Text.text("owner", shop.ownerName()),
                Text.text("kind", shop.admin() ? "admin" : "gracz"),
                Text.number("amount", shop.amount()),
                Text.component("item", itemName(shop.item())),
                Text.component("buy", priceOrDash(shop.buyPrice())),
                Text.component("sell", priceOrDash(shop.sellPrice())),
                Text.number("stock", stock(shop)),
                Text.text("location", shop.sign().toString())));
    }

    // ------------------------------------------------------------------ helpers

    public boolean isOwner(Player player, Shop shop) {
        return shop.owner().equals(player.getUniqueId());
    }

    public boolean canModify(Player player, Shop shop) {
        return isOwner(player, shop) || player.hasPermission("chestshops.admin");
    }

    public int stock(Shop shop) {
        if (shop.admin()) {
            return -1;
        }
        return Containers.count(Containers.inventoryOf(containerBlock(shop)), shop.item());
    }

    public Component money(double amount) {
        return Text.parse(String.format(Locale.ROOT, settings.currencyFormat(), amount));
    }

    public Component itemName(ItemStack item) {
        return item.effectiveName();
    }

    private Component priceOrDash(double price) {
        return price < 0 ? Text.parse(settings.disabledPriceText()) : money(price);
    }

    private Block containerBlock(Shop shop) {
        return shop.container() == null ? null : shop.container().toBlock();
    }

    private void notifyOwner(Shop shop, String key, TagResolver... resolvers) {
        if (shop.admin()) {
            return;
        }
        Player owner = Bukkit.getPlayer(shop.owner());
        if (owner != null && owner.isOnline()) {
            messages.send(owner, key, resolvers);
        }
    }

    private ItemStack resolveItem(Player player, String rawLine) {
        String raw = rawLine.trim();
        if (raw.isEmpty()) {
            ItemStack inHand = player.getInventory().getItemInMainHand();
            if (inHand.getType().isAir()) {
                return null;
            }
            return inHand.asOne();
        }
        Material material = Material.matchMaterial(raw);
        if (material == null || !material.isItem()) {
            return null;
        }
        return ItemStack.of(material);
    }

    private int parseAmount(String rawLine, ItemStack item) {
        String raw = rawLine.trim();
        if (raw.isEmpty()) {
            return 1;
        }
        try {
            int parsed = Integer.parseInt(raw);
            return Math.min(parsed, item.getMaxStackSize() * 36);
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    private double parsePrice(String line, Pattern pattern) {
        Matcher matcher = pattern.matcher(line);
        return matcher.find() ? Double.parseDouble(matcher.group(1).replace(',', '.')) : -1;
    }

    private void log(Shop shop, Player player, String type, int amount, double price) {
        if (!settings.logTransactions()) {
            return;
        }
        String line = "%s %s %s %dx %s za %.2f (sklep %s / %s)%n".formatted(
                LocalDateTime.now().format(LOG_TIME), player.getName(), type, amount,
                shop.item().getType().getKey().asString(), price, shop.ownerName(), shop.sign());
        Path file = plugin.getDataPath().resolve("transactions.log");
        plugin.getServer().getAsyncScheduler().runNow(plugin, task -> {
            try {
                Files.createDirectories(file.getParent());
                Files.writeString(file, line, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException exception) {
                plugin.getSLF4JLogger().warn("Nie udalo sie zapisac logu transakcji", exception);
            }
        });
    }

    public OfflinePlayer ownerOf(Shop shop) {
        return Bukkit.getOfflinePlayer(shop.owner());
    }
}
