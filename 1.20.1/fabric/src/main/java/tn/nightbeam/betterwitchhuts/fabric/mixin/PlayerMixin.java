package tn.nightbeam.betterwitchhuts.fabric.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tn.nightbeam.betterwitchhuts.game.LegacyAttributes;

/** Mining speed of Clumsy Fingers and the Moth pact (1.20.1 has no mining speed attribute); runs on both sides. */
@Mixin(Player.class)
public abstract class PlayerMixin {
    @Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
    private void better_witch_huts$miningSpeed(BlockState state, CallbackInfoReturnable<Float> cir) {
        float factor = LegacyAttributes.miningFactor((Player) (Object) this);
        if (factor != 1.0F) {
            cir.setReturnValue(cir.getReturnValueF() * factor);
        }
    }
}
