package tn.nightbeam.betterwitchhuts.game;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import tn.nightbeam.betterwitchhuts.content.ModContent;
import tn.nightbeam.betterwitchhuts.core.Hex;
import tn.nightbeam.betterwitchhuts.core.Pact;

/**
 * Minecraft 1.20.1 only. The 1.21 builds give pacts and hexes jump strength, safe fall distance, fall damage, burning
 * time, step height, gravity, reach and mining speed through attributes that 1.20.1 does not have. This class does the
 * same job by hand, reading which pact and hex effects the player carries (the client knows its own effects, so the
 * client-side parts agree with the server):
 * <ul>
 * <li>jump strength (Toad, Leaden Limbs): a hidden Jump Boost effect, one level up or down;</li>
 * <li>safe fall distance and fall damage (Toad, Hare, Raven, Brittle Bones): vanilla's fall damage is replaced;</li>
 * <li>burning time (Toad): new fire is doubled;</li>
 * <li>step height (Hare) and gravity (Raven): set on both sides every tick or second;</li>
 * <li>mining speed (Clumsy Fingers, Moth by day): {@link #miningFactor}, applied by each loader's break-speed hook;</li>
 * <li>reach (Raven, Clumsy Fingers): Forge's reach attributes ({@code ModContent.linkReach}), Fabric's client pick
 * range ({@link #blockReachBonus}).</li>
 * </ul>
 */
public final class LegacyAttributes {
    /** A player's own step height in 1.20.1. */
    public static final float BASE_STEP = 0.6F;
    public static final float HARE_STEP = 0.5F;
    /** Raven: 20% lower gravity (vanilla pulls 0.08 a tick, then keeps 98% of the speed). */
    public static final double RAVEN_GRAVITY = 0.20;
    private static final Map<UUID, Integer> FIRE = new ConcurrentHashMap<>();
    private static boolean refalling;

    private LegacyAttributes() {
    }

    public static void forget(UUID player) {
        FIRE.remove(player);
    }

    public static boolean has(Player player, Pact pact) {
        MobEffect effect = ModContent.PACT_EFFECTS.get(pact).get();
        return player.hasEffect(effect);
    }

    public static boolean has(Player player, Hex hex) {
        MobEffect effect = ModContent.HEX_EFFECTS.get(hex).get();
        return player.hasEffect(effect);
    }

    /** Under the open daytime sky, in a dimension with a sky. Night Hunter and Moth read this on the server. */
    public static boolean daylight(Player player) {
        Level level = player.level();
        return level.dimensionType().hasSkyLight() && level.isDay() && level.canSeeSky(BlockPos.containing(player.getEyePosition()));
    }

    /** Blocks added to the 3 a fall is free for: Toad +6, Raven +3, Brittle Bones -1. */
    public static double safeFallBonus(Player player) {
        return (has(player, Pact.TOAD) ? 6 : 0) + (has(player, Pact.RAVEN) ? 3 : 0) - (has(player, Hex.BRITTLE_BONES) ? 1 : 0);
    }

    /** Added to the fall damage multiplier of 1: Hare +0.5, Brittle Bones +1. */
    public static double fallDamageBonus(Player player) {
        return (has(player, Pact.HARE) ? 0.5 : 0) + (has(player, Hex.BRITTLE_BONES) ? 1.0 : 0);
    }

    /** Jump Boost levels: Toad +1 (about 40% higher), Leaden Limbs -1 (about a quarter lower). */
    public static int jumpLevels(Player player) {
        return (has(player, Pact.TOAD) ? 1 : 0) - (has(player, Hex.LEADEN_LIMBS) ? 1 : 0);
    }

    public static float stepBonus(Player player) {
        return has(player, Pact.HARE) ? HARE_STEP : 0;
    }

    /** Block reach added to the client's pick range (Fabric): Raven +1, Clumsy Fingers -1. */
    public static float blockReachBonus(Player player) {
        return (has(player, Pact.RAVEN) ? 1 : 0) - (has(player, Hex.CLUMSY_FINGERS) ? 1 : 0);
    }

    /** Mining speed factor: Clumsy Fingers x0.75, Moth under the daytime sky x0.8. */
    public static float miningFactor(Player player) {
        float factor = 1.0F;
        if (has(player, Hex.CLUMSY_FINGERS)) {
            factor *= 0.75F;
        }
        if (has(player, Pact.MOTH) && daylight(player)) {
            factor *= 0.8F;
        }
        return factor;
    }

