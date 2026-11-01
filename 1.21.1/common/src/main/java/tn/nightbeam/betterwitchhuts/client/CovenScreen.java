package tn.nightbeam.betterwitchhuts.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import tn.nightbeam.betterwitchhuts.Constants;
import tn.nightbeam.betterwitchhuts.core.Errand;
import tn.nightbeam.betterwitchhuts.core.Hex;
import tn.nightbeam.betterwitchhuts.core.Objective;
import tn.nightbeam.betterwitchhuts.core.Objectives;
import tn.nightbeam.betterwitchhuts.core.Pact;
import tn.nightbeam.betterwitchhuts.core.Standing;
import tn.nightbeam.betterwitchhuts.core.WitchConfig;
import tn.nightbeam.betterwitchhuts.menu.CovenMenu;

/** The Coven screen: a resident witch's services, or the player's own Grimoire. Drawn by hand, no textures needed (1.21.1: through {@link Canvas}). */
public class CovenScreen extends AbstractContainerScreen<CovenMenu> {
    private static final int W = 316;
    private static final int H = 222;
    private static final int BG = 0xF2120C18;
    private static final int PANEL = 0xFF1E1526;
    private static final int ROW = 0xFF271C31;
    private static final int GOLD = 0xFFC9A45C;
    private static final int GOLD_DIM = 0xFF6E5A36;
    private static final int TEXT = 0xFFEADFCB;
    private static final int DIM = 0xFF9A8CA6;
    private static final int GIFT = 0xFF86D67E;
    private static final int PRICE = 0xFFE5676B;
    private static final int BUTTON = 0xFF3B2A4A;
    private static final int BUTTON_HOVER = 0xFF5A4170;
    private static final int BUTTON_OFF = 0xFF241C2B;
    private static final int[] TIER_COLOURS = {0xFF7A1418, 0xFFB8483A, 0xFF7E7884, 0xFF5DAA4E, 0xFF4FB3B8, 0xFFC56BD9};

    private enum Tab { PACTS, HEXES, ERRAND, REMEDIES, RECORD }

    private Tab tab = Tab.PACTS;
    private final List<Button> buttons = new ArrayList<>();

