package tn.nightbeam.betterwitchhuts.game;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import tn.nightbeam.betterwitchhuts.Constants;
import tn.nightbeam.betterwitchhuts.Witchbound;
import tn.nightbeam.betterwitchhuts.content.ModContent;
import tn.nightbeam.betterwitchhuts.core.Draft;
import tn.nightbeam.betterwitchhuts.core.Errand;
import tn.nightbeam.betterwitchhuts.core.Hex;
import tn.nightbeam.betterwitchhuts.core.Objective;
import tn.nightbeam.betterwitchhuts.core.Pact;
import tn.nightbeam.betterwitchhuts.core.PlayerRecord;
import tn.nightbeam.betterwitchhuts.core.Standing;
import tn.nightbeam.betterwitchhuts.core.WitchConfig;
import tn.nightbeam.betterwitchhuts.entity.CovenWitch;

/** Coven Standing and every service a resident witch offers. All of it runs on the server. */
public final class Coven {
    /** Items a witch accepts as an offering: the things witches themselves carry. */
    public static final List<net.minecraft.world.item.Item> OFFERINGS = List.of(Items.SPIDER_EYE, Items.SUGAR, Items.GLOWSTONE_DUST,
            Items.REDSTONE, Items.GLASS_BOTTLE, Items.FERMENTED_SPIDER_EYE, Items.NETHER_WART);
    /** Offering items per point of standing. */
    public static final int OFFERING_ITEMS_PER_POINT = 2;
    private static final int MAX_OFFERING_ITEMS = 16;

    private Coven() {
    }

    // Standing ---------------------------------------------------------------------------------------------------------

    public static Standing.Tier tier(ServerPlayer player) {
        return Standing.tier(Records.get(player).standing);
    }

    public static boolean isHunted(ServerPlayer player) {
        return tier(player) == Standing.Tier.HUNTED;
    }

    public static void changeStanding(ServerPlayer player, int delta) {
        if (delta == 0) {
            return;
        }
        PlayerRecord record = Records.get(player);
        Standing.Tier before = Standing.tier(record.standing);
        record.standing = Standing.clamp(record.standing + delta);
        record.dirty = true;
        Standing.Tier after = Standing.tier(record.standing);
        player.displayClientMessage(Component.translatable("message.better_witch_huts.standing.change",
                Component.literal((delta > 0 ? "+" : "") + delta).withStyle(delta > 0 ? ChatFormatting.GREEN : ChatFormatting.RED),
                tierName(after)).withStyle(ChatFormatting.DARK_PURPLE), true);
        if (after != before) {
            boolean up = after.ordinal() > before.ordinal();
            player.sendSystemMessage(Component.translatable(up ? "message.better_witch_huts.standing.up" : "message.better_witch_huts.standing.down",
                    tierName(after)).withStyle(up ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.DARK_RED));
            player.level().playSound(null, player.blockPosition(), up ? SoundEvents.AMETHYST_BLOCK_CHIME : SoundEvents.WITCH_CELEBRATE,
                    SoundSource.PLAYERS, 1.0F, up ? 1.2F : 0.8F);
        }
        awardStanding(player, after);
    }

    public static void awardStanding(ServerPlayer player, Standing.Tier tier) {
        if (tier.atLeast(Standing.Tier.KNOWN)) {
            award(player, "known");
        }
        if (tier.atLeast(Standing.Tier.TRUSTED)) {
            award(player, "trusted");
        }
        if (tier == Standing.Tier.KIN) {
            award(player, "kin");
        }
        if (tier == Standing.Tier.HUNTED) {
            award(player, "hunted");
        }
    }

    public static MutableComponent tierName(Standing.Tier tier) {
        ChatFormatting colour = switch (tier) {
            case HUNTED -> ChatFormatting.DARK_RED;
            case DISTRUSTED -> ChatFormatting.RED;
            case STRANGER -> ChatFormatting.GRAY;
            case KNOWN -> ChatFormatting.GREEN;
            case TRUSTED -> ChatFormatting.AQUA;
            case KIN -> ChatFormatting.LIGHT_PURPLE;
        };
        return Component.translatable("tier.better_witch_huts." + tier.id()).withStyle(colour);
    }

