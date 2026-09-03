package pl.maniuuu.chestshops.storage;

import pl.maniuuu.chestshops.shop.Shop;

import java.util.Collection;
import java.util.List;

public interface ShopStorage {

    List<Shop> loadAll();

    void saveAll(Collection<Shop> shops);
}
