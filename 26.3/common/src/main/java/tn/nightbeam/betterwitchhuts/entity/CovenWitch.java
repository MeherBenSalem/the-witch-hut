package tn.nightbeam.betterwitchhuts.entity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableWitchTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestHealableRaiderTargetGoal;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import tn.nightbeam.betterwitchhuts.game.Coven;
import tn.nightbeam.betterwitchhuts.menu.CovenMenu;

/**
 * A resident witch: she lives in her hut, never despawns, never joins raids, and only fights a player who strikes her
 * or whom the Coven hunts. Right-click her to open the Coven menu: pacts, remedies, errands and offerings.
 */
public class CovenWitch extends Witch {
    /** Names for residents that did not come with one (summoned or spawned from an egg). */
    public static final List<String> NAMES = List.of("Old Mother Mirewort", "Granny Hollowell", "Auntie Silt", "Hedgewife Tansy",
            "Mother Nightshade", "Frostwife Isolde", "Crone Ossia", "Widow Bramblewick", "Goody Thistle", "Nan Corvina",
            "Agatha Rook", "Mistress Belladonna");

    private String hut = "";
    private UUID customer;
    private final Map<UUID, Long> hurtBy = new HashMap<>();

    public CovenWitch(EntityType<? extends Witch> type, Level level) {
        super(type, level);
        setCanJoinRaid(false);
        setPersistenceRequired();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // Vanilla witches hunt every player and heal raiders; a resident only answers a blow (HurtByTargetGoal stays)
        // or a player the Coven hunts.
        targetSelector.removeAllGoals(goal -> goal instanceof NearestAttackableWitchTargetGoal || goal instanceof NearestHealableRaiderTargetGoal);
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                (target, level) -> target instanceof ServerPlayer player && Coven.isHunted(player)));
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!isAlive() || getTarget() == player) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            if (Coven.isHunted(serverPlayer)) {
                serverPlayer.sendOverlayMessage(Component.translatable("message.better_witch_huts.refuses", getDisplayName()));
                playSound(SoundEvents.WITCH_AMBIENT, 1.0F, 0.7F);
                return InteractionResult.CONSUME;
            }
            customer = player.getUUID();
            getNavigation().stop();
            CovenMenu.open(serverPlayer, this);
            playSound(SoundEvents.WITCH_AMBIENT, 1.0F, 1.1F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (customer != null && level() instanceof ServerLevel level) {
            Player player = level.getPlayerByUUID(customer);
            if (player == null || !(player.containerMenu instanceof CovenMenu menu) || menu.witch() != this) {
                customer = null;
            } else {
                getNavigation().stop();
                getLookControl().setLookAt(player, 30.0F, 30.0F);
            }
        }
    }

    /** Returns true at most once every 3 seconds per player, so a flurry of blows costs standing once. */
    public boolean noteHurtBy(ServerPlayer player, long now) {
        Long last = hurtBy.get(player.getUUID());
        if (last != null && now - last < 60) {
            return false;
        }
        hurtBy.put(player.getUUID(), now);
        return true;
    }

    public String hut() {
        return hut;
    }

    public void setHut(String hut) {
        this.hut = hut == null ? "" : hut;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data);
        if (!hasCustomName()) {
            setCustomName(Component.literal(NAMES.get(getRandom().nextInt(NAMES.size()))));
        }
        return result;
    }

    @Override
    public boolean canJoinRaid() {
        return false;
    }

    @Override
    public boolean canBeLeader() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("Hut", hut);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        hut = input.getStringOr("Hut", "");
        // A name saved as JSON text ({"text":"..."}) shows up literally on a modded entity: keep only the text.
        if (getCustomName() != null) {
            String name = getCustomName().getString();
            if (name.startsWith("{\"text\":\"") && name.endsWith("\"}")) {
                setCustomName(Component.literal(name.substring(9, name.length() - 2)));
            }
        }
    }
}