    public static long day(ServerLevel level) {
        return Math.floorDiv(level.getServer().overworld().getDayTime(), 24000L);
    }

    // What a resident offers --------------------------------------------------------------------------------------------

    public static List<Pact> offers(ServerPlayer player, CovenWitch witch) {
        PlayerRecord record = Records.get(player);
        long seed = Draft.seed(witch.getUUID().getLeastSignificantBits(), player.getUUID().getMostSignificantBits(),
                day((ServerLevel) witch.level()));
        return Draft.pacts(seed, record.pactList(), WitchConfig.get().disabledPacts());
    }

    public static Errand errandOffer(ServerPlayer player, CovenWitch witch) {
        long seed = Draft.seed(witch.getUUID().getMostSignificantBits(), player.getUUID().getLeastSignificantBits(),
                day((ServerLevel) witch.level()));
        return Draft.errand(seed);
    }

    public static int freeSlots(ServerPlayer player) {
        PlayerRecord record = Records.get(player);
        return Math.max(0, WitchConfig.get().pactSlots(Standing.tier(record.standing)) - record.pacts.size());
    }

    /** Emeralds to lift one hex at the player's standing; -1 when the witch refuses. Magpie pact: a quarter off. */
    public static int liftCost(ServerPlayer player) {
        PlayerRecord record = Records.get(player);
        Integer cost = WitchConfig.get().services().liftCost().get(Standing.tier(record.standing));
        if (cost == null) {
            return -1;
        }
        return record.hasPact(Pact.MAGPIE) ? (int) Math.floor(cost * 0.75) : cost;
    }

    public static int poppetCost(ServerPlayer player) {
        return tier(player).atLeast(Standing.Tier.KNOWN) ? WitchConfig.get().services().poppetCost() : -1;
    }

    public static int bottleCost(ServerPlayer player) {
        return isHunted(player) ? -1 : WitchConfig.get().services().witchBottleCost();
    }

    // Services ----------------------------------------------------------------------------------------------------------

    public static boolean strike(ServerPlayer player, CovenWitch witch, int offer) {
        PlayerRecord record = Records.get(player);
        List<Pact> offers = offers(player, witch);
        if (offer < 0 || offer >= offers.size()) {
            return false;
        }
        if (freeSlots(player) <= 0) {
            player.sendSystemMessage(Component.translatable("message.better_witch_huts.no_slot").withStyle(ChatFormatting.RED));
            return false;
        }
        Pact pact = offers.get(offer);
        record.pacts.add(pact.id());
        record.pactsStruck++;
        record.dirty = true;
        Effects.sync(player, record);
        ServerLevel level = (ServerLevel) player.level();
        level.sendParticles(ParticleTypes.WITCH, player.getX(), player.getY() + 1, player.getZ(), 40, 0.5, 0.8, 0.5, 0.05);
        level.playSound(null, player.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.8F, 0.7F);
        level.playSound(null, witch.blockPosition(), SoundEvents.WITCH_CELEBRATE, SoundSource.NEUTRAL, 1.0F, 1.0F);
        player.sendSystemMessage(Component.translatable("message.better_witch_huts.pact_struck", pactName(pact)).withStyle(ChatFormatting.LIGHT_PURPLE));
        award(player, "first_pact");
        if (record.pacts.size() >= 3) {
            award(player, "three_pacts");
        }
        return true;
    }

    public static boolean renounce(ServerPlayer player, int index) {
        PlayerRecord record = Records.get(player);
        if (index < 0 || index >= record.pacts.size() || isHunted(player)) {
            return false;
        }
        int cost = WitchConfig.get().services().renounceCost();
        if (!pay(player, cost)) {
            return false;
        }
        Pact pact = Pact.byId(record.pacts.remove(index));
        record.dirty = true;
        Effects.sync(player, record);
        player.level().playSound(null, player.blockPosition(), SoundEvents.CHAIN_BREAK, SoundSource.PLAYERS, 1.0F, 0.8F);
        if (pact != null) {
            player.sendSystemMessage(Component.translatable("message.better_witch_huts.pact_renounced", pactName(pact)).withStyle(ChatFormatting.GRAY));
        }
        changeStanding(player, WitchConfig.get().standing().renounce());
        return true;
    }

