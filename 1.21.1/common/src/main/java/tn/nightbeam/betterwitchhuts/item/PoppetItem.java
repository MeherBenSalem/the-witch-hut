package tn.nightbeam.betterwitchhuts.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Carried anywhere in the inventory, it takes the next hex a witch casts at you, and burns. */
public class PoppetItem extends Item {
    public PoppetItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.better_witch_huts.poppet.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
