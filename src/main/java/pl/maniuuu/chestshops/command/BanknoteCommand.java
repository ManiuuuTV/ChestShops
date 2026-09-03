package pl.maniuuu.chestshops.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;
import pl.maniuuu.chestshops.economy.BanknoteService;
import pl.maniuuu.chestshops.shop.ShopService;

import java.util.List;

/** {@code /wyplac <kwota>} and {@code /wplac} — cash in and out of the account. */
public final class BanknoteCommand {

    private final ShopService shops;
    private final BanknoteService banknotes;

    public BanknoteCommand(ShopService shops, BanknoteService banknotes) {
        this.shops = shops;
        this.banknotes = banknotes;
    }

    public void register(Commands registrar) {
        registrar.register(Commands.literal("wyplac")
                        .requires(source -> source.getSender().hasPermission("chestshops.banknote"))
                        .then(Commands.argument("kwota", DoubleArgumentType.doubleArg(0.01))
                                .executes(this::withdraw))
                        .executes(this::usage)
                        .build(),
                "Wyplaca pieniadze z konta na banknot", List.of("withdraw"));

        registrar.register(Commands.literal("wplac")
                        .requires(source -> source.getSender().hasPermission("chestshops.banknote"))
                        .executes(this::deposit)
                        .build(),
                "Wplaca trzymany banknot na konto", List.of("deposit"));
    }

    private int usage(CommandContext<CommandSourceStack> context) {
        shops.messages().send(context.getSource().getSender(), "help.withdraw");
        return 1;
    }

    private int withdraw(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getSender() instanceof Player player)) {
            return 0;
        }
        return banknotes.withdraw(player, context.getArgument("kwota", Double.class)) ? 1 : 0;
    }

    private int deposit(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getSender() instanceof Player player)) {
            return 0;
        }
        return banknotes.deposit(player) ? 1 : 0;
    }
}
