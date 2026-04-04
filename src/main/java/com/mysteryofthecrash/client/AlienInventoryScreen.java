package com.mysteryofthecrash.client;

import com.mysteryofthecrash.inventory.AlienInventoryMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class AlienInventoryScreen extends AbstractContainerScreen<AlienInventoryMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

    private static final int TOP_ROWS = 4;

    public AlienInventoryScreen(AlienInventoryMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageHeight     = TOP_ROWS * 18 + 114;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int left = (this.width  - this.imageWidth)  / 2;
        int top  = (this.height - this.imageHeight) / 2;

        int topHeight = TOP_ROWS * 18 + 17;
        g.blit(TEXTURE, left, top,             0,   0, this.imageWidth, topHeight);

        g.blit(TEXTURE, left, top + topHeight, 0, 126, this.imageWidth, 96);
    }

    private boolean isMenuInvalid() {
        return this.menu == null || !this.menu.isValid();
    }

    @Override
    public void removed() {
        if (isMenuInvalid()) return;
        super.removed();
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isMenuInvalid()) return false;
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMenuInvalid()) return false;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (isMenuInvalid()) return false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (isMenuInvalid()) {
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.displayClientMessage(
                        Component.literal("§c[Telepathy] The alien is not nearby."), true);
                this.minecraft.setScreen(null);
            }
            return;
        }
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
