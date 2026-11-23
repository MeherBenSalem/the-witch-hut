package tn.nightbeam.betterwitchhuts.content;

import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import tn.nightbeam.betterwitchhuts.Constants;
import tn.nightbeam.betterwitchhuts.core.Hex;
import tn.nightbeam.betterwitchhuts.core.Pact;
import tn.nightbeam.betterwitchhuts.effect.WitchEffect;
import tn.nightbeam.betterwitchhuts.entity.CovenWitch;
import tn.nightbeam.betterwitchhuts.item.GrimoireItem;
import tn.nightbeam.betterwitchhuts.item.PoppetItem;
import tn.nightbeam.betterwitchhuts.item.WitchBottleItem;
import tn.nightbeam.betterwitchhuts.menu.CovenMenu;
import tn.nightbeam.betterwitchhuts.platform.Platform;
import tn.nightbeam.betterwitchhuts.platform.Registrar;
import tn.nightbeam.betterwitchhuts.world.HutPiece;
import tn.nightbeam.betterwitchhuts.world.HutStructure;

/** Everything the mod registers. */
public final class ModContent {
    public static Supplier<EntityType<CovenWitch>> COVEN_WITCH;
    public static Supplier<Item> GRIMOIRE;
    public static Supplier<Item> WITCH_BOTTLE;
    public static Supplier<Item> POPPET;
    public static Supplier<MenuType<CovenMenu>> COVEN_MENU;
    public static Supplier<StructureType<HutStructure>> HUT;
    public static Supplier<StructurePieceType> HUT_PIECE;
    public static final Map<Pact, Supplier<MobEffect>> PACT_EFFECTS = new EnumMap<>(Pact.class);
    public static final Map<Hex, Supplier<MobEffect>> HEX_EFFECTS = new EnumMap<>(Hex.class);

    private ModContent() {
    }

    public static void register(Registrar r, Platform platform) {
        COVEN_WITCH = r.register(Registries.ENTITY_TYPE, "coven_witch", id -> EntityType.Builder.of(CovenWitch::new, MobCategory.MONSTER)
                .sized(0.6F, 1.95F).clientTrackingRange(8)
                .build(id.toString()));
        GRIMOIRE = r.register(Registries.ITEM, "grimoire", id -> new GrimoireItem(item(id).stacksTo(1).rarity(Rarity.UNCOMMON)));
        WITCH_BOTTLE = r.register(Registries.ITEM, "witch_bottle", id -> new WitchBottleItem(item(id).stacksTo(16).rarity(Rarity.UNCOMMON)));
        POPPET = r.register(Registries.ITEM, "poppet", id -> new PoppetItem(item(id).stacksTo(1).rarity(Rarity.RARE)));
        COVEN_MENU = r.register(Registries.MENU, "coven", id -> platform.menuType(CovenMenu::new));
        HUT = r.register(Registries.STRUCTURE_TYPE, "hut", id -> () -> HutStructure.CODEC);
        HUT_PIECE = r.register(Registries.STRUCTURE_PIECE, "hut_piece", id -> HutPiece::new);
        for (Pact pact : Pact.values()) {
            PACT_EFFECTS.put(pact, r.register(Registries.MOB_EFFECT, "pact_" + pact.id(), id -> pactEffect(pact)));
        }
        for (Hex hex : Hex.values()) {
            HEX_EFFECTS.put(hex, r.register(Registries.MOB_EFFECT, "hex_" + hex.id(), id -> hexEffect(hex)));
        }
    }

