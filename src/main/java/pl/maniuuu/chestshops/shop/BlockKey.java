package pl.maniuuu.chestshops.shop;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.UUID;

public record BlockKey(UUID world, int x, int y, int z) {

    public static BlockKey of(Block block) {
        return new BlockKey(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
    }

    public static BlockKey of(Location location) {
        return new BlockKey(location.getWorld().getUID(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public Location toLocation() {
        World bukkitWorld = Bukkit.getWorld(world);
        return bukkitWorld == null ? null : new Location(bukkitWorld, x, y, z);
    }

    public Block toBlock() {
        World bukkitWorld = Bukkit.getWorld(world);
        return bukkitWorld == null ? null : bukkitWorld.getBlockAt(x, y, z);
    }

    @Override
    public String toString() {
        return x + ", " + y + ", " + z;
    }
}
