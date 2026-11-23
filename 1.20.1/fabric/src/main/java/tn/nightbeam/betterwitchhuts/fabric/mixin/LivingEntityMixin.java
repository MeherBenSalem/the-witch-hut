package tn.nightbeam.betterwitchhuts.fabric.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tn.nightbeam.betterwitchhuts.game.Game;

/**
 * Fabric API 0.92 (Minecraft 1.20.1) has no {@code ServerLivingEntityEvents.AFTER_DAMAGE}; this stands in for it. After
 * an accepted hit it reports the health (and absorption) the entity lost, the damage the 1.21 builds receive from
 * AFTER_DAMAGE and NeoForge's {@code LivingDamageEvent.Post}.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Unique
    private float better_witch_huts$healthBefore;

    @Inject(method = "hurt", at = @At("HEAD"))
    private void better_witch_huts$beforeHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        better_witch_huts$healthBefore = self.getHealth() + self.getAbsorptionAmount();
    }

    @Inject(method = "hurt", at = @At("RETURN"))
    private void better_witch_huts$afterHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!cir.getReturnValueZ() || self.level().isClientSide()) {
            return;
        }
        float taken = better_witch_huts$healthBefore - (self.getHealth() + self.getAbsorptionAmount());
        if (taken > 0) {
            Game.afterDamage(self, source, taken);
        }
    }
}
