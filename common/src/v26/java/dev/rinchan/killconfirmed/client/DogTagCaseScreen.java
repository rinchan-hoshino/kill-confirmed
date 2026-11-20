package dev.rinchan.killconfirmed.client;

import dev.rinchan.killconfirmed.DogTagCaseMenu;
import dev.rinchan.killconfirmed.DogTagCaseStorage;
import dev.rinchan.killconfirmed.portable.DogTagCaseModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** 100% vanilla-style dual-section stone container screen for the 108-slot Dog Tag Case (26.x). */
public final class DogTagCaseScreen extends AbstractContainerScreen<DogTagCaseMenu> {
    private static final int PANEL_W = 194;
    private static final int PANEL_H = 222;

    private static final int SCROLL_X = 175;
    private static final int SCROLL_Y = 18;
    private static final int SCROLL_W = 12;
    private static final int SCROLL_H = 108;
    private static final int THUMB_H = 15;

    private static final int BTN_SIZE = 12;
    private static final int BTN_AUTO_X = 95;
    private static final int BTN_SORT_X = 109;
    private static final int BTN_Y = 4;

    private static final int SEARCH_X = 124;
    private static final int SEARCH_Y = 4;
    private static final int SEARCH_W = 63;
    private static final int SEARCH_H = 12;

    private static final int STATS_X = 175;
    private static final int STATS_Y = 139;
    private static final int STATS_SIZE = 12;

    private EditBox searchBox;
    private String searchQuery = "";
    private boolean draggingScroll = false;

