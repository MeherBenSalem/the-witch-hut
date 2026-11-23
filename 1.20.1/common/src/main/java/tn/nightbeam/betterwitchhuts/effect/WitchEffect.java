package tn.nightbeam.betterwitchhuts.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * A pact or a hex shown as a status effect, so the player sees it in the inventory and the HUD. The effect only carries
 * the fixed attribute changes; the Coven's records are the truth, and the effect is put back within a second if milk,
 * death or a command removes it.
 */
public class WitchEffect extends MobEffect {
    public WitchEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
}
