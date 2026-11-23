package tn.nightbeam.betterwitchhuts.fabric.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tn.nightbeam.betterwitchhuts.game.LegacyAttributes;

/**
 * Block reach of the Raven pact (+1) and Clumsy Fingers (-1): 1.20.1 has no reach attribute, and Fabric picks blocks
 * within this range on the client (the server accepts up to 6 blocks).
 */
@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
    @Inject(method = "getPickRange", at = @At("RETURN"), cancellable = true)
    private void better_witch_huts$reach(CallbackInfoReturnable<Float> cir) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            float bonus = LegacyAttributes.blockReachBonus(player);
            if (bonus != 0) {
                cir.setReturnValue(cir.getReturnValueF() + bonus);
            }
        }
    }
}
