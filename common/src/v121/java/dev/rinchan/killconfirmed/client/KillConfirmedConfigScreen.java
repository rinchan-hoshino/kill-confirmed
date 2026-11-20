package dev.rinchan.killconfirmed.client;

import dev.rinchan.killconfirmed.KillConfirmed;
import dev.rinchan.killconfirmed.KillConfirmedConfig;
import dev.rinchan.killconfirmed.portable.DropStrategy;
import dev.rinchan.killconfirmed.portable.TemplateDocument;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Native zero-dependency configuration screen for Kill Confirmed (1.21.1). */
public final class KillConfirmedConfigScreen extends Screen {
    private final Screen parent;
    private DropStrategy dropStrategy;
    private boolean playerKillsOnly;
    private boolean fireResistant;
    private String cooldownText;
    private boolean enableCaseRecipe;
    private boolean enableCaseLoot;
    private boolean enableWandererTrade;
    private String functionText;

    private EditBox cooldownBox;
    private EditBox functionBox;

    public KillConfirmedConfigScreen(Screen parent) {
        super(Component.translatable("screen.kill_confirmed.title"));
        this.parent = parent;
        loadFrom(KillConfirmed.config());
    }

    private void loadFrom(KillConfirmedConfig cfg) {
        this.dropStrategy = cfg.dropStrategy();
        this.playerKillsOnly = cfg.playerKillsOnly();
        this.fireResistant = cfg.fireResistant();
        this.cooldownText = Integer.toString(cfg.cooldownSeconds());
        this.enableCaseRecipe = cfg.enableDogTagCaseRecipe();
        this.enableCaseLoot = cfg.enableCaseLoot();
        this.enableWandererTrade = cfg.enableWandererTrade();
        this.functionText = cfg.function();
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int colW = Math.min(155, (this.width - 36) / 2);
        int leftX = centerX - colW - 4;
        int rightX = centerX + 4;
        int fullW = colW * 2 + 8;
        int y = 34;

        this.addRenderableWidget(CycleButton.<DropStrategy>builder(strategy -> Component.translatable(
                        strategy == DropStrategy.ON_RESPAWN_INVENTORY
                                ? "screen.kill_confirmed.drop_strategy.respawn_inventory"
                                : "screen.kill_confirmed.drop_strategy.death_position"))
                .withValues(DropStrategy.AT_DEATH_POSITION, DropStrategy.ON_RESPAWN_INVENTORY)
                .withInitialValue(this.dropStrategy)
                .create(leftX, y, colW, 20,
                        Component.translatable("screen.kill_confirmed.drop_strategy"),
                        (btn, value) -> this.dropStrategy = value));

        this.addRenderableWidget(CycleButton.onOffBuilder(this.playerKillsOnly)
                .create(rightX, y, colW, 20,
                        Component.translatable("screen.kill_confirmed.player_kills_only"),
                        (btn, value) -> this.playerKillsOnly = value));
        y += 24;

        this.addRenderableWidget(CycleButton.onOffBuilder(this.fireResistant)
                .create(leftX, y, colW, 20,
                        Component.translatable("screen.kill_confirmed.fire_resistant"),
                        (btn, value) -> this.fireResistant = value));

        this.addRenderableWidget(CycleButton.onOffBuilder(this.enableCaseRecipe)
                .create(rightX, y, colW, 20,
                        Component.translatable("screen.kill_confirmed.enable_case_recipe"),
                        (btn, value) -> this.enableCaseRecipe = value));
        y += 24;

        this.addRenderableWidget(CycleButton.onOffBuilder(this.enableCaseLoot)
                .create(leftX, y, colW, 20,
                        Component.translatable("screen.kill_confirmed.enable_case_loot"),
                        (btn, value) -> this.enableCaseLoot = value));

        this.addRenderableWidget(CycleButton.onOffBuilder(this.enableWandererTrade)
                .create(rightX, y, colW, 20,
                        Component.translatable("screen.kill_confirmed.enable_wanderer_trade"),
                        (btn, value) -> this.enableWandererTrade = value));
        y += 34;

        this.cooldownBox = new EditBox(this.font, leftX, y, colW, 20,
                Component.translatable("screen.kill_confirmed.cooldown_seconds"));
        this.cooldownBox.setMaxLength(6);
        this.cooldownBox.setValue(this.cooldownText);
        this.cooldownBox.setFilter(s -> s.isEmpty() || s.matches("\\d+"));
        this.cooldownBox.setResponder(s -> this.cooldownText = s);
        this.addRenderableWidget(this.cooldownBox);

        this.functionBox = new EditBox(this.font, rightX, y, colW, 20,
                Component.translatable("screen.kill_confirmed.function"));
        this.functionBox.setMaxLength(128);
        this.functionBox.setValue(this.functionText);
        this.functionBox.setHint(Component.translatable("screen.kill_confirmed.function_hint"));
        this.functionBox.setResponder(s -> this.functionText = s);
        this.addRenderableWidget(this.functionBox);

        int btnY = Math.min(this.height - 28, y + 36);
        int btnW = (fullW - 12) / 3;

        this.addRenderableWidget(Button.builder(
                        Component.translatable("screen.kill_confirmed.reset"),
                        btn -> {
                            loadFrom(KillConfirmedConfig.defaults());
                            this.rebuildWidgets();
                        })
                .bounds(leftX, btnY, btnW, 20)
                .build());

        this.addRenderableWidget(Button.builder(
                        Component.translatable("screen.kill_confirmed.cancel"),
                        btn -> onClose())
                .bounds(leftX + btnW + 6, btnY, btnW, 20)
                .build());

        this.addRenderableWidget(Button.builder(
                        Component.translatable("screen.kill_confirmed.save"),
                        btn -> saveAndClose())
                .bounds(leftX + (btnW + 6) * 2, btnY, fullW - (btnW + 6) * 2, 20)
                .build());
    }

    private void saveAndClose() {
        int cooldown = 0;
        try {
            if (!this.cooldownText.isBlank()) {
                cooldown = Math.max(0, Integer.parseInt(this.cooldownText.trim()));
            }
        } catch (NumberFormatException ignored) {
            cooldown = 0;
        }
        String fn = this.functionText == null ? "" : this.functionText.trim();
        if (!fn.isEmpty() && !fn.matches("[a-z0-9_.-]+:[a-z0-9/._-]+")) {
            fn = KillConfirmed.config().function();
        }
        KillConfirmedConfig existing = KillConfirmed.config();
        KillConfirmedConfig updated = new KillConfirmedConfig(
                this.dropStrategy,
                this.playerKillsOnly,
                this.fireResistant,
                cooldown,
                this.enableCaseRecipe,
                this.enableCaseLoot,
                this.enableWandererTrade,
                fn,
                existing.loreLines(),
                new TemplateDocument(existing.loreLines()));
        KillConfirmed.saveConfig(updated);
        onClose();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(this.font, this.title, this.width / 2, 14, 0xFFFFFF);
        if (this.cooldownBox != null) {
            g.drawString(this.font, Component.translatable("screen.kill_confirmed.cooldown_seconds"),
                    this.cooldownBox.getX(), this.cooldownBox.getY() - 10, 0xA0A0A0);
        }
        if (this.functionBox != null) {
            g.drawString(this.font, Component.translatable("screen.kill_confirmed.function"),
                    this.functionBox.getX(), this.functionBox.getY() - 10, 0xA0A0A0);
        }
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }
}
