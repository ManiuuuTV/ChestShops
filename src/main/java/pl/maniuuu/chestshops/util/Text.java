package pl.maniuuu.chestshops.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public final class Text {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private Text() {
    }

    public static Component parse(String input, TagResolver... resolvers) {
        return MINI_MESSAGE.deserialize(input, resolvers);
    }

    public static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    public static TagResolver text(String key, String value) {
        return Placeholder.unparsed(key, value);
    }

    public static TagResolver component(String key, Component value) {
        return Placeholder.component(key, value);
    }

    public static TagResolver number(String key, Number value) {
        return Placeholder.unparsed(key, String.valueOf(value));
    }
}