    public static boolean lift(ServerPlayer player, int index) {
        PlayerRecord record = Records.get(player);
        int cost = liftCost(player);
        if (cost < 0 || index < 0 || index >= record.hexes.size() || !pay(player, cost)) {
            return false;
        }
        HexRules.remove(player, record, index, "message.better_witch_huts.hex_lifted");
        return true;
    }

    public static boolean buyBottle(ServerPlayer player) {
        int cost = bottleCost(player);
        return cost >= 0 && pay(player, cost) && give(player, new ItemStack(ModContent.WITCH_BOTTLE.get()));
    }

    public static boolean buyPoppet(ServerPlayer player) {
        int cost = poppetCost(player);
        return cost >= 0 && pay(player, cost) && give(player, new ItemStack(ModContent.POPPET.get()));
    }

    public static boolean acceptErrand(ServerPlayer player, CovenWitch witch) {
        PlayerRecord record = Records.get(player);
        if (record.errand != null || isHunted(player)) {
            return false;
        }
        Errand errand = errandOffer(player, witch);
        int baseline = errand.objective().kind() == Objective.Kind.DELIVER ? 0 : Progress.read(player, errand.objective());
        record.errand = new PlayerRecord.ErrandState(errand.id(), baseline, witch.getName().getString());
        record.dirty = true;
        player.level().playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    public static int errandProgress(ServerPlayer player, PlayerRecord record) {
        if (record.errand == null) {
            return 0;
        }
        Errand errand = Errand.byId(record.errand.errand);
        if (errand == null) {
            return 0;
        }
        int value = Progress.read(player, errand.objective());
        return Math.max(0, errand.objective().kind() == Objective.Kind.DELIVER ? value : value - record.errand.baseline);
    }

    public static boolean turnIn(ServerPlayer player, CovenWitch witch) {
        PlayerRecord record = Records.get(player);
        if (record.errand == null) {
            return false;
        }
        Errand errand = Errand.byId(record.errand.errand);
        if (errand == null || errandProgress(player, record) < errand.objective().target()) {
            return false;
        }
        if (errand.objective().kind() == Objective.Kind.DELIVER) {
            Progress.take(player.getInventory(), errand.objective(), errand.objective().target());
        }
        record.errand = null;
        record.errandsDone++;
        record.dirty = true;
        give(player, new ItemStack(Items.EMERALD, errand.emeralds()));
        if (player.getRandom().nextFloat() < 0.25F) {
            give(player, new ItemStack(ModContent.WITCH_BOTTLE.get()));
        }
        ServerLevel level = (ServerLevel) player.level();
        level.playSound(null, witch.blockPosition(), SoundEvents.WITCH_CELEBRATE, SoundSource.NEUTRAL, 1.0F, 1.1F);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, witch.getX(), witch.getY() + 2.2, witch.getZ(), 8, 0.3, 0.2, 0.3, 0);
        player.sendSystemMessage(Component.translatable("message.better_witch_huts.errand_done", witch.getDisplayName(),
                errand.emeralds()).withStyle(ChatFormatting.GREEN));
        changeStanding(player, errand.standing());
        award(player, "first_errand");
        if (record.errandsDone >= 10) {
            award(player, "ten_errands");
        }
        return true;
    }

    public static boolean abandonErrand(ServerPlayer player) {
        PlayerRecord record = Records.get(player);
        if (record.errand == null) {
            return false;
        }
        record.errand = null;
        record.dirty = true;
        changeStanding(player, WitchConfig.get().standing().abandonErrand());
        return true;
    }

