package pl.maniuuu.chestshops.economy;

import java.util.UUID;

public interface EconomyService {

    String name();

    double balance(UUID player);

    boolean has(UUID player, double amount);

    boolean withdraw(UUID player, double amount);

    boolean deposit(UUID player, double amount);
}
