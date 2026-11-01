package tn.nightbeam.betterwitchhuts.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import tn.nightbeam.betterwitchhuts.core.PlayerRecord;
import tn.nightbeam.betterwitchhuts.game.Coven;
import tn.nightbeam.betterwitchhuts.game.HexRules;
import tn.nightbeam.betterwitchhuts.game.Records;

/** Folk magic against witchcraft: smash it to break the oldest hex on you. */
public class WitchBottleItem extends Item {
    public WitchBottleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.success(stack);
        }
        PlayerRecord record = Records.get(serverPlayer);
        if (record.hexes.isEmpty()) {
            serverPlayer.displayClientMessage(Component.translatable("message.better_witch_huts.no_hex"), true);
            return InteractionResultHolder.fail(stack);
        }
        HexRules.remove(serverPlayer, record, 0, "message.better_witch_huts.hex_bottle");
        ServerLevel server = (ServerLevel) level;
        server.playSound(null, player.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
        server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, stack.copyWithCount(1)), player.getX(), player.getY() + 1.2, player.getZ(),
                16, 0.3, 0.3, 0.3, 0.08);
        Coven.award(serverPlayer, "witch_bottle");
        stack.consume(1, player);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.better_witch_huts.witch_bottle.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
