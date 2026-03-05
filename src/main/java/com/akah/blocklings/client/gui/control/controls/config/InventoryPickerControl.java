package com.akah.blocklings.client.gui.control.controls.config;

import com.mojang.blaze3d.vertex.PoseStack;
import com.akah.blocklings.client.gui.control.BaseControl;
import com.akah.blocklings.client.gui.control.Control;
import com.akah.blocklings.client.gui.control.event.events.ItemAddedEvent;
import com.akah.blocklings.client.gui.control.event.events.input.MouseReleasedEvent;
import com.akah.blocklings.client.gui.texture.Textures;
import com.akah.blocklings.client.gui.util.GuiUtil;
import com.akah.blocklings.client.gui.util.ScissorStack;
import com.akah.blocklings.util.event.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public class InventoryPickerControl extends Control
{
    private static final int SLOT_SIZE = 20;
    private static final int SLOT_GAP = 2;
    private static final int STEP = SLOT_SIZE + SLOT_GAP;

    @Nonnull
    public final EventHandler<ItemAddedEvent> onItemSelected = new EventHandler<>();

    @Nonnull
    private List<Item> inventoryItems = new ArrayList<>();

    private int cachedColumns = -1;

    public InventoryPickerControl()
    {
        super();
        setWidthPercentage(1.0);
        setFitHeightToContent(false);
        setClipContentsToBounds(false);
        refreshInventoryItems();
    }

    public void refreshInventoryItems()
    {
        LocalPlayer player = Minecraft.getInstance().player;
        inventoryItems = new ArrayList<>();

        if (player == null)
        {
            updateContentHeight();
            return;
        }

        Set<Item> seen = new LinkedHashSet<>();
        Inventory inv = player.getInventory();

        for (int slot = 0; slot < inv.getContainerSize(); slot++)
        {
            ItemStack stack = inv.getItem(slot);
            if (!stack.isEmpty() && stack.getItem() != Items.AIR)
            {
                seen.add(stack.getItem());
            }
        }

        inventoryItems = new ArrayList<>(seen);
        updateContentHeight();
    }

    @Override
    protected void onRenderUpdate(@Nonnull PoseStack matrixStack, @Nonnull ScissorStack scissorStack, double mouseX, double mouseY, float partialTicks)
    {
        super.onRenderUpdate(matrixStack, scissorStack, mouseX, mouseY, partialTicks);

        int cols = getColumns();
        if (cols != cachedColumns)
        {
            cachedColumns = cols;
            updateContentHeight();
        }
    }

    @Override
    protected void onRender(@Nonnull PoseStack matrixStack, @Nonnull ScissorStack scissorStack, double mouseX, double mouseY, float partialTicks)
    {
        super.onRender(matrixStack, scissorStack, mouseX, mouseY, partialTicks);

        int cols = getColumns();
        if (cols <= 0 || inventoryItems.isEmpty())
        {
            return;
        }

        double pixelScale = getPixelScaleX();
        double guiScale = GuiUtil.get().getGuiScale();
        // NOTE: No manual scrollY subtraction here.
        // getPixelY() already incorporates the parent tab's scrollY via the layout system.
        // Subtracting scrollY again would double-apply the offset.
        int totalRows = getRows();
        int hoveredIndex = getHoveredEntryIndex(mouseX, mouseY);

        for (int row = 0; row < totalRows; row++)
        {
            for (int col = 0; col < cols; col++)
            {
                int index = row * cols + col;
                if (index >= inventoryItems.size())
                {
                    break;
                }

                Item item = inventoryItems.get(index);
                double lx = col * STEP;
                double ly = row * STEP;
                double px = getPixelX() + lx * pixelScale;
                double py = getPixelY() + ly * pixelScale;

                renderTexture(matrixStack, Textures.Tasks.TASK_ICON_BACKGROUND_RAISED, px, py, pixelScale, pixelScale);

                int ix = (int) ((px + SLOT_SIZE * pixelScale / 2.0) / guiScale);
                int iy = (int) ((py + SLOT_SIZE * pixelScale / 2.0) / guiScale);
                GuiUtil.get().renderItemStack(matrixStack, new ItemStack(item), ix, iy, getRenderZ(),
                        0.8f * (float) (SLOT_SIZE * pixelScale / guiScale));

                if (index == hoveredIndex)
                {
                    renderRectangle(matrixStack, px, py, (int) (SLOT_SIZE * pixelScale), (int) (SLOT_SIZE * pixelScale), 0x44ffffff);
                }
            }
        }
    }

    @Override
    protected void onMouseReleased(@Nonnull MouseReleasedEvent e)
    {
        super.onMouseReleased(e);

        int index = getHoveredEntryIndex(e.mouseX, e.mouseY);
        if (index >= 0 && index < inventoryItems.size())
        {
            onItemSelected.handle(new ItemAddedEvent(inventoryItems.get(index)));
            e.setIsHandled(true);
        }
    }

    @Override
    public void onRenderTooltip(@Nonnull PoseStack matrixStack, double mouseX, double mouseY, float partialTicks)
    {
        int index = getHoveredEntryIndex(mouseX, mouseY);
        if (index >= 0 && index < inventoryItems.size())
        {
            renderTooltip(matrixStack, mouseX, mouseY, new ItemStack(inventoryItems.get(index)).getHoverName());
        }
    }

    private int getColumns()
    {
        double w = getWidthWithoutPadding();
        if (w <= 0)
        {
            w = getWidth();
        }

        return Math.max(1, (int) ((w + SLOT_GAP) / STEP));
    }

    private int getRows()
    {
        int cols = getColumns();
        return cols == 0 ? 0 : (inventoryItems.size() + cols - 1) / cols;
    }

    private void updateContentHeight()
    {
        double h = Math.max(0.0, getRows() * STEP - SLOT_GAP);
        setHeight(h);
        // NOTE: Do NOT manually set scrollContainer.setMaxScrollY() here.
        // The tab container computes its own maxScrollY automatically in calculateScroll(),
        // which measures all direct children (including this picker's container chain with
        // the add-button, grid, spacing, and wlPanel margins).  Overriding it with only
        // this picker's height produces a wrong scroll range and immediately snaps scrollY
        // to the wrong position, causing the visible jump/compression when adding items.
    }

    private int getHoveredEntryIndex(double mouseX, double mouseY)
    {
        double pixelScale = getPixelScaleX();
        int cols = getColumns();
        // NOTE: No manual scrollY addition here.
        // getPixelY() already accounts for the parent tab's scroll offset.
        double lx = (mouseX - getPixelX()) / pixelScale;
        double ly = (mouseY - getPixelY()) / pixelScale;

        int col = (int) (lx / STEP);
        int row = (int) (ly / STEP);
        if (col < 0 || col >= cols || row < 0)
        {
            return -1;
        }

        double sx = lx - col * STEP;
        double sy = ly - row * STEP;
        if (sx < 0 || sy < 0 || sx > SLOT_SIZE || sy > SLOT_SIZE)
        {
            return -1;
        }

        int index = row * cols + col;
        return index >= 0 && index < inventoryItems.size() ? index : -1;
    }
}