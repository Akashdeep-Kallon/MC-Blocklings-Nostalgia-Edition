package com.akah.blocklings.client.gui.control.controls.config;

import com.mojang.blaze3d.vertex.PoseStack;
import com.akah.blocklings.client.gui.control.BaseControl;
import com.akah.blocklings.client.gui.control.Control;
import com.akah.blocklings.client.gui.control.controls.TextBlockControl;
import com.akah.blocklings.client.gui.control.controls.TexturedControl;
import com.akah.blocklings.client.gui.control.controls.panels.StackPanel;
import com.akah.blocklings.client.gui.control.event.events.ItemAddedEvent;
import com.akah.blocklings.client.gui.control.event.events.input.MouseReleasedEvent;
import com.akah.blocklings.client.gui.properties.Visibility;
import com.akah.blocklings.client.gui.texture.Textures;
import com.akah.blocklings.client.gui.util.GuiUtil;
import com.akah.blocklings.client.gui.util.ScissorStack;
import com.akah.blocklings.entity.blockling.goal.config.whitelist.GoalWhitelist;
import com.akah.blocklings.util.BlocklingsComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@OnlyIn(Dist.CLIENT)
public class WhitelistItemListControl extends Control
{
    private static final int SLOT_SIZE = 20;
    private static final int SLOT_GAP = 2;
    private static final int STEP = SLOT_SIZE + SLOT_GAP;

    @Nonnull
    private final GoalWhitelist whitelist;

    @Nonnull
    private List<ResourceLocation> activeEntries = new ArrayList<>();

    @Nonnull
    private final StackPanel mainPanel;

    @Nonnull
    private final GridControl gridControl;

    @Nonnull
    private final TextBlockControl emptyLabel;

    @Nullable
    private InventoryPickerControl inventoryPicker;

    @Nullable
    private final Runnable onWhitelistChanged;

    private int cachedGridColumns = -1;

    public WhitelistItemListControl(@Nonnull GoalWhitelist whitelist, @Nullable BaseControl scrollContainer)
    {
        this(whitelist, scrollContainer, null);
    }

    public WhitelistItemListControl(@Nonnull GoalWhitelist whitelist, @Nullable BaseControl scrollContainer, @Nullable Runnable onWhitelistChanged)
    {
        this.whitelist = whitelist;
        this.onWhitelistChanged = onWhitelistChanged;

        setWidthPercentage(1.0);
        setFitHeightToContent(true);
        setScrollFromDragControl(scrollContainer);

        mainPanel = new StackPanel();
        mainPanel.setParent(this);
        mainPanel.setWidthPercentage(1.0);
        mainPanel.setFitHeightToContent(true);
        mainPanel.setSpacing(4.0);
        mainPanel.setScrollFromDragControl(scrollContainer);

        buildAddButton().setParent(mainPanel);

        emptyLabel = new TextBlockControl();
        emptyLabel.setText(new BlocklingsComponent("config.whitelist.empty"));
        emptyLabel.setParent(mainPanel);
        emptyLabel.setWidthPercentage(1.0);
        emptyLabel.setHorizontalContentAlignment(0.5);
        emptyLabel.setMargins(0.0, 2.0, 0.0, 2.0);

        gridControl = new GridControl();
        gridControl.setParent(mainPanel);
        gridControl.setWidthPercentage(1.0);
        gridControl.setClipContentsToBounds(false);
        gridControl.setScrollFromDragControl(scrollContainer);

        rebuildActiveEntries();
        refreshEmptyState();
        gridControl.updateGridHeight();
    }

    private class GridControl extends Control
    {
        @Override
        protected void onRenderUpdate(@Nonnull PoseStack matrixStack, @Nonnull ScissorStack scissorStack, double mouseX, double mouseY, float partialTicks)
        {
            super.onRenderUpdate(matrixStack, scissorStack, mouseX, mouseY, partialTicks);

            int cols = getColumnCount();
            if (cols != cachedGridColumns)
            {
                cachedGridColumns = cols;
                updateGridHeight();
            }
        }

