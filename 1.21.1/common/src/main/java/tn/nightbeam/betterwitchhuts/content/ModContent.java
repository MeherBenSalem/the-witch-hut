package tn.nightbeam.betterwitchhuts.content;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
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
import tn.nightbeam.betterwitchhuts.Witchbound;
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
    public static final Map<Pact, Holder<MobEffect>> PACT_EFFECTS = new EnumMap<>(Pact.class);
    public static final Map<Hex, Holder<MobEffect>> HEX_EFFECTS = new EnumMap<>(Hex.class);

    private ModContent() {
    }

    public static void register(Registrar r, Platform platform) {
        COVEN_WITCH = r.register(Registries.ENTITY_TYPE, "coven_witch", id -> EntityType.Builder.of(CovenWitch::new, MobCategory.MONSTER)
                .sized(0.6F, 1.95F).eyeHeight(1.62F).clientTrackingRange(8)
                .build(id.toString()));
        GRIMOIRE = r.register(Registries.ITEM, "grimoire", id -> new GrimoireItem(item(id).stacksTo(1).rarity(Rarity.UNCOMMON)));
        WITCH_BOTTLE = r.register(Registries.ITEM, "witch_bottle", id -> new WitchBottleItem(item(id).stacksTo(16).rarity(Rarity.UNCOMMON)));
        POPPET = r.register(Registries.ITEM, "poppet", id -> new PoppetItem(item(id).stacksTo(1).rarity(Rarity.RARE)));
        COVEN_MENU = r.register(Registries.MENU, "coven", id -> platform.menuType(CovenMenu::new));
        HUT = r.register(Registries.STRUCTURE_TYPE, "hut", id -> () -> HutStructure.CODEC);
        HUT_PIECE = r.register(Registries.STRUCTURE_PIECE, "hut_piece", id -> HutPiece::new);
        for (Pact pact : Pact.values()) {
            PACT_EFFECTS.put(pact, r.holder(Registries.MOB_EFFECT, "pact_" + pact.id(), id -> pactEffect(pact)));
        }
        for (Hex hex : Hex.values()) {
            HEX_EFFECTS.put(hex, r.holder(Registries.MOB_EFFECT, "hex_" + hex.id(), id -> hexEffect(hex)));
        }
    }

    /** The fixed half of each pact; the rest (night, rain, combat) is in {@code PactRules}. */
    private static MobEffect pactEffect(Pact pact) {
        WitchEffect e = new WitchEffect(MobEffectCategory.NEUTRAL, pact.color());
        String key = "pact." + pact.id();
        switch (pact) {
            case LEECH -> mod(e, Attributes.MAX_HEALTH, key, -4, Operation.ADD_VALUE);
            case TOAD -> {
                mod(e, Attributes.JUMP_STRENGTH, key, 0.40, Operation.ADD_MULTIPLIED_BASE);
                mod(e, Attributes.SAFE_FALL_DISTANCE, key, 6, Operation.ADD_VALUE);
                mod(e, Attributes.BURNING_TIME, key, 1.0, Operation.ADD_MULTIPLIED_BASE);
            }
            case STONESKIN -> {
                mod(e, Attributes.ARMOR, key, 4, Operation.ADD_VALUE);
                mod(e, Attributes.ARMOR_TOUGHNESS, key, 2, Operation.ADD_VALUE);
                mod(e, Attributes.MOVEMENT_SPEED, key, -0.12, Operation.ADD_MULTIPLIED_TOTAL);
            }
            case HARE -> {
                mod(e, Attributes.MOVEMENT_SPEED, key, 0.18, Operation.ADD_MULTIPLIED_TOTAL);
                mod(e, Attributes.STEP_HEIGHT, key, 0.5, Operation.ADD_VALUE);
                mod(e, Attributes.FALL_DAMAGE_MULTIPLIER, key, 0.5, Operation.ADD_VALUE);
            }
            case MAGPIE -> {
                mod(e, Attributes.LUCK, key, 3, Operation.ADD_VALUE);
                mod(e, Attributes.MAX_HEALTH, key, -2, Operation.ADD_VALUE);
            }
            case BRIAR -> mod(e, Attributes.ATTACK_SPEED, key, -0.10, Operation.ADD_MULTIPLIED_TOTAL);
            case RAVEN -> {
                mod(e, Attributes.GRAVITY, key, -0.20, Operation.ADD_MULTIPLIED_BASE);
                mod(e, Attributes.SAFE_FALL_DISTANCE, key, 3, Operation.ADD_VALUE);
                mod(e, Attributes.BLOCK_INTERACTION_RANGE, key, 1, Operation.ADD_VALUE);
                mod(e, Attributes.ENTITY_INTERACTION_RANGE, key, 1, Operation.ADD_VALUE);
                mod(e, Attributes.ARMOR, key, -0.25, Operation.ADD_MULTIPLIED_TOTAL);
            }
            case UNDYING -> mod(e, Attributes.MAX_HEALTH, key, -6, Operation.ADD_VALUE);
            default -> {
                // NIGHT_HUNTER, WITCHFIRE, MOTH, WOLF: everything depends on the moment (PactRules).
            }
        }
        return e;
    }

    private static MobEffect hexEffect(Hex hex) {
        WitchEffect e = new WitchEffect(MobEffectCategory.HARMFUL, hex.color());
        String key = "hex." + hex.id();
        switch (hex) {
            case LEADEN_LIMBS -> {
                mod(e, Attributes.MOVEMENT_SPEED, key, -0.15, Operation.ADD_MULTIPLIED_TOTAL);
                mod(e, Attributes.JUMP_STRENGTH, key, -0.25, Operation.ADD_MULTIPLIED_BASE);
            }
            case BLACK_CAT -> mod(e, Attributes.LUCK, key, -4, Operation.ADD_VALUE);
            case BRITTLE_BONES -> {
                mod(e, Attributes.FALL_DAMAGE_MULTIPLIER, key, 1.0, Operation.ADD_VALUE);
                mod(e, Attributes.SAFE_FALL_DISTANCE, key, -1, Operation.ADD_VALUE);
            }
            case WITHERING_GRIP -> {
                mod(e, Attributes.ATTACK_DAMAGE, key, -0.20, Operation.ADD_MULTIPLIED_TOTAL);
                mod(e, Attributes.ATTACK_SPEED, key, -0.10, Operation.ADD_MULTIPLIED_TOTAL);
            }
            case FRAIL_HEART -> mod(e, Attributes.MAX_HEALTH, key, -6, Operation.ADD_VALUE);
            case CLUMSY_FINGERS -> {
                mod(e, Attributes.BLOCK_BREAK_SPEED, key, -0.25, Operation.ADD_MULTIPLIED_TOTAL);
                mod(e, Attributes.BLOCK_INTERACTION_RANGE, key, -1, Operation.ADD_VALUE);
            }
            default -> {
                // GNAWING_HUNGER, INSOMNIA: handled every tick (HexRules).
            }
        }
        return e;
    }

    private static void mod(MobEffect effect, Holder<Attribute> attribute, String key, double amount, Operation operation) {
        effect.addAttributeModifier(attribute, Witchbound.id(key), amount, operation);
    }

    /** Items for the creative tab (Tools and Utilities on the loaders). */
    public static List<Item> tabItems() {
        return List.of(GRIMOIRE.get(), WITCH_BOTTLE.get(), POPPET.get());
    }

    private static Item.Properties item(ResourceLocation id) {
        return new Item.Properties();
    }
}
