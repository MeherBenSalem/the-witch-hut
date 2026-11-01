package tn.nightbeam.betterwitchhuts.game;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.monster.Witch;
import tn.nightbeam.betterwitchhuts.core.Standing;
import tn.nightbeam.betterwitchhuts.core.WitchConfig;
import tn.nightbeam.betterwitchhuts.entity.CovenWitch;

/**
 * Witches that fight a player channel a hex at them: purple motes swirl around the witch for a second and a half, then
 * the hex flies if she can still see the player. Hitting her while she channels breaks the spell. Also keeps wild
 * witches off players of Kin standing.
 */
public final class HexCaster {
    /** Ticks a witch channels before the hex lands. */
    public static final int CHANNEL = 30;
    private static final Map<UUID, Cast> CASTS = new ConcurrentHashMap<>();

    private HexCaster() {
    }

    private static final class Cast {
        long next;
        long channelEnd;
        UUID target;
        int hurtAtStart;
    }

    public static void clear() {
        CASTS.clear();
    }

    /** Every 5 ticks for each level. */
    public static void tick(ServerLevel level, long now) {
        WitchConfig.Values config = WitchConfig.get();
        WitchConfig.Hexing rules = config.hexing();
        for (ServerPlayer player : level.players()) {
            if (!player.isAlive() || player.isSpectator() || player.isCreative()) {
                continue;
            }
            Standing.Tier tier = Coven.tier(player);
            for (Witch witch : level.getEntitiesOfClass(Witch.class, player.getBoundingBox().inflate(rules.range()),
                    w -> w.isAlive() && w.getTarget() == player)) {
                boolean resident = witch instanceof CovenWitch;
                if (!resident && tier == Standing.Tier.KIN && config.kinPeace() && witch.getCurrentRaid() == null) {
                    witch.setTarget(null);
                    continue;
                }
                if (!resident && !rules.wildWitches()) {
                    continue;
                }
                channel(level, witch, player, now, rules, tier);
            }
        }
        if (now % 200 == 0) {
            Iterator<Map.Entry<UUID, Cast>> it = CASTS.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, Cast> entry = it.next();
                if (now - entry.getValue().next > 2400 && entry.getValue().channelEnd == 0) {
                    it.remove();
                }
            }
        }
    }

    private static void channel(ServerLevel level, Witch witch, ServerPlayer player, long now, WitchConfig.Hexing rules, Standing.Tier tier) {
        Cast cast = CASTS.computeIfAbsent(witch.getUUID(), id -> {
            Cast c = new Cast();
            c.next = now + cooldown(level, rules) / 2;
            return c;
        });
        if (cast.channelEnd == 0) {
            if (now >= cast.next && witch.hasLineOfSight(player) && !witch.isDrinkingPotion()) {
                cast.channelEnd = now + CHANNEL;
                cast.target = player.getUUID();
                cast.hurtAtStart = witch.hurtTime;
                level.playSound(null, witch.blockPosition(), SoundEvents.EVOKER_PREPARE_WOLOLO, SoundSource.HOSTILE, 1.0F, 1.4F);
            }
            return;
        }
        if (!player.getUUID().equals(cast.target)) {
            return;
        }
        double angle = (now % 40) / 40.0 * Math.PI * 2;
        for (int i = 0; i < 3; i++) {
            double a = angle + i * Math.PI * 2 / 3;
            level.sendParticles(ParticleTypes.WITCH, witch.getX() + Math.cos(a) * 0.8, witch.getY() + 2.1, witch.getZ() + Math.sin(a) * 0.8,
                    1, 0, 0, 0, 0);
        }
        if (witch.hurtTime > 0 && witch.hurtTime != cast.hurtAtStart) {
            fizzle(level, witch, cast, now, rules);
            player.sendOverlayMessage(Component.translatable("message.better_witch_huts.interrupted"));
            return;
        }
        if (now < cast.channelEnd) {
            return;
        }
        cast.channelEnd = 0;
        cast.next = now + cooldown(level, rules);
        if (!witch.hasLineOfSight(player) || witch.distanceToSqr(player) > (double) rules.range() * rules.range()) {
            level.sendParticles(ParticleTypes.SMOKE, witch.getX(), witch.getY() + 2.0, witch.getZ(), 6, 0.2, 0.2, 0.2, 0.01);
            return;
        }
        double chance = rules.chance();
        if (level.isDarkOutside()) {
            chance += rules.nightBonus();
            if (level.getMoonBrightness(player.blockPosition()) >= 1.0F) {
                chance += rules.fullMoonBonus();
            }
        }
        if (tier == Standing.Tier.HUNTED || witch instanceof CovenWitch) {
            chance += 0.25;
        }
        level.playSound(null, witch.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 1.0F, 1.2F);
        if (level.getRandom().nextDouble() < chance) {
            HexRules.lay(player, null, witch.getDisplayName(), true);
        } else {
            level.sendParticles(ParticleTypes.WITCH, player.getX(), player.getY() + 1.0, player.getZ(), 8, 0.4, 0.6, 0.4, 0.02);
        }
    }

    private static void fizzle(ServerLevel level, Witch witch, Cast cast, long now, WitchConfig.Hexing rules) {
        cast.channelEnd = 0;
        cast.next = now + cooldown(level, rules);
        level.sendParticles(ParticleTypes.SMOKE, witch.getX(), witch.getY() + 2.0, witch.getZ(), 10, 0.3, 0.2, 0.3, 0.02);
        level.playSound(null, witch.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 0.6F, 1.6F);
    }

    private static long cooldown(ServerLevel level, WitchConfig.Hexing rules) {
        int min = rules.cooldownMin() * 20;
        int max = rules.cooldownMax() * 20;
        return min + (max > min ? level.getRandom().nextInt(max - min + 1) : 0);
    }
}
