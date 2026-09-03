# ChestShops

Sklepy graczy na skrzynkach dla serwerów survival na silniku **Purpur / Paper 26.2**, napisane w **Javie 25**.
Cała warstwa tekstowa (wiadomości, tabliczki, waluta) jest w składni **MiniMessage**, więc gradienty,
hovery i kolory RGB konfiguruje się bez dotykania kodu.

## Wymagania

- Purpur lub Paper 26.2+
- Java 25
- Vault + dowolny plugin ekonomii (opcjonalnie — bez Vaulta plugin używa własnej, wbudowanej ekonomii)

## Zakładanie sklepu

Postaw skrzynię (lub beczkę / shulkera), a na niej albo obok niej tabliczkę:

```
[sklep]        <- slowo kluczowe (konfigurowalne: shop, sklep, adminshop, adminsklep)
16             <- ilosc towaru w jednej transakcji (puste = 1)
B 100 S 40     <- B = cena kupna przez gracza, S = cena skupu; mozna podac tylko jedna
DIAMOND        <- przedmiot; puste = przedmiot trzymany w rece (z NBT)
```

Po zatwierdzeniu tabliczka jest przerysowywana według szablonu z `config.yml` (`sign.player` / `sign.admin`).

Domyślne akcje (konfigurowalne w `interaction`):

| Akcja | Efekt |
| --- | --- |
| lewy klik | kupno |
| prawy klik | sprzedaż |
| shift + lewy klik | informacje o sklepie |

Sklepy admina (`[adminsklep]`) nie potrzebują skrzyni — mają nieskończony towar i nie ruszają salda właściciela.

## Komendy

| Komenda | Opis |
| --- | --- |
| `/cshop` | pomoc |
| `/cshop info` | szczegóły sklepu, na który patrzysz |
| `/cshop remove` | usunięcie sklepu |
| `/cshop price <kupno> <sprzedaz>` | zmiana cen (`-1` wyłącza stronę transakcji) |
| `/cshop amount <ilosc>` | zmiana ilości towaru |
| `/cshop list` | lista twoich sklepów |
| `/cshop balance` | stan konta |
| `/cshop reload` | przeładowanie konfiguracji |

Aliasy: `/chestshops`, `/cshop`, `/cs`.

## Uprawnienia

| Uprawnienie | Domyślnie | Opis |
| --- | --- | --- |
| `chestshops.use` | wszyscy | handel w sklepach |
| `chestshops.create` | wszyscy | zakładanie sklepów |
| `chestshops.admin` | op | sklepy admina, `/cshop reload`, omijanie ochrony |
| `chestshops.limit.bypass` | op | omijanie limitu sklepów |

## Ochrona

- skrzynia i tabliczka sklepu są chronione przed rozbiciem przez innych graczy,
- lejek postawiony przy cudzej skrzyni sklepowej jest blokowany (`protection.hoppers`),
- eksplozje nie niszczą sklepów (`protection.explosions`).

## Dane

- `plugins/ChestShops/shops.json` — sklepy (zapis atomowy, autozapis co `storage.auto-save-seconds`),
- `plugins/ChestShops/economy.json` — salda wbudowanej ekonomii (tylko bez Vaulta),
- `plugins/ChestShops/transactions.log` — log transakcji.

## Budowanie

```bash
./gradlew build      # jar w build/libs/ChestShops-<wersja>.jar
```
