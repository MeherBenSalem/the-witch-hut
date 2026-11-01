package tn.nightbeam.betterwitchhuts.game;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import tn.nightbeam.betterwitchhuts.core.Hex;
import tn.nightbeam.betterwitchhuts.core.Pact;
import tn.nightbeam.betterwitchhuts.core.PlayerRecord;

/** Keeps the pact and hex status effects in line with the player's record (the record is the truth). */
public final class Effects {
    private Effects() {
    }

    public static void sync(ServerPlayer player, PlayerRecord record) {
        if (!player.isAlive()) {
            return;
        }
        for (Pact pact : Pact.values()) {
            set(player, Coven.effect(pact), record.hasPact(pact));
        }
        for (Hex hex : Hex.values()) {
            set(player, Coven.effect(hex), record.hasHex(hex));
        }
    }

    private static void set(ServerPlayer player, Holder<MobEffect> effect, boolean wanted) {
        MobEffectInstance current = player.getEffect(effect);
        if (wanted && (current == null || !current.isInfiniteDuration())) {
            player.addEffect(new MobEffectInstance(effect, MobEffectInstance.INFINITE_DURATION, 0, true, false, true));
        } else if (!wanted && current != null) {
            player.removeEffect(effect);
        }
    }
}