    // Fall damage --------------------------------------------------------------------------------------------------------

    /**
     * The fall damage the player should take instead of {@code amount}, or null to leave vanilla's. Vanilla counts the
     * blocks fallen past 3 (and past any Jump Boost levels), times the landing block's multiplier; the pacts move the
     * free blocks and scale the result, and the hidden Jump Boost of {@link #serverTick} does not count.
     */
    public static Float fallDamage(LivingEntity victim, DamageSource source, float amount) {
        if (refalling || !(victim instanceof Player player) || !source.is(DamageTypeTags.IS_FALL)) {
            return null;
        }
        double safe = safeFallBonus(player);
        double extra = fallDamageBonus(player);
        MobEffectInstance jump = player.getEffect(MobEffects.JUMP);
        int levels = jump == null ? 0 : jump.getAmplifier() + 1;
        int ours = jump != null && isOurs(jump) ? levels : 0;
        if (safe == 0 && extra == 0 && ours == 0) {
            return null;
        }
        double counted = player.fallDistance - 3.0 - levels;
        double blockMultiplier = counted > 0 ? Math.min(4.0, amount / Math.max(1.0, Math.ceil(counted))) : 1.0;
        double wanted = player.fallDistance - 3.0 - (levels - ours) - safe;
        float damage = wanted <= 0 ? 0 : (float) Math.ceil(wanted * blockMultiplier * (1.0 + extra));
        return damage == amount ? null : damage;
    }

    /** Deals the replacement fall damage (the original hit has been cancelled). */
    public static void refall(LivingEntity victim, DamageSource source, float damage) {
        if (damage <= 0) {
            return;
        }
        refalling = true;
        try {
            victim.hurt(source, damage);
        } finally {
            refalling = false;
        }
    }

    // Ticks ---------------------------------------------------------------------------------------------------------------

    /** Once a second, after the pact and hex effects are synced. */
    public static void serverTick(ServerPlayer player) {
        jump(player);
        step(player);
        burning(player);
    }

    /** Every client tick, for the local player: movement is worked out on the client. */
    public static void clientTick(Player player) {
        step(player);
        if (has(player, Pact.RAVEN) && !player.onGround() && !player.isInWater() && !player.isInLava() && !player.isPassenger()
                && !player.getAbilities().flying && !player.isFallFlying() && !player.onClimbable()
                && !player.hasEffect(MobEffects.SLOW_FALLING) && !player.hasEffect(MobEffects.LEVITATION)) {
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(motion.x, motion.y + 0.08 * RAVEN_GRAVITY * 0.98, motion.z);
        }
    }

    private static boolean isOurs(MobEffectInstance effect) {
        return effect.isInfiniteDuration() && effect.isAmbient() && !effect.isVisible() && !effect.showIcon();
    }

    private static void jump(ServerPlayer player) {
        int levels = jumpLevels(player);
        MobEffectInstance current = player.getEffect(MobEffects.JUMP);
        boolean ours = current != null && isOurs(current);
        if (levels == 0) {
            if (ours) {
                player.removeEffect(MobEffects.JUMP);
            }
            return;
        }
        if (current != null && !ours) {
            return; // A Jump Boost potion wins while it lasts.
        }
        int amplifier = levels - 1;
        if (current == null || current.getAmplifier() != amplifier) {
            if (current != null) {
                player.removeEffect(MobEffects.JUMP);
            }
            player.addEffect(new MobEffectInstance(MobEffects.JUMP, MobEffectInstance.INFINITE_DURATION, amplifier, true, false, false));
        }
    }

    private static void step(Player player) {
        float bonus = stepBonus(player);
        float step = player.maxUpStep();
        if (bonus > 0) {
            if (step < BASE_STEP + bonus) {
                player.setMaxUpStep(BASE_STEP + bonus);
            }
        } else if (step == BASE_STEP + HARE_STEP) {
            player.setMaxUpStep(BASE_STEP);
        }
    }

    /** Toad: fire that caught since the last second burns twice as long. */
    private static void burning(ServerPlayer player) {
        int fire = Math.max(0, player.getRemainingFireTicks());
        Integer last = FIRE.get(player.getUUID());
        if (fire > 0 && !player.fireImmune() && has(player, Pact.TOAD)) {
            int expected = last == null ? 0 : Math.max(0, last - 20);
            if (fire > expected) {
                fire += fire - expected;
                player.setRemainingFireTicks(fire);
            }
        }
        FIRE.put(player.getUUID(), fire);
    }
}
