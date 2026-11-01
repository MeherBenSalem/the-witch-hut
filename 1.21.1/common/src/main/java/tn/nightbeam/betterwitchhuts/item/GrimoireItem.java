package tn.nightbeam.betterwitchhuts.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import tn.nightbeam.betterwitchhuts.menu.CovenMenu;

/** The player's own ledger: Coven Standing, pacts, hexes and the errand, readable anywhere. */
public class GrimoireItem extends Item {
    public GrimoireItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) {
            CovenMenu.open(serverPlayer, null);
        }
        player.playSound(SoundEvents.BOOK_PAGE_TURN, 1.0F, 1.0F);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.better_witch_huts.grimoire.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
