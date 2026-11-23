package tn.nightbeam.betterwitchhuts.menu;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import tn.nightbeam.betterwitchhuts.content.ModContent;
import tn.nightbeam.betterwitchhuts.core.Errand;
import tn.nightbeam.betterwitchhuts.core.Hex;
import tn.nightbeam.betterwitchhuts.core.Objectives;
import tn.nightbeam.betterwitchhuts.core.Pact;
import tn.nightbeam.betterwitchhuts.core.PlayerRecord;
import tn.nightbeam.betterwitchhuts.core.WitchConfig;
import tn.nightbeam.betterwitchhuts.entity.CovenWitch;
import tn.nightbeam.betterwitchhuts.game.Coven;
import tn.nightbeam.betterwitchhuts.game.HexRules;
import tn.nightbeam.betterwitchhuts.game.Records;

/**
 * The Coven menu, opened at a resident witch (services) or from a Grimoire (read only). It has no slots: the server
 * sends the state as data values and the screen sends button clicks, both through vanilla's container packets, so no
 * custom networking is needed.
 */
public class CovenMenu extends AbstractContainerMenu {
    public static final int MODE = 0;
    public static final int STANDING = 1;
    public static final int SLOTS = 2;
    public static final int OFFER = 3;
    public static final int PACT = 6;
    public static final int HEX = 9;
    public static final int HEX_TASK = 12;
    public static final int HEX_PROGRESS = 15;
    public static final int ERRAND_OFFER = 18;
    public static final int ERRAND = 19;
    public static final int ERRAND_PROGRESS = 20;
    public static final int RENOUNCE_COST = 21;
    public static final int LIFT_COST = 22;
    public static final int BOTTLE_COST = 23;
    public static final int POPPET_COST = 24;
    public static final int EMERALDS = 25;
    public static final int OFFERING_ITEMS = 26;
    public static final int OFFERING_LEFT = 27;
    public static final int HUT = 28;
    public static final int MAX_HEXES = 29;
    public static final int PACTS_STRUCK = 30;
    public static final int HEXES_BROKEN = 31;
    public static final int ERRANDS_DONE = 32;
    public static final int VISITED = 33;
    public static final int DATA = 34;

    public static final int MODE_WITCH = 0;
    public static final int MODE_LEDGER = 1;

    public static final int BUTTON_STRIKE = 0;
    public static final int BUTTON_RENOUNCE = 10;
    public static final int BUTTON_LIFT = 20;
    public static final int BUTTON_BOTTLE = 30;
    public static final int BUTTON_POPPET = 31;
    public static final int BUTTON_ACCEPT = 40;
    public static final int BUTTON_TURN_IN = 41;
    public static final int BUTTON_ABANDON = 42;
    public static final int BUTTON_OFFER = 50;

    private final ContainerData data;
    private final ServerPlayer player;
    private final CovenWitch witch;

    /** Client side. */
    public CovenMenu(int containerId, Inventory inventory) {
        super(ModContent.COVEN_MENU.get(), containerId);
        this.data = new SimpleContainerData(DATA);
        this.player = null;
        this.witch = null;
        addDataSlots(data);
    }

    /** Server side; {@code witch} is null for the Grimoire. */
    private CovenMenu(int containerId, ServerPlayer player, CovenWitch witch) {
        super(ModContent.COVEN_MENU.get(), containerId);
        this.player = player;
        this.witch = witch;
        this.data = new Live();
        addDataSlots(data);
    }

