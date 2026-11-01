package tn.nightbeam.betterwitchhuts.game;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.level.levelgen.structure.Structure;
import tn.nightbeam.betterwitchhuts.Witchbound;
import tn.nightbeam.betterwitchhuts.core.Errand;
import tn.nightbeam.betterwitchhuts.core.Hex;
import tn.nightbeam.betterwitchhuts.core.PlayerRecord;
import tn.nightbeam.betterwitchhuts.core.Standing;
import tn.nightbeam.betterwitchhuts.core.WitchConfig;
import tn.nightbeam.betterwitchhuts.entity.CovenWitch;

/** Every loader event lands here. */
public final class Game {
    private static long ticks;

    private Game() {
    }

    public static void serverStarted(MinecraftServer server) {
        Records.start(server);
        Progress.clearCache();
        HexCaster.clear();
    }

    public static void serverStopping(MinecraftServer server) {
        Records.stop();
        HexCaster.clear();
    }

    public static void join(ServerPlayer player) {
        PlayerRecord record = Records.get(player);
        Effects.sync(player, record);
        Coven.awardStanding(player, Standing.tier(record.standing));
    }

    public static void leave(ServerPlayer player) {
        Records.unload(player.getUUID());
        PactRules.forget(player.getUUID());
    }

    public static void tick(MinecraftServer server) {
        ticks++;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if ((ticks + player.getId()) % 20 == 0) {
                second(player);
            }
            if (ticks % 5 == 0 && player.isSleeping() && Records.get(player).hasHex(Hex.INSOMNIA)) {
                player.stopSleepInBed(true, true);
                player.displayClientMessage(Component.translatable("message.better_witch_huts.insomnia").withStyle(ChatFormatting.DARK_PURPLE), true);
            }
        }
        if (ticks % 5 == 0) {
            for (ServerLevel level : server.getAllLevels()) {
                HexCaster.tick(level, ticks);
            }
        }
        if (ticks % 600 == 0) {
            Records.saveAll(true);
        }
    }

    private static void second(ServerPlayer player) {
        if (!player.isAlive()) {
            return;
        }
        PlayerRecord record = Records.get(player);
        Effects.sync(player, record);
        PactRules.tick(player, record);
        HexRules.tick(player, record);
        errand(player, record);
        if ((ticks / 20) % 5 == 0) {
            visits(player, record);
        }
    }

    /** Tells the player once when a kill or fishing errand is done. */
    private static void errand(ServerPlayer player, PlayerRecord record) {
        if (record.errand == null) {
            return;
        }
        Errand errand = Errand.byId(record.errand.errand);
        if (errand == null) {
            return;
        }
        boolean ready = Coven.errandProgress(player, record) >= errand.objective().target();
        if (ready && !record.errand.giver.startsWith("!")) {
            record.errand.giver = "!" + record.errand.giver;
            record.dirty = true;
            player.sendSystemMessage(Component.translatable("message.better_witch_huts.errand_ready").withStyle(ChatFormatting.GREEN));
        } else if (!ready && record.errand.giver.startsWith("!")) {
            record.errand.giver = record.errand.giver.substring(1);
        }
    }

    /** Records which huts the player has stood in, for the Grimoire and the advancements. */
    private static void visits(ServerPlayer player, PlayerRecord record) {
        ServerLevel level = (ServerLevel) player.level();
        var structures = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        BlockPos pos = player.blockPosition();
        for (String id : WitchConfig.STRUCTURES) {
            if (record.visited.contains(id)) {
                continue;
            }
            Structure structure = structures.get(ResourceKey.create(Registries.STRUCTURE, Witchbound.id(id)));
            if (structure != null && level.structureManager().getStructureWithPieceAt(pos, structure).isValid()) {
                record.visited.add(id);
                record.dirty = true;
                Coven.award(player, "visit_" + id);
                Coven.award(player, "found_hut");
                if (record.visited.size() >= WitchConfig.STRUCTURES.size()) {
                    Coven.award(player, "all_huts");
                }
            }
        }
    }

    // Damage and death --------------------------------------------------------------------------------------------------

    /** False cancels the damage. */
    public static boolean allowDamage(LivingEntity victim, DamageSource source, float amount) {
        if (victim instanceof ServerPlayer player && source.getEntity() instanceof Witch witch && !(witch instanceof CovenWitch)
                && witch.getCurrentRaid() == null && WitchConfig.get().kinPeace() && Coven.tier(player) == Standing.Tier.KIN) {
            return false;
        }
        return true;
    }

    public static void afterDamage(LivingEntity victim, DamageSource source, float taken) {
        PactRules.afterDamage(victim, source, taken);
        if (victim instanceof CovenWitch witch && source.getEntity() instanceof ServerPlayer player && witch.isAlive()) {
            if (witch.noteHurtBy(player, ticks)) {
                Coven.changeStanding(player, WitchConfig.get().standing().hurtResident());
            }
        }
    }

    /** False cancels the death. */
    public static boolean allowDeath(LivingEntity victim, DamageSource source) {
        return !(victim instanceof ServerPlayer player) || PactRules.allowDeath(player, source);
    }

    public static void afterDeath(LivingEntity victim, DamageSource source) {
        if (!(source.getEntity() instanceof ServerPlayer killer)) {
            return;
        }
        PactRules.onKill(killer, victim);
        if (victim instanceof CovenWitch witch) {
            Coven.changeStanding(killer, WitchConfig.get().standing().killResident());
            killer.sendSystemMessage(Component.translatable("message.better_witch_huts.grudge", witch.getDisplayName()).withStyle(ChatFormatting.DARK_RED));
            if (WitchConfig.get().hexing().residentGrudge()) {
                HexRules.lay(killer, null, witch.getDisplayName(), false);
            }
            Coven.award(killer, "killed_resident");
        } else if (victim instanceof Witch witch && witch.getCurrentRaid() == null) {
            Coven.changeStanding(killer, WitchConfig.get().standing().killWitch());
        }
    }

    public static long ticks() {
        return ticks;
    }

    public static List<String> structures() {
        return WitchConfig.STRUCTURES;
    }
}