    private record Button(int x, int y, int w, int h, int id, boolean enabled, Component label) {
        boolean over(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    public CovenScreen(CovenMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = W;
        this.imageHeight = H;
    }

    private boolean ledger() {
        return menu.get(CovenMenu.MODE) == CovenMenu.MODE_LEDGER;
    }

    private List<Tab> tabs() {
        return ledger() ? List.of(Tab.PACTS, Tab.HEXES, Tab.ERRAND, Tab.RECORD) : List.of(Tab.PACTS, Tab.HEXES, Tab.ERRAND, Tab.REMEDIES);
    }

    private Canvas canvas;

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        super.render(graphics, mouseX, mouseY, partial);
        if (canvas != null) {
            canvas.drawTooltip();
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partial, int mouseX, int mouseY) {
        canvas = new Canvas(graphics);
        background(canvas, mouseX, mouseY);
    }

    private void background(Canvas g, int mouseX, int mouseY) {
        if (!tabs().contains(tab)) {
            tab = Tab.PACTS;
        }
        buttons.clear();
        int x = leftPos;
        int y = topPos;
        g.fill(x - 2, y - 2, x + W + 2, y + H + 2, 0xFF07040A);
        g.fill(x, y, x + W, y + H, BG);
        g.outline(x, y, W, H, GOLD);
        g.outline(x + 2, y + 2, W - 4, H - 4, GOLD_DIM);
        header(g, x, y);
        tabsRow(g, x, y + 40, mouseX, mouseY);
        int cy = y + 60;
        g.fill(x + 6, cy - 2, x + W - 6, y + H - 6, PANEL);
        switch (tab) {
            case PACTS -> pacts(g, x + 10, cy + 2, mouseX, mouseY);
            case HEXES -> hexes(g, x + 10, cy + 2, mouseX, mouseY);
            case ERRAND -> errand(g, x + 10, cy + 2, mouseX, mouseY);
            case REMEDIES -> remedies(g, x + 10, cy + 2, mouseX, mouseY);
            case RECORD -> record(g, x + 10, cy + 2);
        }
        for (Button b : buttons) {
            boolean hover = b.enabled && b.over(mouseX, mouseY);
            g.fill(b.x, b.y, b.x + b.w, b.y + b.h, !b.enabled ? BUTTON_OFF : hover ? BUTTON_HOVER : BUTTON);
            g.outline(b.x, b.y, b.w, b.h, b.enabled ? GOLD : GOLD_DIM);
            g.centeredText(font, b.label, b.x + b.w / 2, b.y + (b.h - 8) / 2, b.enabled ? TEXT : DIM);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Everything is drawn in renderBg.
    }

    private void header(Canvas g, int x, int y) {
        g.text(font, title.copy().withStyle(ChatFormatting.BOLD), x + 10, y + 9, GOLD, false);
        int standing = menu.get(CovenMenu.STANDING);
        Standing.Tier tier = Standing.tier(standing);
        Component tierText = Component.translatable("gui.better_witch_huts.standing", tierName(tier), (standing > 0 ? "+" : "") + standing);
        g.text(font, tierText, x + W - 10 - font.width(tierText), y + 9, TIER_COLOURS[tier.ordinal()], false);
        // The standing bar: six tier bands from -100 to 100 and a marker.
        int bx = x + 10;
        int by = y + 23;
        int bw = W - 20;
        Standing.Tier[] tiers = Standing.Tier.values();
        for (int i = 0; i < tiers.length; i++) {
            int from = tiers[i].from();
            int to = i + 1 < tiers.length ? tiers[i + 1].from() : Standing.MAX + 1;
            int x1 = bx + (from - Standing.MIN) * bw / (Standing.MAX - Standing.MIN + 1);
            int x2 = bx + (to - Standing.MIN) * bw / (Standing.MAX - Standing.MIN + 1);
            int colour = TIER_COLOURS[i];
            g.fill(x1, by, x2, by + 6, tiers[i] == tier ? colour : (colour & 0x00FFFFFF) | 0x66000000);
        }
        g.outline(bx - 1, by - 1, bw + 2, 8, GOLD_DIM);
        int mx = bx + (standing - Standing.MIN) * bw / (Standing.MAX - Standing.MIN + 1);
        g.fill(mx - 1, by - 3, mx + 2, by + 9, 0xFFFFFFFF);
    }

    private void tabsRow(Canvas g, int x, int y, int mouseX, int mouseY) {
        List<Tab> tabs = tabs();
        int tw = (W - 12) / tabs.size();
        for (int i = 0; i < tabs.size(); i++) {
            Tab t = tabs.get(i);
            int tx = x + 6 + i * tw;
            boolean active = t == tab;
            boolean hover = mouseX >= tx && mouseX < tx + tw - 2 && mouseY >= y && mouseY < y + 16;
            g.fill(tx, y, tx + tw - 2, y + 16, active ? PANEL : hover ? 0xFF2C2036 : 0xFF181020);
            if (active) {
                g.fill(tx, y, tx + tw - 2, y + 1, GOLD);
            }
            Component label = Component.translatable("gui.better_witch_huts.tab." + t.name().toLowerCase(java.util.Locale.ROOT));
            g.centeredText(font, label, tx + (tw - 2) / 2, y + 4, active ? GOLD : DIM);
        }
    }

    // Pacts ---------------------------------------------------------------------------------------------------------------

    private void pacts(Canvas g, int x, int y, int mouseX, int mouseY) {
        int slots = menu.get(CovenMenu.SLOTS);
        int held = 0;
        for (int i = 0; i < 3; i++) {
            if (Pact.byIndex(menu.get(CovenMenu.PACT + i)) != null) {
                held++;
            }
        }
        int width = W - 20;
        if (!ledger()) {
            g.text(font, Component.translatable("gui.better_witch_huts.offers"), x, y, GOLD, false);
            y += 12;
            boolean canStrike = held < slots;
            for (int i = 0; i < 3; i++) {
                Pact pact = Pact.byIndex(menu.get(CovenMenu.OFFER + i));
                if (pact == null) {
                    continue;
                }
                pactRow(g, pact, x, y, width - 64, mouseX, mouseY);
                buttons.add(new Button(x + width - 60, y + 6, 56, 16, CovenMenu.BUTTON_STRIKE + i, canStrike,
                        Component.translatable("gui.better_witch_huts.strike")));
                y += 30;
            }
            if (!canStrike) {
                Component why = slots == 0 ? Component.translatable("gui.better_witch_huts.no_pacts_tier")
                        : Component.translatable("gui.better_witch_huts.slots_full");
                g.text(font, why, x, y, PRICE, false);
            }
            y += 12;
        }
        g.text(font, Component.translatable("gui.better_witch_huts.your_pacts", held, slots), x, y, GOLD, false);
        y += 12;
        if (held == 0) {
            g.text(font, Component.translatable("gui.better_witch_huts.no_pacts"), x, y, DIM, false);
        }
        int cost = menu.get(CovenMenu.RENOUNCE_COST);
        for (int i = 0; i < 3; i++) {
            Pact pact = Pact.byIndex(menu.get(CovenMenu.PACT + i));
            if (pact == null) {
                continue;
            }
            pactRow(g, pact, x, y, width - (ledger() ? 0 : 64), mouseX, mouseY);
            if (!ledger()) {
                buttons.add(new Button(x + width - 60, y + 6, 56, 16, CovenMenu.BUTTON_RENOUNCE + i, menu.get(CovenMenu.EMERALDS) >= cost,
                        Component.translatable("gui.better_witch_huts.renounce", cost)));
            }
            y += 30;
        }
    }

    private void pactRow(Canvas g, Pact pact, int x, int y, int width, int mouseX, int mouseY) {
        g.fill(x - 2, y, x + width, y + 28, ROW);
        g.blit(icon("pact_" + pact.id()), x + 2, y + 5, 0, 0, 18, 18, 18, 18);
        int tx = x + 24;
        int tw = width - 28;
        g.text(font, Component.translatable("effect.better_witch_huts.pact_" + pact.id()), tx, y + 2, TEXT, false);
        g.text(font, clip(Component.translatable("pact.better_witch_huts." + pact.id() + ".gift"), tw), tx, y + 11, GIFT, false);
        g.text(font, clip(Component.translatable("pact.better_witch_huts." + pact.id() + ".price"), tw), tx, y + 19, PRICE, false);
        if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + 28) {
            List<Component> lines = List.of(
                    Component.translatable("effect.better_witch_huts.pact_" + pact.id()).withStyle(ChatFormatting.LIGHT_PURPLE),
                    Component.translatable("gui.better_witch_huts.gift").withStyle(ChatFormatting.GREEN)
                            .append(Component.translatable("pact.better_witch_huts." + pact.id() + ".gift").withStyle(ChatFormatting.WHITE)),
                    Component.translatable("gui.better_witch_huts.price").withStyle(ChatFormatting.RED)
                            .append(Component.translatable("pact.better_witch_huts." + pact.id() + ".price").withStyle(ChatFormatting.WHITE)),
                    Component.translatable("pact.better_witch_huts." + pact.id() + ".lore").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            tooltip(g, lines, mouseX, mouseY);
        }
    }

    // Hexes ---------------------------------------------------------------------------------------------------------------

    private void hexes(Canvas g, int x, int y, int mouseX, int mouseY) {
        int max = menu.get(CovenMenu.MAX_HEXES);
        int count = 0;
        for (int i = 0; i < 3; i++) {
            if (Hex.byIndex(menu.get(CovenMenu.HEX + i)) != null) {
                count++;
            }
        }
        g.text(font, Component.translatable("gui.better_witch_huts.your_hexes", count, max), x, y, GOLD, false);
        y += 12;
        if (count == 0) {
            g.text(font, Component.translatable("gui.better_witch_huts.no_hexes"), x, y, DIM, false);
            y += 12;
        }
        int width = W - 20;
        int cost = menu.get(CovenMenu.LIFT_COST);
        for (int i = 0; i < 3; i++) {
            Hex hex = Hex.byIndex(menu.get(CovenMenu.HEX + i));
            Objective task = Objectives.byIndex(menu.get(CovenMenu.HEX_TASK + i));
            if (hex == null || task == null) {
                continue;
            }
            int rowW = width - (ledger() ? 0 : 64);
            g.fill(x - 2, y, x + rowW, y + 38, ROW);
            g.blit(icon("hex_" + hex.id()), x + 2, y + 4, 0, 0, 18, 18, 18, 18);
            int tx = x + 24;
            int tw = rowW - 28;
            g.text(font, Component.translatable("effect.better_witch_huts.hex_" + hex.id()), tx, y + 2, PRICE, false);
            g.text(font, clip(Component.translatable("hex.better_witch_huts." + hex.id() + ".effect"), tw), tx, y + 11, DIM, false);
            int progress = task.display(menu.get(CovenMenu.HEX_PROGRESS + i));
            Component taskText = Component.translatable("gui.better_witch_huts.break", Component.translatable("objective.better_witch_huts." + task.id(), task.displayTarget()));
            g.text(font, clip(taskText, tw - 40), tx, y + 20, TEXT, false);
            bar(g, tx, y + 30, tw - 4, progress, task.displayTarget(), 0xFF8E6BD0);
            if (!ledger()) {
                boolean refused = cost < 0;
                Component label = refused ? Component.translatable("gui.better_witch_huts.refused")
                        : cost == 0 ? Component.translatable("gui.better_witch_huts.lift_free") : Component.translatable("gui.better_witch_huts.lift", cost);
                buttons.add(new Button(x + width - 60, y + 11, 56, 16, CovenMenu.BUTTON_LIFT + i,
                        !refused && menu.get(CovenMenu.EMERALDS) >= cost, label));
            }
            if (mouseX >= x && mouseX < x + rowW && mouseY >= y && mouseY < y + 38) {
                tooltip(g, List.of(Component.translatable("effect.better_witch_huts.hex_" + hex.id()).withStyle(ChatFormatting.RED),
                        Component.translatable("hex.better_witch_huts." + hex.id() + ".effect").withStyle(ChatFormatting.GRAY),
                        Component.translatable("gui.better_witch_huts.break", Component.translatable("objective.better_witch_huts." + task.id(), task.displayTarget()))
                                .withStyle(ChatFormatting.WHITE),
                        Component.translatable("gui.better_witch_huts.progress", progress, task.displayTarget()).withStyle(ChatFormatting.LIGHT_PURPLE),
                        Component.translatable("gui.better_witch_huts.hex_cures").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)),
                        mouseX, mouseY);
            }
            y += 42;
        }
        if (count > 0) {
            g.textWithWordWrap(font, Component.translatable("gui.better_witch_huts.hex_cures"), x, y, width, DIM, false);
        }
    }

    // Errand --------------------------------------------------------------------------------------------------------------

    private void errand(Canvas g, int x, int y, int mouseX, int mouseY) {
        int width = W - 20;
        Errand active = Errand.byIndex(menu.get(CovenMenu.ERRAND));
        if (active != null) {
            g.text(font, Component.translatable("gui.better_witch_huts.errand_active"), x, y, GOLD, false);
            y += 14;
            errandCard(g, active, x, y, width);
            int progress = active.objective().display(menu.get(CovenMenu.ERRAND_PROGRESS));
            int target = active.objective().displayTarget();
            bar(g, x + 24, y + 34, width - 30, progress, target, 0xFF5DAA4E);
            g.text(font, Component.translatable("gui.better_witch_huts.progress", progress, target), x + 24, y + 40, DIM, false);
            y += 58;
            if (!ledger()) {
                buttons.add(new Button(x, y, 120, 18, CovenMenu.BUTTON_TURN_IN, progress >= target,
                        Component.translatable("gui.better_witch_huts.turn_in")));
                buttons.add(new Button(x + 126, y, 90, 18, CovenMenu.BUTTON_ABANDON, true,
                        Component.translatable("gui.better_witch_huts.abandon")));
            } else {
                g.textWithWordWrap(font, Component.translatable("gui.better_witch_huts.errand_return"), x, y, width, DIM, false);
            }
            return;
        }
        if (ledger()) {
            g.textWithWordWrap(font, Component.translatable("gui.better_witch_huts.no_errand_ledger"), x, y, width, DIM, false);
            return;
        }
        Errand offer = Errand.byIndex(menu.get(CovenMenu.ERRAND_OFFER));
        g.text(font, Component.translatable("gui.better_witch_huts.errand_offer"), x, y, GOLD, false);
        y += 14;
        if (offer != null) {
            errandCard(g, offer, x, y, width);
            y += 52;
            buttons.add(new Button(x, y, 120, 18, CovenMenu.BUTTON_ACCEPT, true, Component.translatable("gui.better_witch_huts.accept")));
            y += 26;
            g.textWithWordWrap(font, Component.translatable("gui.better_witch_huts.errand_hint"), x, y, width, DIM, false);
        }
    }

    private void errandCard(Canvas g, Errand errand, int x, int y, int width) {
        g.fill(x - 2, y, x + width, y + 48, ROW);
        g.item(icon(errand), x + 2, y + 4);
        g.text(font, Component.translatable("errand.better_witch_huts." + errand.id()), x + 24, y + 3, TEXT, false);
        g.text(font, clip(Component.translatable("objective.better_witch_huts." + errand.objective().id(), errand.objective().displayTarget()),
                width - 30), x + 24, y + 13, DIM, false);
        g.text(font, Component.translatable("gui.better_witch_huts.reward", errand.standing(), errand.emeralds()), x + 24, y + 23, GIFT, false);
    }

    private static ItemStack icon(Errand errand) {
        Objective objective = errand.objective();
        String key = objective.keys().get(0);
        ResourceLocation id = key.contains(":") ? ResourceLocation.parse(key) : ResourceLocation.withDefaultNamespace(key);
        Item item = switch (objective.kind()) {
            case DELIVER, USED -> BuiltInRegistries.ITEM.getOptional(id).orElse(Items.PAPER);
            case KILLED -> BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), id.getPath() + "_spawn_egg"))
                    .orElse(Items.IRON_SWORD);
            case CUSTOM -> objective.keys().contains("fish_caught") ? Items.FISHING_ROD : Items.PAPER;
            case MINED -> Items.IRON_PICKAXE;
        };
        return new ItemStack(item);
    }

    // Remedies ------------------------------------------------------------------------------------------------------------

    private void remedies(Canvas g, int x, int y, int mouseX, int mouseY) {
        int width = W - 20;
        int emeralds = menu.get(CovenMenu.EMERALDS);
        g.text(font, Component.translatable("gui.better_witch_huts.emeralds", emeralds >= 9999 ? "∞" : String.valueOf(emeralds)), x, y, GIFT, false);
        y += 14;
        int bottle = menu.get(CovenMenu.BOTTLE_COST);
        remedyRow(g, new ItemStack(Items.GLASS_BOTTLE), "witch_bottle", x, y, width, bottle, CovenMenu.BUTTON_BOTTLE, emeralds, mouseX, mouseY);
        y += 32;
        int poppet = menu.get(CovenMenu.POPPET_COST);
        remedyRow(g, new ItemStack(Items.WHEAT), "poppet", x, y, width, poppet, CovenMenu.BUTTON_POPPET, emeralds, mouseX, mouseY);
        y += 36;
        g.text(font, Component.translatable("gui.better_witch_huts.offering"), x, y, GOLD, false);
        y += 12;
        int items = menu.get(CovenMenu.OFFERING_ITEMS);
        int left = menu.get(CovenMenu.OFFERING_LEFT);
        g.textWithWordWrap(font, Component.translatable("gui.better_witch_huts.offering_desc"), x, y, width - 70, DIM, false);
        g.text(font, Component.translatable("gui.better_witch_huts.offering_have", items, left), x, y + 30, TEXT, false);
        buttons.add(new Button(x + width - 64, y + 4, 60, 18, CovenMenu.BUTTON_OFFER, items >= 2 && left > 0,
                Component.translatable("gui.better_witch_huts.offer")));
    }

    private void remedyRow(Canvas g, ItemStack fallback, String item, int x, int y, int width, int cost, int button, int emeralds,
                           int mouseX, int mouseY) {
        g.fill(x - 2, y, x + width, y + 28, ROW);
        Item registered = BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, item)).orElse(fallback.getItem());
        g.item(new ItemStack(registered), x + 2, y + 6);
        g.text(font, Component.translatable("item.better_witch_huts." + item), x + 24, y + 4, TEXT, false);
        g.text(font, clip(Component.translatable("item.better_witch_huts." + item + ".tooltip"), width - 100), x + 24, y + 15, DIM, false);
        Component label = cost < 0 ? Component.translatable(button == CovenMenu.BUTTON_POPPET ? "gui.better_witch_huts.needs_known" : "gui.better_witch_huts.refused")
                : Component.translatable("gui.better_witch_huts.buy", cost);
        buttons.add(new Button(x + width - 64, y + 6, 60, 16, button, cost >= 0 && emeralds >= cost, label));
    }

    // Record (Grimoire) ---------------------------------------------------------------------------------------------------

    private void record(Canvas g, int x, int y) {
        int width = W - 20;
        String[] keys = {"pacts_struck", "hexes_broken", "errands_done"};
        int[] values = {menu.get(CovenMenu.PACTS_STRUCK), menu.get(CovenMenu.HEXES_BROKEN), menu.get(CovenMenu.ERRANDS_DONE)};
        for (int i = 0; i < keys.length; i++) {
            g.text(font, Component.translatable("gui.better_witch_huts.record." + keys[i]), x, y, DIM, false);
            g.text(font, String.valueOf(values[i]), x + width - 30, y, TEXT, false);
            y += 13;
        }
        g.text(font, Component.translatable("gui.better_witch_huts.record.huts"), x, y, DIM, false);
        g.text(font, menu.get(CovenMenu.VISITED) + " / " + WitchConfig.STRUCTURES.size(), x + width - 30, y, TEXT, false);
        y += 20;
        Standing.Tier tier = Standing.tier(menu.get(CovenMenu.STANDING));
        g.textWithWordWrap(font, Component.translatable("gui.better_witch_huts.tier_desc." + tier.id()), x, y, width, TEXT, false);
        y += 34;
        if (tier != Standing.Tier.KIN) {
            g.textWithWordWrap(font, Component.translatable("gui.better_witch_huts.next_tier", tierName(tier.next()), tier.next().from()),
                    x, y, width, DIM, false);
        }
    }

    // Helpers -------------------------------------------------------------------------------------------------------------

    private static Component tierName(Standing.Tier tier) {
        return Component.translatable("tier.better_witch_huts." + tier.id());
    }

    private void bar(Canvas g, int x, int y, int width, int value, int max, int colour) {
        g.fill(x, y, x + width, y + 4, 0xFF120C18);
        int filled = max <= 0 ? 0 : Math.min(width, value * width / max);
        g.fill(x, y, x + filled, y + 4, colour);
        g.outline(x - 1, y - 1, width + 2, 6, GOLD_DIM);
    }

    private FormattedCharSequence clip(Component text, int width) {
        if (font.width(text) <= width) {
            return text.getVisualOrderText();
        }
        String plain = text.getString();
        while (!plain.isEmpty() && font.width(plain + "…") > width) {
            plain = plain.substring(0, plain.length() - 1);
        }
        return Component.literal(plain + "…").withStyle(text.getStyle()).getVisualOrderText();
    }


    private void tooltip(Canvas g, List<Component> lines, int mouseX, int mouseY) {
        g.setTooltipForNextFrame(font, lines, mouseX, mouseY);
    }

    private static ResourceLocation icon(String effect) {
        return ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "textures/mob_effect/" + effect + ".png");
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            List<Tab> tabs = tabs();
            int tw = (W - 12) / tabs.size();
            int ty = topPos + 40;
            if (mouseY >= ty && mouseY < ty + 16) {
                for (int i = 0; i < tabs.size(); i++) {
                    int tx = leftPos + 6 + i * tw;
                    if (mouseX >= tx && mouseX < tx + tw - 2) {
                        tab = tabs.get(i);
                        click();
                        return true;
                    }
                }
            }
            for (Button b : buttons) {
                if (b.enabled && b.over(mouseX, mouseY) && minecraft != null && minecraft.gameMode != null) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, b.id);
                    click();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void click() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }
}