    public DogTagCaseScreen(DogTagCaseMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, PANEL_W, PANEL_H);
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 129;
    }

    public static void open(net.minecraft.client.Minecraft minecraft, int containerId, Component title) {
        if (minecraft.player == null) return;
        DogTagCaseMenu menu = new DogTagCaseMenu(containerId, minecraft.player.getInventory());
        minecraft.player.containerMenu = menu;
        minecraft.setScreenAndShow(new DogTagCaseScreen(menu, minecraft.player.getInventory(), title));
    }

    @Override
    protected void init() {
        super.init();
        this.searchBox = new EditBox(
                this.font,
                this.leftPos + SEARCH_X,
                this.topPos + SEARCH_Y,
                SEARCH_W,
                SEARCH_H,
                Component.translatable("kill_confirmed.case.search.hint"));
        this.searchBox.setMaxLength(64);
        this.searchBox.setBordered(true);
        this.searchBox.setValue(this.searchQuery);
        this.searchBox.setHint(Component.translatable("kill_confirmed.case.search.hint"));
        this.searchBox.setResponder(text -> this.searchQuery = text);
        this.addRenderableWidget(this.searchBox);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        int x = this.leftPos;
        int y = this.topPos;

        drawVanillaPanel(g, x, y, PANEL_W, PANEL_H);

        for (int row = 0; row < DogTagCaseModel.VISIBLE_ROWS; row++) {
            for (int col = 0; col < DogTagCaseModel.COLUMNS; col++) {
                drawSlotWell(g, x + 7 + col * 18, y + 17 + row * 18);
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlotWell(g, x + 7 + col * 18, y + 139 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlotWell(g, x + 7 + col * 18, y + 197);
        }

        int sx = x + SCROLL_X;
        int sy = y + SCROLL_Y;
        drawRecessedBox(g, sx - 1, sy - 1, SCROLL_W + 2, SCROLL_H + 2, 0xFF8B8B8B);
        int maxOffset = SCROLL_H - THUMB_H;
        int thumbY = sy + (DogTagCaseModel.MAX_SCROLL_ROW > 0
                ? (this.menu.scrollRow() * maxOffset) / DogTagCaseModel.MAX_SCROLL_ROW
                : 0);
        boolean hoverThumb = isInRect(mouseX, mouseY, sx, thumbY, SCROLL_W, THUMB_H) || draggingScroll;
        drawBeveledButton(g, sx, thumbY, SCROLL_W, THUMB_H, hoverThumb);
        for (int gy = 4; gy <= 10; gy += 2) {
            g.fill(sx + 2, thumbY + gy, sx + SCROLL_W - 2, thumbY + gy + 1, 0xFF8B8B8B);
            g.fill(sx + 2, thumbY + gy + 1, sx + SCROLL_W - 2, thumbY + gy + 2, 0xFFE6E6E6);
        }

        int autoX = x + BTN_AUTO_X;
        int autoY = y + BTN_Y;
        boolean hoverAuto = isInRect(mouseX, mouseY, autoX, autoY, BTN_SIZE, BTN_SIZE);
        drawBeveledButton(g, autoX, autoY, BTN_SIZE, BTN_SIZE, hoverAuto);
        int dotColor = this.menu.isAutoPickup() ? 0xFF3CB350 : 0xFF6B6B6B;
        int dotHi = this.menu.isAutoPickup() ? 0xFF8AF09C : 0xFF9A9A9A;
        g.fill(autoX + 3, autoY + 3, autoX + 9, autoY + 9, 0xFF2B2B2B);
        g.fill(autoX + 4, autoY + 4, autoX + 8, autoY + 8, dotColor);
        g.fill(autoX + 4, autoY + 4, autoX + 6, autoY + 6, dotHi);

        int sortX = x + BTN_SORT_X;
        int sortY = y + BTN_Y;
        boolean hoverSort = isInRect(mouseX, mouseY, sortX, sortY, BTN_SIZE, BTN_SIZE);
        drawBeveledButton(g, sortX, sortY, BTN_SIZE, BTN_SIZE, hoverSort);
        g.fill(sortX + 3, sortY + 3, sortX + 9, sortY + 4, 0xFF373737);
        g.fill(sortX + 3, sortY + 5, sortX + 8, sortY + 6, 0xFF373737);
        g.fill(sortX + 3, sortY + 7, sortX + 6, sortY + 8, 0xFF373737);
        g.fill(sortX + 8, sortY + 6, sortX + 9, sortY + 9, 0xFF555555);

        int stX = x + STATS_X;
        int stY = y + STATS_Y;
        boolean hoverStats = isInRect(mouseX, mouseY, stX, stY, STATS_SIZE, STATS_SIZE);
        drawBeveledButton(g, stX, stY, STATS_SIZE, STATS_SIZE, hoverStats);
        g.fill(stX + 3, stY + 2, stX + 9, stY + 10, 0xFF555555);
        g.fill(stX + 4, stY + 3, stX + 8, stY + 9, 0xFFD8C088);
        g.fill(stX + 5, stY + 4, stX + 7, stY + 5, 0xFF555555);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);

        if (this.searchQuery != null && !this.searchQuery.isBlank()) {
            for (int i = 0; i < DogTagCaseModel.VISIBLE_SLOTS; i++) {
                Slot slot = this.menu.slots.get(i);
                ItemStack stack = slot.getItem();
                if (stack.isEmpty()) continue;
                int sx = this.leftPos + slot.x;
                int sy = this.topPos + slot.y;
                if (DogTagCaseModel.matchesSearch(DogTagCaseStorage.searchableText(stack), this.searchQuery)) {
                    g.fill(sx - 1, sy - 1, sx + 17, sy, 0xFFE5C878);
                    g.fill(sx - 1, sy + 16, sx + 17, sy + 17, 0xFFE5C878);
                    g.fill(sx - 1, sy, sx, sy + 16, 0xFFE5C878);
                    g.fill(sx + 16, sy, sx + 17, sy + 16, 0xFFE5C878);
                } else {
                    g.fill(sx, sy, sx + 16, sy + 16, 0xAA000000);
                }
            }
        }

        int x = this.leftPos;
        int y = this.topPos;
        if (isInRect(mouseX, mouseY, x + BTN_AUTO_X, y + BTN_Y, BTN_SIZE, BTN_SIZE)) {
            boolean on = this.menu.isAutoPickup();
            g.setTooltipForNextFrame(this.font, List.of(
                    Component.translatable(on ? "kill_confirmed.case.auto_pickup.on" : "kill_confirmed.case.auto_pickup.off")
                            .withStyle(on ? ChatFormatting.GREEN : ChatFormatting.GRAY),
                    Component.translatable("kill_confirmed.case.auto_pickup.tooltip").withStyle(ChatFormatting.DARK_GRAY)
            ), Optional.empty(), mouseX, mouseY);
        } else if (isInRect(mouseX, mouseY, x + BTN_SORT_X, y + BTN_Y, BTN_SIZE, BTN_SIZE)) {
            g.setTooltipForNextFrame(this.font, List.of(
                    Component.translatable(this.menu.sortMode().translationKey()).withStyle(ChatFormatting.GOLD),
                    Component.translatable("kill_confirmed.case.sort.tooltip").withStyle(ChatFormatting.GRAY)
            ), Optional.empty(), mouseX, mouseY);
        } else if (isInRect(mouseX, mouseY, x + STATS_X, y + STATS_Y, STATS_SIZE, STATS_SIZE)) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("kill_confirmed.case.stats.title").withStyle(ChatFormatting.GOLD));
            lines.add(Component.translatable("kill_confirmed.case.stored",
                    this.menu.filledCount(), DogTagCaseModel.CAPACITY).withStyle(ChatFormatting.GRAY));
            ItemStack caseStack = this.menu.findCaseStack();
            var top = DogTagCaseModel.topVictims(DogTagCaseStorage.readOwnerNames(caseStack), 5);
            if (top.isEmpty()) {
                lines.add(Component.translatable("kill_confirmed.case.stats.empty").withStyle(ChatFormatting.DARK_GRAY));
            } else {
                for (var entry : top) {
                    lines.add(Component.literal("  • ").withStyle(ChatFormatting.DARK_GRAY)
                            .append(Component.translatable("kill_confirmed.case.stats.entry",
                                    entry.name(), entry.count()).withStyle(ChatFormatting.WHITE)));
                }
            }
            g.setTooltipForNextFrame(this.font, lines, Optional.empty(), mouseX, mouseY);
        } else {
            this.extractTooltip(g, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (event.button() == 0) {
            double mouseX = event.x();
            double mouseY = event.y();
            int x = this.leftPos;
            int y = this.topPos;
            if (isInRect(mouseX, mouseY, x + BTN_AUTO_X, y + BTN_Y, BTN_SIZE, BTN_SIZE)) {
                sendButton(DogTagCaseMenu.BUTTON_TOGGLE_AUTO_PICKUP);
                return true;
            }
            if (isInRect(mouseX, mouseY, x + BTN_SORT_X, y + BTN_Y, BTN_SIZE, BTN_SIZE)) {
                sendButton(DogTagCaseMenu.BUTTON_CYCLE_SORT);
                return true;
            }
            if (isInRect(mouseX, mouseY, x + SCROLL_X, y + SCROLL_Y, SCROLL_W, SCROLL_H)) {
                this.draggingScroll = true;
                updateScrollFromMouse(mouseY);
                return true;
            }
        }
        return super.mouseClicked(event, isDoubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (this.draggingScroll) {
            updateScrollFromMouse(event.y());
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        this.draggingScroll = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0) {
            int delta = scrollY > 0 ? -1 : 1;
            int next = DogTagCaseModel.clampScrollRow(this.menu.scrollRow() + delta);
            if (next != this.menu.scrollRow()) {
                sendButton(DogTagCaseMenu.BUTTON_SCROLL_BASE + next);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.searchBox != null && this.searchBox.isFocused()) {
            if (event.key() == 256) {
                this.searchBox.setFocused(false);
                return true;
            }
            return this.searchBox.keyPressed(event);
        }
        return super.keyPressed(event);
    }

    private void updateScrollFromMouse(double mouseY) {
        double rel = (mouseY - (this.topPos + SCROLL_Y) - THUMB_H / 2.0) / (SCROLL_H - THUMB_H);
        int target = Mth.clamp((int) Math.round(rel * DogTagCaseModel.MAX_SCROLL_ROW), 0, DogTagCaseModel.MAX_SCROLL_ROW);
        if (target != this.menu.scrollRow()) {
            sendButton(DogTagCaseMenu.BUTTON_SCROLL_BASE + target);
        }
    }

    private void sendButton(int buttonId) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
        }
    }

    private static boolean isInRect(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static void drawVanillaPanel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x + 2, y, x + w - 2, y + h, 0xFF000000);
        g.fill(x, y + 2, x + w, y + h - 2, 0xFF000000);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF000000);
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, 0xFFC6C6C6);
        g.fill(x + 2, y + 1, x + w - 3, y + 3, 0xFFFFFFFF);
        g.fill(x + 1, y + 2, x + 3, y + h - 3, 0xFFFFFFFF);
        g.fill(x + 3, y + h - 3, x + w - 2, y + h - 1, 0xFF555555);
        g.fill(x + w - 3, y + 3, x + w - 1, y + h - 2, 0xFF555555);
    }

    private static void drawSlotWell(GuiGraphicsExtractor g, int x, int y) {
        drawRecessedBox(g, x, y, 18, 18, 0xFF8B8B8B);
    }

    private static void drawRecessedBox(GuiGraphicsExtractor g, int x, int y, int w, int h, int fill) {
        g.fill(x, y, x + w, y + h, fill);
        g.fill(x, y, x + w - 1, y + 1, 0xFF373737);
        g.fill(x, y, x + 1, y + h - 1, 0xFF373737);
        g.fill(x + 1, y + h - 1, x + w, y + h, 0xFFFFFFFF);
        g.fill(x + w - 1, y + 1, x + w, y + h, 0xFFFFFFFF);
    }

    private static void drawBeveledButton(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean hovered) {
        int fill = hovered ? 0xFFD8D8D8 : 0xFFC6C6C6;
        g.fill(x, y, x + w, y + h, 0xFF373737);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
        g.fill(x + 1, y + 1, x + w - 2, y + 2, 0xFFFFFFFF);
        g.fill(x + 1, y + 1, x + 2, y + h - 2, 0xFFFFFFFF);
        g.fill(x + 2, y + h - 2, x + w - 1, y + h - 1, 0xFF555555);
        g.fill(x + w - 2, y + 2, x + w - 1, y + h - 1, 0xFF555555);
    }
}