        @Override
        protected void onRender(@Nonnull PoseStack matrixStack, @Nonnull ScissorStack scissorStack, double mouseX, double mouseY, float partialTicks)
        {
            super.onRender(matrixStack, scissorStack, mouseX, mouseY, partialTicks);
            renderActiveItems(matrixStack, mouseX, mouseY);
        }

        @Override
        public void onRenderTooltip(@Nonnull PoseStack matrixStack, double mouseX, double mouseY, float partialTicks)
        {
            int index = getHoveredGridIndex(mouseX, mouseY);
            if (index >= 0 && index < activeEntries.size())
            {
                Item item = BuiltInRegistries.ITEM.get(activeEntries.get(index));
                List<FormattedCharSequence> tooltip = new ArrayList<>();
                tooltip.add(new ItemStack(item).getHoverName().getVisualOrderText());
                tooltip.add(new BlocklingsComponent("config.whitelist.click_to_remove").withStyle(ChatFormatting.GRAY).getVisualOrderText());
                renderTooltip(matrixStack, mouseX, mouseY, tooltip);
            }
        }

        @Override
        protected void onMouseReleased(@Nonnull MouseReleasedEvent e)
        {
            super.onMouseReleased(e);

            int index = getHoveredGridIndex(e.mouseX, e.mouseY);
            if (index >= 0 && index < activeEntries.size())
            {
                ResourceLocation rl = activeEntries.get(index);
                whitelist.setEntry(rl, false, true);
                rebuildActiveEntries();
                refreshEmptyState();
                updateGridHeight();
                notifyWhitelistChanged();
                e.setIsHandled(true);
            }
        }

        private int getColumnCount()
        {
            double w = getWidthWithoutPadding();
            if (w <= 0)
            {
                w = getWidth();
            }

            return Math.max(1, (int) ((w + SLOT_GAP) / STEP));
        }

        private int getRowCount()
        {
            int cols = getColumnCount();
            return cols == 0 ? 0 : (activeEntries.size() + cols - 1) / cols;
        }

        void updateGridHeight()
        {
            double h = Math.max(0.0, getRowCount() * STEP - SLOT_GAP);
            setHeight(h);
            // NOTE: Do NOT manually set scrollContainer.setMaxScrollY() here.
            // The tab container (scrollContainer) computes its own maxScrollY automatically
            // during layout by measuring all its direct children (wlPanel with margins,
            // add button, spacing, etc.).  Overriding it with only the grid height
            // ignores those extras and produces a wrong scroll range.
        }

        private void renderActiveItems(@Nonnull PoseStack matrixStack, double mouseX, double mouseY)
        {
            int cols = getColumnCount();
            if (cols <= 0 || activeEntries.isEmpty())
            {
                return;
            }

            double pixelScale = getPixelScaleX();
            double guiScale = GuiUtil.get().getGuiScale();
            int hoveredIndex = getHoveredGridIndex(mouseX, mouseY);

            // NOTE: No manual scrollY subtraction here.
            // getPixelY() already incorporates the parent tab's scrollY via the layout system
            // (BaseControl.toPixelY subtracts parent.scrollY when computing child pixel positions).
            // Subtracting scrollY again would double-apply the offset, causing the whole panel to
            // shift instead of just scrolling the item list.
            int totalRows = getRowCount();
            for (int row = 0; row < totalRows; row++)
            {
                for (int col = 0; col < cols; col++)
                {
                    int index = row * cols + col;
                    if (index >= activeEntries.size())
                    {
                        break;
                    }

                    ResourceLocation rl = activeEntries.get(index);
                    double lx = col * STEP;
                    double ly = row * STEP;

                    double px = getPixelX() + lx * pixelScale;
                    double py = getPixelY() + ly * pixelScale;

                    renderTexture(matrixStack, Textures.Tasks.TASK_ICON_BACKGROUND_RAISED, px, py, pixelScale, pixelScale);

                    Item item = BuiltInRegistries.ITEM.get(rl);
                    int ix = (int) ((px + SLOT_SIZE * pixelScale / 2.0) / guiScale);
                    int iy = (int) ((py + SLOT_SIZE * pixelScale / 2.0) / guiScale);
                    GuiUtil.get().renderItemStack(matrixStack, new ItemStack(item), ix, iy, getRenderZ(),
                            0.8f * (float) (SLOT_SIZE * pixelScale / guiScale));

                    if (index == hoveredIndex)
                    {
                        renderRectangle(matrixStack, px, py,
                                (int) (SLOT_SIZE * pixelScale), (int) (SLOT_SIZE * pixelScale), 0x66cc0000);
                        renderTexture(matrixStack, Textures.Common.CROSS_ICON,
                                px + (SLOT_SIZE * pixelScale - Textures.Common.CROSS_ICON.width * pixelScale) / 2.0,
                                py + (SLOT_SIZE * pixelScale - Textures.Common.CROSS_ICON.height * pixelScale) / 2.0,
                                pixelScale, pixelScale);
                    }
                }
            }
        }

