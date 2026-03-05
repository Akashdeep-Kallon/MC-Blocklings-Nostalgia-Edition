package com.akah.blocklings.client.gui.screen;

import net.minecraft.client.gui.GuiGraphics;
import com.akah.blocklings.client.gui.control.controls.ScreenControl;
import com.akah.blocklings.client.gui.control.event.events.input.*;
import com.akah.blocklings.client.gui.util.GuiUtil;
import com.akah.blocklings.entity.blockling.BlocklingEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;

/**
 * A base screen to provide an adapter for {@link AbstractContainerScreen}.
 */
@OnlyIn(Dist.CLIENT)
public class BlocklingsAbstractContainerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T>
{
    /**
     * The blockling associated with the screen.
     */
    @Nonnull
    public final BlocklingEntity blockling;

    /**
     * The player.
     */
    @Nonnull
    private final Player player;

    /**
     * The root control that contains all the sub controls on the screen.
     */
    @Nonnull
    public final ScreenControl screenControl = new ScreenControl();

    /**
     * @param blockling the blockling associated with the screen.
     * @param container the container.
     */
    protected BlocklingsAbstractContainerScreen(@Nonnull BlocklingEntity blockling, @Nonnull T container)
    {
        super(container, Minecraft.getInstance().player.getInventory(), Component.literal(""));
        this.blockling = blockling;
        this.player = Minecraft.getInstance().player;
    }

    @Override
    protected void init()
    {
        super.init();

        screenControl.setWidth(width);
        screenControl.setHeight(height);
        screenControl.markMeasureDirty(true);
        screenControl.markArrangeDirty(true);
    }

    @Override
    public void onClose()
    {
        super.onClose();

        screenControl.forwardClose(screenControl.shouldReallyClose());
    }

    @Override
    protected void containerTick()
    {
        screenControl.forwardTick();
        super.containerTick();
    }

    @Override
    protected void renderBg(@Nonnull GuiGraphics graphics, float partialTicks, int mouseX, int mouseY)
    {

    }

    @Override
    public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks)
    {
        screenControl.render(graphics.pose(), mouseX, mouseY, partialTicks);

        super.render(graphics, mouseX, mouseY, partialTicks);

        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(@Nonnull GuiGraphics graphics, int screenMouseX, int screenMouseY)
    {

    }

    @Override
    public boolean mouseClicked(double screenMouseX, double screenMouseY, int button)
    {
        double mouseX = GuiUtil.get().getPixelMouseX();
        double mouseY = GuiUtil.get().getPixelMouseY();

        MouseClickedEvent e = new MouseClickedEvent(mouseX, mouseY, button);

        screenControl.forwardMouseClicked(e);

        if (e.isHandled() || super.mouseClicked(screenMouseX, screenMouseY, button))
        {
            return true;
        }
        else
        {
            screenControl.setPressed(true);
            screenControl.setFocused(true);

            return false;
        }
    }

    @Override
    public boolean mouseReleased(double screenMouseX, double screenMouseY, int button)
    {
        double mouseX = GuiUtil.get().getPixelMouseX();
        double mouseY = GuiUtil.get().getPixelMouseY();

        MouseReleasedEvent e = new MouseReleasedEvent(mouseX, mouseY, button);

        screenControl.forwardMouseReleased(e);

        return e.isHandled() || super.mouseReleased(screenMouseX, screenMouseY, button);
    }

    @Override
    public boolean mouseScrolled(double screenMouseX, double screenMouseY, double amount)
    {
        double mouseX = GuiUtil.get().getPixelMouseX();
        double mouseY = GuiUtil.get().getPixelMouseY();

        MouseScrolledEvent e = new MouseScrolledEvent(mouseX, mouseY, amount);

        screenControl.forwardMouseScrolled(e);

        return e.isHandled() || super.mouseScrolled(screenMouseX, screenMouseY, amount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        KeyPressedEvent e = new KeyPressedEvent(keyCode, scanCode, modifiers);

        screenControl.forwardGlobalKeyPressed(e);

        return e.isHandled() || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers)
    {
        KeyReleasedEvent e = new KeyReleasedEvent(keyCode, scanCode, modifiers);

        screenControl.forwardGlobalKeyReleased(e);

        return e.isHandled() || super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char character, int modifiers)
    {
        CharTypedEvent e = new CharTypedEvent(character, modifiers);

        screenControl.forwardGlobalCharTyped(e);

        return e.isHandled() || super.charTyped(character, modifiers);
    }
}
