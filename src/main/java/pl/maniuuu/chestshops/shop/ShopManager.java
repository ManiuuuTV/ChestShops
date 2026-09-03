package pl.maniuuu.chestshops.shop;

import org.slf4j.Logger;
import pl.maniuuu.chestshops.storage.ShopStorage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ShopManager {

    private final Map<BlockKey, Shop> shopsBySign = new ConcurrentHashMap<>();
    private final Map<BlockKey, Shop> shopsByContainer = new ConcurrentHashMap<>();
    private final ShopStorage storage;
    private final Logger logger;
    private final AtomicBoolean dirty = new AtomicBoolean();

    public ShopManager(ShopStorage storage, Logger logger) {
        this.storage = storage;
        this.logger = logger;
    }

    public void load() {
        shopsBySign.clear();
        shopsByContainer.clear();
        for (Shop shop : storage.loadAll()) {
            index(shop);
        }
        logger.info("Wczytano {} sklepow.", shopsBySign.size());
    }

    public void save() {
        storage.saveAll(List.copyOf(shopsBySign.values()));
        dirty.set(false);
    }

    public void saveIfDirty() {
        if (dirty.compareAndSet(true, false)) {
            storage.saveAll(List.copyOf(shopsBySign.values()));
        }
    }

    public void markDirty() {
        dirty.set(true);
    }

    public void add(Shop shop) {
        index(shop);
        markDirty();
    }

    public void remove(Shop shop) {
        shopsBySign.remove(shop.sign());
        if (shop.container() != null) {
            shopsByContainer.remove(shop.container());
        }
        markDirty();
    }

    public Shop bySign(BlockKey key) {
        return shopsBySign.get(key);
    }

    public Shop byContainer(BlockKey key) {
        return shopsByContainer.get(key);
    }

    public Collection<Shop> all() {
        return List.copyOf(shopsBySign.values());
    }

    public List<Shop> byOwner(UUID owner) {
        List<Shop> result = new ArrayList<>();
        for (Shop shop : shopsBySign.values()) {
            if (shop.owner().equals(owner)) {
                result.add(shop);
            }
        }
        return result;
    }

    public int countOwned(UUID owner) {
        return (int) shopsBySign.values().stream()
                .filter(shop -> shop.kind() == ShopKind.PLAYER && shop.owner().equals(owner))
                .count();
    }

    private void index(Shop shop) {
        shopsBySign.put(shop.sign(), shop);
        if (shop.container() != null) {
            shopsByContainer.put(shop.container(), shop);
        }
    }
}