    public static void open(ServerPlayer player, CovenWitch witch) {
        Component title = witch != null ? witch.getDisplayName() : Component.translatable("item.better_witch_huts.grimoire");
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new CovenMenu(id, player, witch), title));
    }

    public CovenWitch witch() {
        return witch;
    }

    public int get(int index) {
        return data.get(index);
    }

    @Override
    public boolean clickMenuButton(Player clicker, int id) {
        if (player == null || clicker != player) {
            return false;
        }
        if (witch == null) {
            return false;
        }
        boolean done;
        if (id >= BUTTON_STRIKE && id < BUTTON_STRIKE + 3) {
            done = Coven.strike(player, witch, id - BUTTON_STRIKE);
        } else if (id >= BUTTON_RENOUNCE && id < BUTTON_RENOUNCE + 3) {
            done = Coven.renounce(player, id - BUTTON_RENOUNCE);
        } else if (id >= BUTTON_LIFT && id < BUTTON_LIFT + 3) {
            done = Coven.lift(player, id - BUTTON_LIFT);
        } else {
            done = switch (id) {
                case BUTTON_BOTTLE -> Coven.buyBottle(player);
                case BUTTON_POPPET -> Coven.buyPoppet(player);
                case BUTTON_ACCEPT -> Coven.acceptErrand(player, witch);
                case BUTTON_TURN_IN -> Coven.turnIn(player, witch);
                case BUTTON_ABANDON -> Coven.abandonErrand(player);
                case BUTTON_OFFER -> Coven.offer(player, witch);
                default -> false;
            };
        }
        broadcastChanges();
        return done;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return witch == null || (witch.isAlive() && witch.getTarget() != player && player.distanceToSqr(witch) < 100.0);
    }

    /** Server-side values, read fresh every time the menu checks for changes (each tick while open). */
    private final class Live implements ContainerData {
        @Override
        public int get(int index) {
            PlayerRecord record = Records.get(player);
            return switch (index) {
                case MODE -> witch == null ? MODE_LEDGER : MODE_WITCH;
                case STANDING -> record.standing;
                case SLOTS -> WitchConfig.get().pactSlots(tn.nightbeam.betterwitchhuts.core.Standing.tier(record.standing));
                case OFFER, OFFER + 1, OFFER + 2 -> {
                    if (witch == null) {
                        yield -1;
                    }
                    List<Pact> offers = Coven.offers(player, witch);
                    int i = index - OFFER;
                    yield i < offers.size() ? Pact.index(offers.get(i)) : -1;
                }
                case PACT, PACT + 1, PACT + 2 -> {
                    List<Pact> pacts = record.pactList();
                    int i = index - PACT;
                    yield i < pacts.size() ? Pact.index(pacts.get(i)) : -1;
                }
                case HEX, HEX + 1, HEX + 2 -> {
                    int i = index - HEX;
                    yield i < record.hexes.size() ? Hex.index(Hex.byId(record.hexes.get(i).hex)) : -1;
                }
                case HEX_TASK, HEX_TASK + 1, HEX_TASK + 2 -> {
                    int i = index - HEX_TASK;
                    yield i < record.hexes.size() ? Objectives.index(Objectives.byId(record.hexes.get(i).task)) : -1;
                }
                case HEX_PROGRESS, HEX_PROGRESS + 1, HEX_PROGRESS + 2 -> {
                    int i = index - HEX_PROGRESS;
                    yield i < record.hexes.size() ? clampShort(HexRules.progress(player, record.hexes.get(i))) : 0;
                }
                case ERRAND_OFFER -> witch == null ? -1 : Errand.index(Coven.errandOffer(player, witch));
                case ERRAND -> record.errand == null ? -1 : Errand.index(Errand.byId(record.errand.errand));
                case ERRAND_PROGRESS -> clampShort(Coven.errandProgress(player, record));
                case RENOUNCE_COST -> WitchConfig.get().services().renounceCost();
                case LIFT_COST -> Coven.liftCost(player);
                case BOTTLE_COST -> Coven.bottleCost(player);
                case POPPET_COST -> Coven.poppetCost(player);
                case EMERALDS -> player.getAbilities().instabuild ? 9999 : clampShort(Coven.emeralds(player));
                case OFFERING_ITEMS -> clampShort(Coven.offeringItems(player));
                case OFFERING_LEFT -> Coven.offeringStandingLeft(player);
                case HUT -> witch == null ? -1 : WitchConfig.STRUCTURES.indexOf(witch.hut());
                case MAX_HEXES -> WitchConfig.get().hexing().maxHexes();
                case PACTS_STRUCK -> clampShort(record.pactsStruck);
                case HEXES_BROKEN -> clampShort(record.hexesBroken);
                case ERRANDS_DONE -> clampShort(record.errandsDone);
                case VISITED -> record.visited.size();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DATA;
        }
    }

    /** Menu data travels as shorts. */
    private static int clampShort(int value) {
        return Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, value));
    }
}
