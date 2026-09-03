package pl.maniuuu.chestshops.shop;

import org.bukkit.event.block.Action;

import java.util.Locale;

public enum ClickAction {
    LEFT_CLICK,
    RIGHT_CLICK,
    SHIFT_LEFT_CLICK,
    SHIFT_RIGHT_CLICK,
    DISABLED;

    public static ClickAction parse(String raw) {
        if (raw == null) {
            return DISABLED;
        }
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return DISABLED;
        }
    }

    public static ClickAction of(Action action, boolean sneaking) {
        return switch (action) {
            case LEFT_CLICK_BLOCK -> sneaking ? SHIFT_LEFT_CLICK : LEFT_CLICK;
            case RIGHT_CLICK_BLOCK -> sneaking ? SHIFT_RIGHT_CLICK : RIGHT_CLICK;
            default -> DISABLED;
        };
    }
}
