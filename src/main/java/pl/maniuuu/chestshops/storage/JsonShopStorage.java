package pl.maniuuu.chestshops.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.inventory.ItemStack;
import org.slf4j.Logger;
import pl.maniuuu.chestshops.shop.BlockKey;
import pl.maniuuu.chestshops.shop.Shop;
import pl.maniuuu.chestshops.shop.ShopKind;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public final class JsonShopStorage implements ShopStorage {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LIST_TYPE = new TypeToken<List<ShopRecord>>() {
    }.getType();

    private final Path file;
    private final Logger logger;

    public JsonShopStorage(Path file, Logger logger) {
        this.file = file;
        this.logger = logger;
    }

    @Override
    public List<Shop> loadAll() {
        if (!Files.exists(file)) {
            return List.of();
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            List<ShopRecord> records = GSON.fromJson(reader, LIST_TYPE);
            if (records == null) {
                return List.of();
            }
            List<Shop> shops = new ArrayList<>(records.size());
            for (ShopRecord record : records) {
                try {
                    shops.add(record.toShop());
                } catch (RuntimeException exception) {
                    logger.warn("Pomijam uszkodzony wpis sklepu {}", record.id, exception);
                }
            }
            return shops;
        } catch (IOException exception) {
            logger.error("Nie udalo sie wczytac sklepow z {}", file, exception);
            return List.of();
        }
    }

    @Override
    public void saveAll(Collection<Shop> shops) {
        List<ShopRecord> records = shops.stream().map(ShopRecord::from).toList();
        try {
            Files.createDirectories(file.getParent());
            Path temp = file.resolveSibling(file.getFileName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                GSON.toJson(records, LIST_TYPE, writer);
            }
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            logger.error("Nie udalo sie zapisac sklepow do {}", file, exception);
        }
    }

    private static final class ShopRecord {
        String id;
        String owner;
        String ownerName;
        String kind;
        String world;
        int signX;
        int signY;
        int signZ;
        Integer containerX;
        Integer containerY;
        Integer containerZ;
        String item;
        int amount;
        double buyPrice;
        double sellPrice;

        static ShopRecord from(Shop shop) {
            ShopRecord record = new ShopRecord();
            record.id = shop.id().toString();
            record.owner = shop.owner().toString();
            record.ownerName = shop.ownerName();
            record.kind = shop.kind().name();
            record.world = shop.sign().world().toString();
            record.signX = shop.sign().x();
            record.signY = shop.sign().y();
            record.signZ = shop.sign().z();
            if (shop.container() != null) {
                record.containerX = shop.container().x();
                record.containerY = shop.container().y();
                record.containerZ = shop.container().z();
            }
            record.item = Base64.getEncoder().encodeToString(shop.item().serializeAsBytes());
            record.amount = shop.amount();
            record.buyPrice = shop.buyPrice();
            record.sellPrice = shop.sellPrice();
            return record;
        }

        Shop toShop() {
            UUID worldId = UUID.fromString(world);
            BlockKey signKey = new BlockKey(worldId, signX, signY, signZ);
            BlockKey containerKey = containerX == null ? null
                    : new BlockKey(worldId, containerX, containerY, containerZ);
            ItemStack stack = ItemStack.deserializeBytes(Base64.getDecoder().decode(item));
            return new Shop(UUID.fromString(id), UUID.fromString(owner), ownerName, ShopKind.valueOf(kind),
                    signKey, containerKey, stack, amount, buyPrice, sellPrice);
        }
    }
}
