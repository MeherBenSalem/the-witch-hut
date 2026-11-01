package tn.nightbeam.betterwitchhuts.game;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import tn.nightbeam.betterwitchhuts.Witchbound;
import tn.nightbeam.betterwitchhuts.core.Hex;
import tn.nightbeam.betterwitchhuts.core.Pact;
import tn.nightbeam.betterwitchhuts.core.PlayerRecord;

/** The parts of pacts (and two hexes) that depend on the moment: night and day, rain, hunger, combat. */
public final class PactRules {
    public static final double LEECH_SHARE = 0.20;
    public static final double NIGHT_BONUS = 0.35;
    public static final double DAY_MALUS = -0.20;
    public static final double WET_MALUS = -0.25;
    public static final int WITCHFIRE_SECONDS = 4;
    public static final double BRIAR_SHARE = 0.30;
    public static final double WOLF_EXTRA_HUNGER = 0.5;
    public static final double GNAWING_EXTRA_HUNGER = 1.0;

    private static final Identifier NIGHT_HUNTER = Witchbound.id("pact.night_hunter.now");
    private static final Identifier WITCHFIRE_WET = Witchbound.id("pact.witchfire.wet");
    private static final Identifier MOTH_SPEED = Witchbound.id("pact.moth.daylight_speed");
    private static final Identifier MOTH_MINING = Witchbound.id("pact.moth.daylight_mining");
    /** Food and saturation seen a second ago, to scale hunger drain. */
    private static final Map<UUID, float[]> HUNGER = new ConcurrentHashMap<>();
    private static boolean reflecting;

    private PactRules() {
    }

    public static void forget(UUID player) {
        HUNGER.remove(player);
    }

