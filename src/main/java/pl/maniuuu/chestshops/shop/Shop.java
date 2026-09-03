package pl.maniuuu.chestshops.shop;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public final class Shop {

    private final UUID id;
    private final UUID owner;
    private final String ownerName;
    private final ShopKind kind;
    private final BlockKey sign;
    private final BlockKey container;
    private final ItemStack item;
    private int amount;
    private double buyPrice;
    private double sellPrice;
    private final ShopStats stats;

    public Shop(UUID id, UUID owner, String ownerName, ShopKind kind, BlockKey sign, BlockKey container,
                ItemStack item, int amount, double buyPrice, double sellPrice) {
        this(id, owner, ownerName, kind, sign, container, item, amount, buyPrice, sellPrice, new ShopStats());
    }

    public Shop(UUID id, UUID owner, String ownerName, ShopKind kind, BlockKey sign, BlockKey container,
                ItemStack item, int amount, double buyPrice, double sellPrice, ShopStats stats) {
        this.id = id;
        this.owner = owner;
        this.ownerName = ownerName;
        this.kind = kind;
        this.sign = sign;
        this.container = container;
        this.item = item.clone();
        this.amount = amount;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
        this.stats = stats;
    }

    public ShopStats stats() {
        return stats;
    }

    public UUID id() {
        return id;
    }

    public UUID owner() {
        return owner;
    }

    public String ownerName() {
        return ownerName;
    }

    public ShopKind kind() {
        return kind;
    }

    public boolean admin() {
        return kind == ShopKind.ADMIN;
    }

    public BlockKey sign() {
        return sign;
    }

    public BlockKey container() {
        return container;
    }

    /** A single-unit template of the traded item; trade volume is {@link #amount()}. */
    public ItemStack item() {
        return item.clone();
    }

    public int amount() {
        return amount;
    }

    public void amount(int amount) {
        this.amount = amount;
    }

    public double buyPrice() {
        return buyPrice;
    }

    public void buyPrice(double buyPrice) {
        this.buyPrice = buyPrice;
    }

    public double sellPrice() {
        return sellPrice;
    }

    public void sellPrice(double sellPrice) {
        this.sellPrice = sellPrice;
    }

    public boolean buyEnabled() {
        return buyPrice >= 0;
    }

    public boolean sellEnabled() {
        return sellPrice >= 0;
    }

    /** Price of a single item, used for comparing offers across shops. */
    public double unitBuyPrice() {
        return buyEnabled() ? buyPrice / amount : Double.MAX_VALUE;
    }

    public double unitSellPrice() {
        return sellEnabled() ? sellPrice / amount : -1;
    }

    public ItemStack tradeStack() {
        ItemStack stack = item.clone();
        stack.setAmount(amount);
        return stack;
    }
}
