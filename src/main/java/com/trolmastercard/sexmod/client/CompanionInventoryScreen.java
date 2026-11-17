package com.trolmastercard.sexmod.client;

import com.trolmastercard.sexmod.FapCraft;
import com.trolmastercard.sexmod.entity.CompanionEntity;
import com.trolmastercard.sexmod.entity.CompanionScene;
import com.trolmastercard.sexmod.menu.CompanionInventoryMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public final class CompanionInventoryScreen extends AbstractContainerScreen<CompanionInventoryMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            FapCraft.MOD_ID,
            "textures/gui/girlinventory.png"
    );

    private Button toggleScene;
    private Button slower;
    private Button faster;
    private Button finish;
    private Button outfit;
    private Button variant;
    private Button scale;
    private Button mode;
    private Button home;
    private final List<Button> sceneButtons = new ArrayList<>();
    private List<CompanionScene> availableScenes = List.of();

    public CompanionInventoryScreen(CompanionInventoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 254;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 118;
    }

    @Override
    protected void init() {
        super.init();
        int x = this.leftPos;
        int y = this.topPos;

        // The 1.12.2 interaction menu exposed each character's actions directly.
        // Keep that discoverable layout while routing every choice through the
        // server-validated scene selection in CompanionInventoryMenu.
        CompanionEntity companion = this.menu.companion();
        List<CompanionScene> scenes = companion == null
                ? List.of()
                : CompanionScene.forKind(companion.kind());
        this.availableScenes = scenes;
        this.sceneButtons.clear();
        for (int index = 0; index < scenes.size(); index++) {
            CompanionScene scene = scenes.get(index);
            int sceneIndex = index;
            int column = index % 2;
            int row = index / 2;
            Button choice = Button.builder(
                    Component.translatable(scene.translationKey()),
                    ignored -> this.sendButton(CompanionInventoryMenu.BUTTON_SCENE_SELECT_BASE + sceneIndex)
            ).bounds(x + 4 + column * 86, y + 30 + row * 22, 82, 20).build();
            this.sceneButtons.add(this.addRenderableWidget(choice));
        }

        this.toggleScene = this.addRenderableWidget(this.button(
                "screen.sexmod.scene_start", x + 4, y + 76, 41, 20,
                CompanionInventoryMenu.BUTTON_SCENE_TOGGLE
        ));
        this.slower = this.addRenderableWidget(this.button(
                "screen.sexmod.scene_slower", x + 47, y + 76, 39, 20,
                CompanionInventoryMenu.BUTTON_SCENE_SLOWER
        ));
        this.faster = this.addRenderableWidget(this.button(
                "screen.sexmod.scene_faster", x + 88, y + 76, 39, 20,
                CompanionInventoryMenu.BUTTON_SCENE_FASTER
        ));
        this.finish = this.addRenderableWidget(this.button(
                "screen.sexmod.scene_finish", x + 129, y + 76, 43, 20,
                CompanionInventoryMenu.BUTTON_SCENE_FINISH
        ));

        this.outfit = this.addRenderableWidget(this.button(
                "editor.sexmod.outfit", x + 4, y + 208, 54, 20,
                CompanionInventoryMenu.BUTTON_OUTFIT
        ));
        this.variant = this.addRenderableWidget(this.button(
                "editor.sexmod.variant", x + 61, y + 208, 54, 20,
                CompanionInventoryMenu.BUTTON_VARIANT
        ));
        this.scale = this.addRenderableWidget(this.button(
                "editor.sexmod.scale", x + 118, y + 208, 54, 20,
                CompanionInventoryMenu.BUTTON_SCALE
        ));
        this.mode = this.addRenderableWidget(this.button(
                "editor.sexmod.movement", x + 4, y + 231, 82, 20,
                CompanionInventoryMenu.BUTTON_MODE
        ));
        this.home = this.addRenderableWidget(this.button(
                "editor.sexmod.home", x + 89, y + 231, 83, 20,
                CompanionInventoryMenu.BUTTON_HOME
        ));
        this.updateButtonState();
    }

    private Button button(String translationKey, int x, int y, int width, int height, int buttonId) {
        return Button.builder(
                Component.translatable(translationKey),
                ignored -> this.sendButton(buttonId)
        ).bounds(x, y, width, height).build();
    }

    private void sendButton(int buttonId) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        this.updateButtonState();
    }

    private void updateButtonState() {
        if (this.toggleScene == null) {
            return;
        }
        CompanionEntity companion = this.menu.companion();
        CompanionScene scene = companion == null ? null : companion.currentScene();
        boolean active = scene != null;
        int phase = active ? companion.currentScenePhase() : -1;
        CompanionScene.Phase currentPhase = active ? scene.phase(phase) : null;
        boolean controllableLoop = currentPhase != null && currentPhase.loop();

        this.toggleScene.setMessage(Component.translatable(
                active ? "screen.sexmod.scene_stop" : "screen.sexmod.scene_start"
        ));
        CompanionScene selectedScene = this.menu.selectedScene();
        for (int index = 0; index < this.sceneButtons.size(); index++) {
            Button choice = this.sceneButtons.get(index);
            boolean selected = selectedScene != null
                    && selectedScene.equals(this.availableScenes.get(index));
            choice.active = companion != null && !active && !selected;
        }
        this.slower.active = controllableLoop && scene.previousLoopPhase(phase) >= 0;
        this.faster.active = controllableLoop && scene.nextLoopPhase(phase) >= 0;
        this.finish.active = controllableLoop;
        this.outfit.active = !active;
        this.variant.active = !active;
        this.scale.active = !active;
        this.mode.active = !active;
        this.home.active = !active;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(
                this.leftPos,
                this.topPos,
                this.leftPos + this.imageWidth,
                this.topPos + this.imageHeight,
                0xE0101010
        );
        graphics.blit(TEXTURE, this.leftPos, this.topPos + 100, 80, 142, this.imageWidth, 114);
        graphics.fill(this.leftPos, this.topPos + 98, this.leftPos + this.imageWidth, this.topPos + 100, 0xFF705030);
        graphics.fill(this.leftPos, this.topPos + 204, this.leftPos + this.imageWidth, this.topPos + 206, 0xFF705030);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        CompanionEntity companion = this.menu.companion();
        CompanionScene activeScene = companion == null ? null : companion.currentScene();
        CompanionScene shownScene = activeScene == null ? this.menu.selectedScene() : activeScene;
        Component sceneName = shownScene == null
                ? Component.translatable("screen.sexmod.scene_none")
                : Component.translatable(shownScene.translationKey());
        graphics.drawCenteredString(this.font, sceneName, this.imageWidth / 2, 10, 0xFFFFFF);

        graphics.drawCenteredString(
                this.font,
                Component.translatable("screen.sexmod.interactions"),
                this.imageWidth / 2,
                21,
                0xFFE0A0
        );
        Component state = activeScene == null
                ? Component.translatable("screen.sexmod.scene_idle")
                : Component.translatable(
                        "screen.sexmod.scene_phase",
                        companion.currentScenePhase() + 1,
                        activeScene.phases().size()
                );
        graphics.drawCenteredString(this.font, state, this.imageWidth / 2, 96, 0xFFE0A0);
        graphics.drawString(
                this.font,
                this.playerInventoryTitle,
                this.inventoryLabelX,
                this.inventoryLabelY,
                0x404040,
                false
        );
    }
}
