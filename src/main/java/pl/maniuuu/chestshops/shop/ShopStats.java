package pl.maniuuu.chestshops.shop;

/** Lifetime trade counters of a single shop, from the owner's point of view. */
public final class ShopStats {

    private long itemsSold;
    private long itemsBought;
    private double earned;
    private double spent;
    private long transactions;

    public ShopStats() {
        this(0, 0, 0, 0, 0);
    }

    public ShopStats(long itemsSold, long itemsBought, double earned, double spent, long transactions) {
        this.itemsSold = itemsSold;
        this.itemsBought = itemsBought;
        this.earned = earned;
        this.spent = spent;
        this.transactions = transactions;
    }

    public void recordSale(int items, double price) {
        itemsSold += items;
        earned += price;
        transactions++;
    }

    public void recordPurchase(int items, double price) {
        itemsBought += items;
        spent += price;
        transactions++;
    }

    public long itemsSold() {
        return itemsSold;
    }

    public long itemsBought() {
        return itemsBought;
    }

    public double earned() {
        return earned;
    }

    public double spent() {
        return spent;
    }

    public long transactions() {
        return transactions;
    }

    public double profit() {
        return earned - spent;
    }
}
