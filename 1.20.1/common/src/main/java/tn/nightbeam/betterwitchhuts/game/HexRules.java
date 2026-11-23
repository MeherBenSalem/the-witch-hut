package tn.nightbeam.betterwitchhuts.game;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import tn.nightbeam.betterwitchhuts.content.ModContent;
import tn.nightbeam.betterwitchhuts.core.Draft;
import tn.nightbeam.betterwitchhuts.core.Hex;
import tn.nightbeam.betterwitchhuts.core.Objective;
import tn.nightbeam.betterwitchhuts.core.Objectives;
import tn.nightbeam.betterwitchhuts.core.PlayerRecord;
import tn.nightbeam.betterwitchhuts.core.WitchConfig;

/** Laying, tracking and breaking hexes. */
public final class HexRules {
    private HexRules() {
    }

    /**
     * Lays a hex on the player: {@code hex}, or a random one they do not carry. A Poppet in the inventory takes it
     * instead. Returns whether a hex landed.
     */
    public static boolean lay(ServerPlayer player, Hex hex, Component caster, boolean wardable) {
        if (player.isCreative() || player.isSpectator()) {
            return false;
        }
        PlayerRecord record = Records.get(player);
        WitchConfig.Hexing rules = WitchConfig.get().hexing();
        if (record.hexes.size() >= rules.maxHexes()) {
            return false;
        }
        if (hex == null) {
            List<Hex> pool = new ArrayList<>();
            for (Hex h : Hex.values()) {
                if (!record.hasHex(h) && !rules.disabled().contains(h.id())) {
                    pool.add(h);
                }
            }
            if (pool.isEmpty()) {
                return false;
            }
            hex = pool.get(player.getRandom().nextInt(pool.size()));
        } else if (record.hasHex(hex)) {
            return false;
        }
        ServerLevel level = (ServerLevel) player.level();
        if (wardable && consumePoppet(player)) {
            level.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.3, 0.5, 0.3, 0.02);
            level.playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.8F, 1.4F);
            player.sendSystemMessage(Component.translatable("message.better_witch_huts.poppet", Coven.hexName(hex)).withStyle(ChatFormatting.GOLD));
            Coven.award(player, "poppet_ward");
            return false;
        }
        Objective task = Draft.task(hex, new java.util.Random(player.getRandom().nextLong()));
        record.hexes.add(new PlayerRecord.HexState(hex.id(), task.id(), Progress.read(player, task), caster.getString()));
        record.dirty = true;
        Effects.sync(player, record);
        level.sendParticles(ParticleTypes.WITCH, player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.4, 0.7, 0.4, 0.1);
        level.playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 0.6F);
        player.sendSystemMessage(Component.translatable("message.better_witch_huts.hexed", caster, Coven.hexName(hex).withStyle(ChatFormatting.RED))
                .withStyle(ChatFormatting.DARK_PURPLE));
        player.sendSystemMessage(Component.translatable("message.better_witch_huts.hexed.task", Coven.objectiveText(task)).withStyle(ChatFormatting.GRAY));
        Coven.award(player, "hexed");
        if (record.hexes.size() >= 3) {
            Coven.award(player, "three_hexes");
        }
        return true;
    }

    private static boolean consumePoppet(ServerPlayer player) {
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(ModContent.POPPET.get())) {
                stack.shrink(1);
                inventory.setChanged();
                return true;
            }
        }
        return false;
    }

    /** Checks every hex's task; called once a second. */
    public static void tick(ServerPlayer player, PlayerRecord record) {
        for (int i = record.hexes.size() - 1; i >= 0; i--) {
            PlayerRecord.HexState state = record.hexes.get(i);
            Objective task = Objectives.byId(state.task);
            if (task != null && progress(player, state) >= task.target()) {
                remove(player, record, i, "message.better_witch_huts.hex_broken");
                record.hexesBroken++;
                Coven.award(player, "broke_hex");
                Coven.changeStanding(player, WitchConfig.get().standing().breakHexTask());
            }
        }
    }

    public static int progress(ServerPlayer player, PlayerRecord.HexState state) {
        Objective task = Objectives.byId(state.task);
        return task == null ? 0 : Math.max(0, Progress.read(player, task) - state.baseline);
    }

    /** Removes hex {@code index} with a message ({@code key} takes the hex's name). */
    public static void remove(ServerPlayer player, PlayerRecord record, int index, String key) {
        PlayerRecord.HexState state = record.hexes.remove(index);
        record.dirty = true;
        Effects.sync(player, record);
        Hex hex = Hex.byId(state.hex);
        ServerLevel level = (ServerLevel) player.level();
        level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.0, player.getZ(), 40, 0.4, 0.8, 0.4, 0.5);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.3F);
        if (hex != null) {
            player.sendSystemMessage(Component.translatable(key, Coven.hexName(hex)).withStyle(ChatFormatting.AQUA));
        }
    }
}