        int getHoveredGridIndex(double mouseX, double mouseY)
        {
            double pixelScale = getPixelScaleX();
            int cols = getColumnCount();
            // NOTE: No manual scrollY addition here.
            // getPixelY() already accounts for the parent tab's scroll offset, so
            // (mouseY - getPixelY()) gives correct local coordinates directly.
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
            return index >= 0 && index < activeEntries.size() ? index : -1;
        }
    }

    private Control buildAddButton()
    {
        Control container = new Control();
        container.setWidthPercentage(1.0);
        container.setFitHeightToContent(true);

        TexturedControl addBg = new TexturedControl(Textures.Common.PLUS_ICON)
        {
            @Override
            public void onRenderTooltip(@Nonnull PoseStack matrixStack, double mouseX, double mouseY, float partialTicks)
            {
                renderTooltip(matrixStack, mouseX, mouseY, new BlocklingsComponent("config.whitelist.add_item"));
            }

            @Override
            protected void onMouseReleased(@Nonnull MouseReleasedEvent e)
            {
                if (isPressed())
                {
                    togglePicker();
                    e.setIsHandled(true);
                }
            }
        };
        addBg.setParent(container);
        addBg.setHorizontalAlignment(0.5);
        addBg.setMargins(0.0, 1.0, 0.0, 1.0);

        return container;
    }

    private void togglePicker()
    {
        if (inventoryPicker != null && inventoryPicker.getParent() != null)
        {
            closePicker();
        }
        else
        {
            openPicker();
        }
    }

    private void openPicker()
    {
        if (inventoryPicker == null)
        {
            inventoryPicker = new InventoryPickerControl();
            inventoryPicker.setScrollFromDragControl(getScrollFromDragControl());
            inventoryPicker.setWidthPercentage(1.0);
            inventoryPicker.onItemSelected.subscribe((ItemAddedEvent evt) ->
            {
                ResourceLocation rl = BuiltInRegistries.ITEM.getKey(evt.item);
                whitelist.addOrSetEntry(rl, true, true);
                rebuildActiveEntries();
                refreshEmptyState();
                gridControl.updateGridHeight();
                notifyWhitelistChanged();
                closePicker();
            });
        }
        else
        {
            inventoryPicker.refreshInventoryItems();
        }

        inventoryPicker.setParent(mainPanel);
    }

    private void closePicker()
    {
        if (inventoryPicker != null)
        {
            inventoryPicker.setParent(null);
        }
    }

    private void rebuildActiveEntries()
    {
        activeEntries = whitelist.entrySet().stream()
                .filter(e -> Boolean.TRUE.equals(e.getValue()))
                .filter(e ->
                {
                    Item item = BuiltInRegistries.ITEM.get(e.getKey());
                    return item != null && item != Items.AIR;
                })
                .sorted(Comparator.comparing((Map.Entry<ResourceLocation, Boolean> e) ->
                        new ItemStack(BuiltInRegistries.ITEM.get(e.getKey())).getHoverName().getString().toLowerCase(Locale.ROOT)))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private void refreshEmptyState()
    {
        boolean empty = activeEntries.isEmpty();
        emptyLabel.setVisibility(empty ? Visibility.VISIBLE : Visibility.COLLAPSED);
        gridControl.setVisibility(empty ? Visibility.COLLAPSED : Visibility.VISIBLE);
    }

    private void notifyWhitelistChanged()
    {
        if (onWhitelistChanged != null)
        {
            onWhitelistChanged.run();
        }
    }
}