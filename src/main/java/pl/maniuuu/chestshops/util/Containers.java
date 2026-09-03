package pl.maniuuu.chestshops.util;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class Containers {

    private Containers() {
    }

    /** Container the sign is mounted on (wall sign) or standing on (post sign). */
    public static Block attachedContainer(Block signBlock) {
        BlockData data = signBlock.getBlockData();
        Block candidate = data instanceof WallSign wallSign
                ? signBlock.getRelative(wallSign.getFacing().getOppositeFace())
                : signBlock.getRelative(BlockFace.DOWN);
        return isContainer(candidate) ? candidate : null;
    }

    public static boolean isContainer(Block block) {
        return block != null && block.getState(false) instanceof Container;
    }

    /** Both halves of a double chest, or an empty list for any other container. */
    public static List<Block> doubleChestHalves(Block block) {
        if (!(block.getState(false) instanceof Chest chest)
                || !(chest.getInventory().getHolder(false) instanceof DoubleChest doubleChest)) {
            return List.of();
        }
        List<Block> halves = new ArrayList<>(2);
        if (doubleChest.getLeftSide(false) instanceof Chest left) {
            halves.add(left.getBlock());
        }
        if (doubleChest.getRightSide(false) instanceof Chest right) {
            halves.add(right.getBlock());
        }
        halves.removeIf(half -> half.equals(block));
        return halves;
    }

    public static Inventory inventoryOf(Block block) {
        return block != null && block.getState(false) instanceof Container container ? container.getInventory() : null;
    }

    public static int count(Inventory inventory, ItemStack template) {
        if (inventory == null) {
            return 0;
        }
        int total = 0;
        for (ItemStack content : inventory.getStorageContents()) {
            if (content != null && content.isSimilar(template)) {
                total += content.getAmount();
            }
        }
        return total;
    }

    /** How many items matching the template still fit in the inventory. */
    public static int freeSpace(Inventory inventory, ItemStack template) {
        if (inventory == null) {
            return 0;
        }
        int maxStack = template.getMaxStackSize();
        int space = 0;
        for (ItemStack content : inventory.getStorageContents()) {
            if (content == null || content.getType().isAir()) {
                space += maxStack;
            } else if (content.isSimilar(template)) {
                space += Math.max(0, maxStack - content.getAmount());
            }
        }
        return space;
    }

    /** Removes exactly {@code amount} matching items, returning false (without changes) if there are not enough. */
    public static boolean removeExact(Inventory inventory, ItemStack template, int amount) {
        if (count(inventory, template) < amount) {
            return false;
        }
        int remaining = amount;
        ItemStack[] contents = inventory.getStorageContents();
        for (int slot = 0; slot < contents.length && remaining > 0; slot++) {
            ItemStack content = contents[slot];
            if (content == null || !content.isSimilar(template)) {
                continue;
            }
            int taken = Math.min(remaining, content.getAmount());
            remaining -= taken;
            if (content.getAmount() == taken) {
                contents[slot] = null;
            } else {
                content.setAmount(content.getAmount() - taken);
            }
        }
        inventory.setStorageContents(contents);
        return true;
    }

    /** Splits {@code amount} items into stack-sized {@link ItemStack}s. */
    public static ItemStack[] split(ItemStack template, int amount) {
        int maxStack = template.getMaxStackSize();
        int stacks = (amount + maxStack - 1) / maxStack;
        ItemStack[] result = new ItemStack[stacks];
        int remaining = amount;
        for (int index = 0; index < stacks; index++) {
            int size = Math.min(maxStack, remaining);
            ItemStack stack = template.clone();
            stack.setAmount(size);
            result[index] = stack;
            remaining -= size;
        }
        return result;
    }

    public static void updateSign(BlockState state) {
        state.update(true, false);
    }
}