    /** Items the player could offer right now, capped to what the day still allows. */
    public static int offeringItems(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        int n = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (OFFERINGS.contains(stack.getItem())) {
                n += stack.getCount();
            }
        }
        return n;
    }

    public static int offeringStandingLeft(ServerPlayer player) {
        PlayerRecord record = Records.get(player);
        long today = day((ServerLevel) player.level());
        int used = record.offeringDay == today ? record.offeringStanding : 0;
        return Math.max(0, WitchConfig.get().standing().offeringDailyCap() - used);
    }

    public static boolean offer(ServerPlayer player, CovenWitch witch) {
        PlayerRecord record = Records.get(player);
        int left = offeringStandingLeft(player);
        if (left <= 0) {
            player.displayClientMessage(Component.translatable("message.better_witch_huts.offering_cap", witch.getDisplayName()), true);
            return false;
        }
        int items = Math.min(Math.min(offeringItems(player), MAX_OFFERING_ITEMS), left * OFFERING_ITEMS_PER_POINT);
        int points = items / OFFERING_ITEMS_PER_POINT;
        if (points <= 0) {
            return false;
        }
        int take = points * OFFERING_ITEMS_PER_POINT;
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize() && take > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (OFFERINGS.contains(stack.getItem())) {
                int used = Math.min(take, stack.getCount());
                stack.shrink(used);
                take -= used;
            }
        }
        long today = day((ServerLevel) player.level());
        if (record.offeringDay != today) {
            record.offeringDay = today;
            record.offeringStanding = 0;
        }
        record.offeringStanding += points;
        record.dirty = true;
        ((ServerLevel) player.level()).sendParticles(ParticleTypes.WITCH, witch.getX(), witch.getY() + 2.0, witch.getZ(), 12, 0.3, 0.3, 0.3, 0.02);
        player.level().playSound(null, witch.blockPosition(), SoundEvents.WITCH_AMBIENT, SoundSource.NEUTRAL, 1.0F, 1.2F);
        player.sendSystemMessage(Component.translatable("message.better_witch_huts.offering", witch.getDisplayName()).withStyle(ChatFormatting.DARK_PURPLE));
        changeStanding(player, points);
        return true;
    }

    // Helpers -----------------------------------------------------------------------------------------------------------

    public static MutableComponent pactName(Pact pact) {
        return Component.translatable("effect.better_witch_huts.pact_" + pact.id());
    }

    public static MutableComponent hexName(Hex hex) {
        return Component.translatable("effect.better_witch_huts.hex_" + hex.id());
    }

    public static MutableComponent objectiveText(Objective objective) {
        return Component.translatable("objective.better_witch_huts." + objective.id(), objective.displayTarget());
    }

    public static int emeralds(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        int n = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(Items.EMERALD)) {
                n += stack.getCount();
            }
        }
        return n;
    }

    /** Takes emeralds; tells the player when they are short. Creative players pay nothing. */
    public static boolean pay(ServerPlayer player, int cost) {
        if (cost <= 0 || player.getAbilities().instabuild) {
            return true;
        }
        if (emeralds(player) < cost) {
            player.sendSystemMessage(Component.translatable("message.better_witch_huts.emeralds", cost).withStyle(ChatFormatting.RED));
            return false;
        }
        Inventory inventory = player.getInventory();
        int left = cost;
        for (int i = 0; i < inventory.getContainerSize() && left > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(Items.EMERALD)) {
                int used = Math.min(left, stack.getCount());
                stack.shrink(used);
                left -= used;
            }
        }
        inventory.setChanged();
        return true;
    }

    public static boolean give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        return true;
    }

    /** Grants every criterion of {@code better_witch_huts:<path>} (all of them are "impossible" and only granted here). */
    public static void award(ServerPlayer player, String path) {
        AdvancementHolder holder = player.level().getServer().getAdvancements().get(Witchbound.id(path));
        if (holder == null) {
            return;
        }
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(holder);
        if (progress.isDone()) {
            return;
        }
        List<String> remaining = new ArrayList<>();
        progress.getRemainingCriteria().forEach(remaining::add);
        for (String criterion : remaining) {
            player.getAdvancements().award(holder, criterion);
        }
    }

    static Holder<MobEffect> effect(Pact pact) {
        return ModContent.PACT_EFFECTS.get(pact);
    }

    static Holder<MobEffect> effect(Hex hex) {
        return ModContent.HEX_EFFECTS.get(hex);
    }

    static void debug(String message, Object... args) {
        Constants.LOG.debug(message, args);
    }
}