    /**
     * The fixed half of each pact; the rest (night, rain, combat) is in {@code PactRules}. Minecraft 1.20.1 has no jump,
     * safe-fall, fall-damage, burning-time, step-height, gravity, reach or mining-speed attributes: those parts of a pact
     * are carried out by {@link tn.nightbeam.betterwitchhuts.game.LegacyAttributes} (reach on Forge through
     * {@link #linkReach}).
     */
    private static MobEffect pactEffect(Pact pact) {
        WitchEffect e = new WitchEffect(MobEffectCategory.NEUTRAL, pact.color());
        String key = "pact." + pact.id();
        switch (pact) {
            case LEECH -> mod(e, Attributes.MAX_HEALTH, key, -4, Operation.ADDITION);
            case STONESKIN -> {
                mod(e, Attributes.ARMOR, key, 4, Operation.ADDITION);
                mod(e, Attributes.ARMOR_TOUGHNESS, key, 2, Operation.ADDITION);
                mod(e, Attributes.MOVEMENT_SPEED, key, -0.12, Operation.MULTIPLY_TOTAL);
            }
            case HARE -> mod(e, Attributes.MOVEMENT_SPEED, key, 0.18, Operation.MULTIPLY_TOTAL);
            case MAGPIE -> {
                mod(e, Attributes.LUCK, key, 3, Operation.ADDITION);
                mod(e, Attributes.MAX_HEALTH, key, -2, Operation.ADDITION);
            }
            case BRIAR -> mod(e, Attributes.ATTACK_SPEED, key, -0.10, Operation.MULTIPLY_TOTAL);
            case RAVEN -> mod(e, Attributes.ARMOR, key, -0.25, Operation.MULTIPLY_TOTAL);
            case UNDYING -> mod(e, Attributes.MAX_HEALTH, key, -6, Operation.ADDITION);
            default -> {
                // TOAD: LegacyAttributes. NIGHT_HUNTER, WITCHFIRE, MOTH, WOLF: everything depends on the moment (PactRules).
            }
        }
        return e;
    }

    private static MobEffect hexEffect(Hex hex) {
        WitchEffect e = new WitchEffect(MobEffectCategory.HARMFUL, hex.color());
        String key = "hex." + hex.id();
        switch (hex) {
            case LEADEN_LIMBS -> mod(e, Attributes.MOVEMENT_SPEED, key, -0.15, Operation.MULTIPLY_TOTAL);
            case BLACK_CAT -> mod(e, Attributes.LUCK, key, -4, Operation.ADDITION);
            case WITHERING_GRIP -> {
                mod(e, Attributes.ATTACK_DAMAGE, key, -0.20, Operation.MULTIPLY_TOTAL);
                mod(e, Attributes.ATTACK_SPEED, key, -0.10, Operation.MULTIPLY_TOTAL);
            }
            case FRAIL_HEART -> mod(e, Attributes.MAX_HEALTH, key, -6, Operation.ADDITION);
            default -> {
                // BRITTLE_BONES, CLUMSY_FINGERS: LegacyAttributes. GNAWING_HUNGER, INSOMNIA: handled every tick (HexRules).
            }
        }
        return e;
    }

    /**
     * Forge's reach attributes for Raven (+1 block and entity reach) and Clumsy Fingers (-1 block reach). Called by the
     * Forge loader once the attributes exist; Fabric changes block reach on the client instead.
     */
    public static void linkReach(Attribute blockReach, Attribute entityReach) {
        MobEffect raven = PACT_EFFECTS.get(Pact.RAVEN).get();
        mod(raven, blockReach, "pact.raven.block_reach", 1, Operation.ADDITION);
        mod(raven, entityReach, "pact.raven.entity_reach", 1, Operation.ADDITION);
        mod(HEX_EFFECTS.get(Hex.CLUMSY_FINGERS).get(), blockReach, "hex.clumsy_fingers.block_reach", -1, Operation.ADDITION);
    }

    /** 1.20.1 attribute modifiers are keyed by UUID: one derived from a fixed name, so it never changes between runs. */
    public static UUID modifierId(String key) {
        return UUID.nameUUIDFromBytes((Constants.MOD_ID + ":" + key).getBytes(StandardCharsets.UTF_8));
    }

    private static void mod(MobEffect effect, Attribute attribute, String key, double amount, Operation operation) {
        effect.addAttributeModifier(attribute, modifierId(key + "/" + attribute.getDescriptionId()).toString(), amount, operation);
    }

    /** Items for the creative tab (Tools and Utilities on the loaders). */
    public static List<Item> tabItems() {
        return List.of(GRIMOIRE.get(), WITCH_BOTTLE.get(), POPPET.get());
    }

    private static Item.Properties item(ResourceLocation id) {
        return new Item.Properties();
    }
}