    /** Once a second. */
    public static void tick(ServerPlayer player, PlayerRecord record) {
        ServerLevel level = (ServerLevel) player.level();
        BlockPos eyes = BlockPos.containing(player.getEyePosition());
        boolean underSky = level.canSeeSky(eyes);
        boolean daylight = underSky && level.isBrightOutside() && level.dimensionType().hasSkyLight();
        double night = 0;
        if (record.hasPact(Pact.NIGHT_HUNTER)) {
            night = daylight ? DAY_MALUS : (level.isDarkOutside() || !underSky ? NIGHT_BONUS : 0);
        }
        modifier(player, Attributes.ATTACK_DAMAGE, NIGHT_HUNTER, night);
        modifier(player, Attributes.ATTACK_DAMAGE, WITCHFIRE_WET,
                record.hasPact(Pact.WITCHFIRE) && player.isInWaterOrRain() ? WET_MALUS : 0);
        boolean moth = record.hasPact(Pact.MOTH);
        modifier(player, Attributes.MOVEMENT_SPEED, MOTH_SPEED, moth && daylight ? -0.10 : 0);
        modifier(player, Attributes.BLOCK_BREAK_SPEED, MOTH_MINING, moth && daylight ? -0.20 : 0);
        if (moth) {
            MobEffectInstance vision = player.getEffect(MobEffects.NIGHT_VISION);
            if (vision == null || (!vision.isInfiniteDuration() && vision.getDuration() < 300)) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, true, false, false));
            }
        }
        hunger(player, record);
    }

    /** Wolf (price) and Gnawing Hunger: food drains faster, by scaling whatever the last second drained. */
    private static void hunger(ServerPlayer player, PlayerRecord record) {
        double extra = (record.hasPact(Pact.WOLF) ? WOLF_EXTRA_HUNGER : 0) + (record.hasHex(Hex.GNAWING_HUNGER) ? GNAWING_EXTRA_HUNGER : 0);
        var food = player.getFoodData();
        float[] last = HUNGER.computeIfAbsent(player.getUUID(), k -> new float[]{food.getFoodLevel(), food.getSaturationLevel(), 0});
        float drained = (last[0] + last[1]) - (food.getFoodLevel() + food.getSaturationLevel());
        if (extra > 0 && drained > 0 && drained < 6 && !player.isCreative()) {
            float owed = last[2] + (float) (drained * extra);
            float saturation = food.getSaturationLevel();
            float fromSaturation = Math.min(saturation, owed);
            food.setSaturation(saturation - fromSaturation);
            owed -= fromSaturation;
            int whole = (int) owed;
            if (whole > 0) {
                food.setFoodLevel(Math.max(0, food.getFoodLevel() - whole));
                owed -= whole;
            }
            last[2] = owed;
        }
        last[0] = food.getFoodLevel();
        last[1] = food.getSaturationLevel();
    }

    private static void modifier(LivingEntity entity, Holder<Attribute> attribute, Identifier id, double amount) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        AttributeModifier current = instance.getModifier(id);
        if (amount == 0) {
            if (current != null) {
                instance.removeModifier(id);
            }
        } else if (current == null || current.amount() != amount) {
            instance.addOrUpdateTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    // Combat ------------------------------------------------------------------------------------------------------------

    /** A melee hit: the damage came straight from the attacker, not from an arrow, a potion or thorns. */
    private static boolean melee(DamageSource source, LivingEntity attacker) {
        return source.getDirectEntity() == attacker && !source.is(DamageTypeTags.IS_PROJECTILE) && !source.is(DamageTypeTags.IS_EXPLOSION)
                && !source.is(net.minecraft.world.damagesource.DamageTypes.THORNS);
    }

    public static void afterDamage(LivingEntity victim, DamageSource source, float taken) {
        if (taken <= 0 || reflecting) {
            return;
        }
        if (source.getEntity() instanceof ServerPlayer attacker && attacker != victim && melee(source, attacker)) {
            PlayerRecord record = Records.get(attacker);
            if (record.hasPact(Pact.LEECH)) {
                attacker.heal((float) (taken * LEECH_SHARE));
            }
            if (record.hasPact(Pact.WITCHFIRE) && !victim.fireImmune()) {
                victim.igniteForSeconds(WITCHFIRE_SECONDS);
            }
        }
        if (victim instanceof ServerPlayer player && source.getEntity() instanceof LivingEntity attacker && attacker != victim
                && melee(source, attacker) && Records.get(player).hasPact(Pact.BRIAR)) {
            reflecting = true;
            try {
                attacker.hurtServer((ServerLevel) player.level(), player.damageSources().thorns(player), (float) (taken * BRIAR_SHARE));
            } finally {
                reflecting = false;
            }
        }
    }

    /** Undying: once per in-game day a killing blow leaves the player standing. Returns false to cancel the death. */
    public static boolean allowDeath(ServerPlayer player, DamageSource source) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return true;
        }
        PlayerRecord record = Records.get(player);
        if (!record.hasPact(Pact.UNDYING)) {
            return true;
        }
        ServerLevel level = (ServerLevel) player.level();
        long today = Coven.day(level);
        if (record.undyingDay == today) {
            return true;
        }
        record.undyingDay = today;
        record.dirty = true;
        player.setHealth(4.0F);
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 60, 1));
        if (player.isOnFire()) {
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200, 0));
        }
        level.sendParticles(ParticleTypes.SOUL, player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.4, 0.8, 0.4, 0.05);
        level.playSound(null, player.blockPosition(), SoundEvents.WITCH_CELEBRATE, SoundSource.PLAYERS, 1.0F, 0.6F);
        player.sendSystemMessage(Component.translatable("message.better_witch_huts.undying").withStyle(ChatFormatting.LIGHT_PURPLE));
        Coven.award(player, "undying_saved");
        return false;
    }

    /** Wolf (gift): every kill feeds the player. */
    public static void onKill(ServerPlayer killer, LivingEntity victim) {
        if (Records.get(killer).hasPact(Pact.WOLF)) {
            killer.getFoodData().eat(2, 0.5F);
            float[] last = HUNGER.get(killer.getUUID());
            if (last != null) {
                last[0] = killer.getFoodData().getFoodLevel();
                last[1] = killer.getFoodData().getSaturationLevel();
            }
        }
    }
}
