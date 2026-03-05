package com.akah.blocklings.client.gui.control.controls.config;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.akah.blocklings.client.gui.control.BaseControl;
import com.akah.blocklings.client.gui.control.Control;
import com.akah.blocklings.client.gui.control.controls.NullableIntFieldControl;
import com.akah.blocklings.client.gui.control.controls.ItemControl;
import com.akah.blocklings.client.gui.control.controls.TextBlockControl;
import com.akah.blocklings.client.gui.control.controls.TexturedControl;
import com.akah.blocklings.client.gui.control.controls.panels.GridPanel;
import com.akah.blocklings.client.gui.control.controls.panels.StackPanel;
import com.akah.blocklings.client.gui.control.event.events.*;
import com.akah.blocklings.client.gui.control.event.events.input.KeyPressedEvent;
import com.akah.blocklings.client.gui.control.event.events.input.MouseReleasedEvent;
import com.akah.blocklings.client.gui.properties.GridDefinition;
import com.akah.blocklings.client.gui.properties.Visibility;
import com.akah.blocklings.client.gui.texture.Texture;
import com.akah.blocklings.client.gui.texture.Textures;
import com.akah.blocklings.client.gui.util.GuiUtil;
import com.akah.blocklings.client.gui.util.ScissorStack;
import com.akah.blocklings.entity.blockling.goal.config.iteminfo.*;
import com.akah.blocklings.entity.blockling.goal.config.whitelist.GoalWhitelist;
import com.akah.blocklings.util.BlocklingsComponent;
import com.akah.blocklings.util.event.ValueChangedEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.ChatFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * A control used to select items.
 */
@OnlyIn(Dist.CLIENT)
public abstract class ItemsConfigurationControl extends Control
{
    /**
     * The associated item info set.
     */
    @Nonnull
    public final OrderedItemInfoSet itemInfoSet;

    /**
     * Whether the configuration is for taking items or depositing items.
     */
    public final boolean isTakeItems;

    /**
     * The max amount of items.
     */
    private int maxItems = 40;

    /**
     * @param itemInfoSet the associated item info set.
     * @param isTakeItems whether the configuration is for taking items or depositing items.
     */
    public ItemsConfigurationControl(@Nonnull OrderedItemInfoSet itemInfoSet, boolean isTakeItems)
    {
        super();
        this.itemInfoSet = itemInfoSet;
        this.isTakeItems = isTakeItems;

        setWidthPercentage(1.0);
        setFitHeightToContent(true);

        itemInfoSet.eventBus.subscribe((OrderedItemInfoSet s, ItemInfoAddedEvent e) ->
        {
            addItemInfo(e.itemInfo);
        });

        itemInfoSet.eventBus.subscribe((OrderedItemInfoSet s, ItemInfoRemovedEvent e) ->
        {
            removeItemInfo(e.itemInfo);
        });

        itemInfoSet.eventBus.subscribe((OrderedItemInfoSet s, ItemInfoMovedEvent e) ->
        {
            moveItemInfo(e.movedItemInfo, e.closestItemInfo, e.insertBefore);
        });
    }

    /**
     * @param itemInfo the item info to get the control for.
     * @return the control for the item info.
     */
    @Nullable
    protected abstract ItemInfoControl getItemInfoControl(@Nonnull ItemInfo itemInfo);

    /**
     * Adds an item info to the list.
     *
     * @param itemInfo the item info to add.
     */
    protected abstract void addItemInfo(@Nonnull ItemInfo itemInfo);

    /**
     * Removes an item info from the list.
     *
     * @param itemInfo the item info to remove.
     */
    protected abstract void removeItemInfo(@Nonnull ItemInfo itemInfo);

    /**
     * Moves an item info in the list.
     *
     * @param movedItemInfo the item info to move.
     * @param closestItemInfo the item info to move the moved item info relative to.
     * @param insertBefore whether to insert the moved item info before the closest item info.
     */
    protected abstract void moveItemInfo(@Nonnull ItemInfo movedItemInfo, @Nonnull ItemInfo closestItemInfo, boolean insertBefore);

    /**
     * @return the max amount of items.
     */
    public int getMaxItems()
    {
        return maxItems;
    }

    /**
     * Sets the max amount of items.
     *
     * @param maxItems the max amount of items.
     */
    public void setMaxItems(int maxItems)
    {
        this.maxItems = maxItems;
    }

    /**
     * An item info config control.
     */
    private static class ItemInfoControl extends Control
    {
        /**
         * The item info to display.
         */
        @Nonnull
        public final ItemInfo itemInfo;

        /**
         * @param itemInfo the item info to display.
         */
        public ItemInfoControl(@Nonnull ItemInfo itemInfo)
        {
            super();
            this.itemInfo = itemInfo;
        }
    }

    /**
     * A simple items configuration control.
     */
    public static class SimpleItemsConfigurationControl extends WhitelistDisplayControl
    {
        public SimpleItemsConfigurationControl(@Nonnull OrderedItemInfoSet itemInfoSet, boolean isTakeItems)
        {
            super(itemInfoSet, isTakeItems, null);
        }
    }

    /**
     * A whitelist display configuration control.
     */
    public static class WhitelistDisplayControl extends ItemsConfigurationControl
    {
        private static final int SLOT_SIZE = 20;
        private static final int SLOT_GAP = 2;
        private static final int STEP = SLOT_SIZE + SLOT_GAP;

        @Nullable
        private final GoalWhitelist whitelist;

        @Nonnull
        private List<ResourceLocation> sortedEntries = new ArrayList<>();

        private int cachedColumns = -1;

        public WhitelistDisplayControl(@Nonnull OrderedItemInfoSet itemInfoSet, boolean isTakeItems, @Nullable GoalWhitelist whitelist)
        {
            super(itemInfoSet, isTakeItems);
            this.whitelist = whitelist;

            setWidthPercentage(1.0);
            setFitHeightToContent(false);
            setClipContentsToBounds(false);

            rebuildSortedList();
            updateContentHeight();
        }

        @Override
        protected void onRenderUpdate(@Nonnull PoseStack matrixStack, @Nonnull ScissorStack scissorStack, double mouseX, double mouseY, float partialTicks)
        {
            super.onRenderUpdate(matrixStack, scissorStack, mouseX, mouseY, partialTicks);

            int columns = getColumns();
            if (columns != cachedColumns)
            {
                cachedColumns = columns;
                updateContentHeight();
            }
        }

        @Override
        protected void onRender(@Nonnull PoseStack matrixStack, @Nonnull ScissorStack scissorStack, double mouseX, double mouseY, float partialTicks)
        {
            super.onRender(matrixStack, scissorStack, mouseX, mouseY, partialTicks);

            int columns = getColumns();
            int rows = getRows();
            if (rows <= 0)
            {
                return;
            }

            double pixelScale = getPixelScaleX();
            double guiScale = GuiUtil.get().getGuiScale();
            // NOTE: No manual scrollY subtraction here.
            // getPixelY() already incorporates the parent tab's scrollY via the layout system.
            // Subtracting scrollY again would double-apply the offset.

            for (int row = 0; row < rows; row++)
            {
                for (int col = 0; col < columns; col++)
                {
                    int index = row * columns + col;
                    if (index >= sortedEntries.size())
                    {
                        break;
                    }

                    ResourceLocation entry = sortedEntries.get(index);
                    boolean enabled = whitelist != null && whitelist.isEntryWhitelisted(entry);

                    double localX = col * STEP;
                    double localY = row * STEP;

                    double pixelX = getPixelX() + localX * pixelScale;
                    double pixelY = getPixelY() + localY * pixelScale;

                    renderTexture(matrixStack, Textures.Tasks.TASK_ICON_BACKGROUND_RAISED, pixelX, pixelY, pixelScale, pixelScale);

                    Item item = BuiltInRegistries.ITEM.get(entry);
                    ItemStack stack = new ItemStack(item);
                    int itemX = (int) ((pixelX + SLOT_SIZE * pixelScale / 2.0) / guiScale);
                    int itemY = (int) ((pixelY + SLOT_SIZE * pixelScale / 2.0) / guiScale);
                    GuiUtil.get().renderItemStack(matrixStack, stack, itemX, itemY, getRenderZ(), 0.8f * (float) (SLOT_SIZE * pixelScale / guiScale));

                    if (!enabled)
                    {
                        renderRectangle(matrixStack, pixelX, pixelY, (int) (SLOT_SIZE * pixelScale), (int) (SLOT_SIZE * pixelScale), 0x88000000);
                    }
                }
            }
        }

        @Override
        protected void onMouseReleased(@Nonnull MouseReleasedEvent e)
        {
            super.onMouseReleased(e);

            int index = getHoveredEntryIndex(e.mouseX, e.mouseY);
            if (index < 0 || index >= sortedEntries.size())
            {
                return;
            }

            if (whitelist == null)
            {
                return;
            }

            whitelist.toggleEntry(sortedEntries.get(index));
            rebuildSortedList();
            updateContentHeight();
            e.setIsHandled(true);
        }

        @Override
        public void onRenderTooltip(@Nonnull PoseStack matrixStack, double mouseX, double mouseY, float partialTicks)
        {
            int index = getHoveredEntryIndex(mouseX, mouseY);
            if (index < 0 || index >= sortedEntries.size())
            {
                return;
            }

            Item item = BuiltInRegistries.ITEM.get(sortedEntries.get(index));
            renderTooltip(matrixStack, mouseX, mouseY, new ItemStack(item).getHoverName());
        }

        @Nullable
        @Override
        protected ItemInfoControl getItemInfoControl(@Nonnull ItemInfo itemInfo)
        {
            return null;
        }

        @Override
        protected void addItemInfo(@Nonnull ItemInfo itemInfo)
        {
        }

        @Override
        protected void removeItemInfo(@Nonnull ItemInfo itemInfo)
        {
        }

        @Override
        protected void moveItemInfo(@Nonnull ItemInfo movedItemInfo, @Nonnull ItemInfo closestItemInfo, boolean insertBefore)
        {
        }

        private void rebuildSortedList()
        {
            if (whitelist == null)
            {
                sortedEntries = new ArrayList<>();
                return;
            }

            sortedEntries = whitelist.entrySet().stream()
                    .filter(e ->
                    {
                        Item item = BuiltInRegistries.ITEM.get(e.getKey());
                        return item != null && item != Items.AIR;
                    })
                    .sorted(Comparator
                            .<Map.Entry<ResourceLocation, Boolean>>comparingInt(e -> e.getValue() ? 0 : 1)
                            .thenComparing(e -> new ItemStack(BuiltInRegistries.ITEM.get(e.getKey()))
                                    .getHoverName().getString().toLowerCase(Locale.ROOT)))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
        }

        private int getColumns()
        {
            double availableWidth = getWidthWithoutPadding();
            if (availableWidth <= 0.0)
            {
                availableWidth = getWidth();
            }
            if (availableWidth <= 0.0)
            {
                return 1;
            }

            return Math.max(1, (int) ((availableWidth + SLOT_GAP) / STEP));
        }

        private int getRows()
        {
            int columns = getColumns();
            return columns == 0 ? 0 : (sortedEntries.size() + columns - 1) / columns;
        }

        private void updateContentHeight()
        {
            double totalHeight = Math.max(0.0, getRows() * STEP - SLOT_GAP);
            setHeight(totalHeight);
            // NOTE: Do NOT manually set scrollContainer.setMaxScrollY() here.
            // The tab container computes its own maxScrollY automatically in calculateScroll().
        }

        private int getHoveredEntryIndex(double mouseX, double mouseY)
        {
            double pixelScale = getPixelScaleX();
            // NOTE: No manual scrollY addition here.
            // getPixelY() already accounts for the parent tab's scroll offset.
            int columns = getColumns();

            double localX = (mouseX - getPixelX()) / pixelScale;
            double localY = (mouseY - getPixelY()) / pixelScale;

            int col = (int) (localX / STEP);
            int row = (int) (localY / STEP);

            if (col < 0 || col >= columns || row < 0)
            {
                return -1;
            }

            double slotLocalX = localX - col * STEP;
            double slotLocalY = localY - row * STEP;

            if (slotLocalX < 0.0 || slotLocalY < 0.0 || slotLocalX > SLOT_SIZE || slotLocalY > SLOT_SIZE)
            {
                return -1;
            }

            int index = row * columns + col;
            if (index < 0 || index >= sortedEntries.size())
            {
                return -1;
            }

            return index;
        }
    }

    /**
     * An advanced items configuration control.
     */
    public static class AdvancedItemsConfigurationControl extends ItemsConfigurationControl
    {
        private final boolean allowItemManagement;

        /**
         * The items panel.
         */
        @Nonnull
        private final StackPanel itemsPanel;

        /**
         * The add item container.
         */
        @Nullable
        private final Control addItemContainer;

        /**
         * The item search control.
         */
        @Nullable
        private final ItemSearchControl itemSearchControl;

        /**
         * @param itemInfoSet the item info set.
         * @param isTakeItems whether the configuration is taking or depositing items.
         */
        public AdvancedItemsConfigurationControl(@Nonnull OrderedItemInfoSet itemInfoSet, boolean isTakeItems)
        {
            this(itemInfoSet, isTakeItems, true);
        }

        public AdvancedItemsConfigurationControl(@Nonnull OrderedItemInfoSet itemInfoSet, boolean isTakeItems, boolean allowItemManagement)
        {
            super(itemInfoSet, isTakeItems);
            this.allowItemManagement = allowItemManagement;

            setWidthPercentage(1.0);
            setFitHeightToContent(true);
            setClipContentsToBounds(false);

            itemsPanel = new StackPanel();
            itemsPanel.setParent(this);
            itemsPanel.setWidthPercentage(1.0);
            itemsPanel.setFitHeightToContent(true);
            itemsPanel.setSpacing(4.0);
            itemsPanel.setClipContentsToBounds(false);
            itemsPanel.eventBus.subscribe((BaseControl c, ReorderEvent e) ->
            {
                ItemInfoControl itemInfoControl = (ItemInfoControl) e.draggedControl;
                ItemInfoControl closestItemInfoControl = (ItemInfoControl) e.closestControl;

                if (e.insertBefore)
                {
                    itemInfoSet.moveBefore(itemInfoControl.itemInfo, closestItemInfoControl.itemInfo);
                }
                else
                {
                    itemInfoSet.moveAfter(itemInfoControl.itemInfo, closestItemInfoControl.itemInfo);
                }
            });

            if (allowItemManagement)
            {
                addItemContainer = new Control();
                addItemContainer.setParent(itemsPanel);
                addItemContainer.setWidthPercentage(1.0);
                addItemContainer.setFitHeightToContent(true);
                addItemContainer.setReorderable(false);
                TexturedControl addItemButton = new TexturedControl(Textures.Common.PLUS_ICON)
                {
                    @Override
                    protected void onRender(@Nonnull PoseStack matrixStack, @Nonnull ScissorStack scissorStack, double mouseX, double mouseY, float partialTicks)
                    {
                        if (itemsPanel.getChildren().size() - 1 > getMaxItems())
                        {
                            renderTextureAsBackground(matrixStack, Textures.Common.PLUS_ICON_DISABLED);
                        }
                        else
                        {
                            super.onRender(matrixStack, scissorStack, mouseX, mouseY, partialTicks);
                        }
                    }

                    @Override
                    public void onRenderTooltip(@Nonnull PoseStack matrixStack, double mouseX, double mouseY, float partialTicks)
                    {
                        List<FormattedCharSequence> tooltip = new ArrayList<>();
                        tooltip.add(new BlocklingsComponent("config.item.add").withStyle(itemsPanel.getChildren().size() - 1 > getMaxItems() ? ChatFormatting.GRAY : ChatFormatting.WHITE).getVisualOrderText());
                        tooltip.add(new BlocklingsComponent("config.item.amount", itemsPanel.getChildren().size() - 2, getMaxItems()).withStyle(ChatFormatting.GRAY).getVisualOrderText());
                        renderTooltip(matrixStack, mouseX, mouseY, tooltip);
                    }

                    @Override
                    protected void onMouseReleased(@Nonnull MouseReleasedEvent e)
                    {
                        if (isPressed() && itemsPanel.getChildren().size() - 1 <= getMaxItems())
                        {
                            openItemSearch();

                            e.setIsHandled(true);
                        }
                    }
                };
                addItemButton.setParent(addItemContainer);
                addItemButton.setHorizontalAlignment(0.5);
                addItemButton.setMargins(0.0, 1.0, 0.0, 1.0);

                itemSearchControl = new ItemSearchControl();
                itemSearchControl.setVisibility(Visibility.COLLAPSED);
                itemSearchControl.setParent(itemsPanel);
                itemSearchControl.setReorderable(false);
                itemSearchControl.setFilter((Item item) -> itemsPanel.getChildren().stream().noneMatch(c -> c instanceof AdvancedItemInfoControl && ((AdvancedItemInfoControl)c).itemInfo.getItem() == item));
                itemSearchControl.eventBus.subscribe((BaseControl c, ItemAddedEvent e) ->
                {
                    ItemInfo itemInfo = new ItemInfo(e.item);

                    addItemInfo(itemInfo);
                    closeItemSearch();

                    itemInfoSet.add(itemInfo, false);
                });

                screenEventBus.subscribe((BaseControl c, FocusChangedEvent e) ->
                {
                    if (!c.isFocused() && itemSearchControl.isThisOrDescendant(c) && !itemSearchControl.isThisOrDescendant(getFocusedControl()))
                    {
                        closeItemSearch();
                    }
                });

                itemsPanel.addChild(addItemContainer);
            }
            else
            {
                addItemContainer = null;
                itemSearchControl = null;
            }

            for (ItemInfo itemInfo : itemInfoSet.getItemInfos())
            {
                addItemInfo(itemInfo);
            }

            if (itemSearchControl != null)
            {
                itemsPanel.insertChildLast(itemSearchControl);
            }

            itemInfoSet.eventBus.subscribe((OrderedItemInfoSet s, ItemInfoSetEvent e) ->
            {
                ItemInfoControl control = getItemInfoControl(e.newItemInfo);

                if (control instanceof AdvancedItemInfoControl)
                {
                    ((AdvancedItemInfoControl) control).syncFromItemInfo(e.newItemInfo);
                }
            });

            if (addItemContainer != null)
            {
                itemsPanel.insertChildLast(addItemContainer);
            }
        }

        @Nullable
        @Override
        protected ItemInfoControl getItemInfoControl(@Nonnull ItemInfo itemInfo)
        {
            for (BaseControl child : itemsPanel.getChildren())
            {
                if (child instanceof AdvancedItemInfoControl && ((AdvancedItemInfoControl)child).itemInfo.equals(itemInfo))
                {
                    return (ItemInfoControl) child;
                }
            }

            return null;
        }

        @Override
        protected void addItemInfo(@Nonnull ItemInfo itemInfo)
        {
            if (getItemInfoControl(itemInfo) != null)
            {
                return;
            }

            if (addItemContainer != null)
            {
                itemsPanel.insertChildBefore(new AdvancedItemInfoControl(itemInfo), addItemContainer);
            }
            else
            {
                itemsPanel.insertChildLast(new AdvancedItemInfoControl(itemInfo));
            }
        }

        @Override
        protected void removeItemInfo(@Nonnull ItemInfo itemInfo)
        {
            ItemInfoControl itemInfoControl = getItemInfoControl(itemInfo);

            if (itemInfoControl == null)
            {
                return;
            }

            itemsPanel.removeChild(itemInfoControl);
        }

        @Override
        protected void moveItemInfo(@Nonnull ItemInfo movedItemInfo, @Nonnull ItemInfo closestItemInfo, boolean insertBefore)
        {
            ItemInfoControl closestItemInfoControl = getItemInfoControl(closestItemInfo);

            if (closestItemInfoControl == null)
            {
                return;
            }

            ItemInfoControl movedItemInfoControl = getItemInfoControl(movedItemInfo);

            if (movedItemInfoControl == null)
            {
                return;
            }

            if (insertBefore)
            {
                itemsPanel.insertChildBefore(movedItemInfoControl, closestItemInfoControl);
            }
            else
            {
                itemsPanel.insertChildAfter(movedItemInfoControl, closestItemInfoControl);
            }
        }

        /**
         * Opens the item search control.
         */
        private void openItemSearch()
        {
            if (itemSearchControl == null || addItemContainer == null)
            {
                return;
            }

            itemsPanel.insertChildBefore(itemSearchControl, addItemContainer);
            itemSearchControl.setVisibility(Visibility.VISIBLE);
            itemSearchControl.setFocused(true);
        }

        /**
         * Closes the item search control.
         */
        private void closeItemSearch()
        {
            if (itemSearchControl == null)
            {
                return;
            }

            itemSearchControl.setVisibility(Visibility.COLLAPSED);
        }

        /**
         * An advanced item info config control.
         */
        private class AdvancedItemInfoControl extends ItemInfoControl
        {
            @Nonnull
            private final NullableIntFieldControl startInventoryField;

            @Nonnull
            private final NullableIntFieldControl startContainerField;

            @Nonnull
            private final NullableIntFieldControl stopInventoryField;

            @Nonnull
            private final NullableIntFieldControl stopContainerField;

            /**
             * @param itemInfo the item info to display.
             */
            public AdvancedItemInfoControl(@Nonnull ItemInfo itemInfo)
            {
                super(itemInfo);

                setWidthPercentage(1.0);
                setFitHeightToContent(true);
                setDraggableY(true);

                GridPanel grid = new GridPanel();
                addChild(grid);
                grid.setWidthPercentage(1.0);
                grid.setFitHeightToContent(true);
                grid.addRowDefinition(GridDefinition.AUTO, 1.0);
                grid.addRowDefinition(GridDefinition.AUTO, 1.0);
                grid.addColumnDefinition(GridDefinition.AUTO, 1.0);

                GridPanel mainGrid = new GridPanel();
                grid.addChild(mainGrid, 0, 0);
                mainGrid.setWidthPercentage(1.0);
                mainGrid.setFitHeightToContent(true);
                mainGrid.addRowDefinition(GridDefinition.RATIO, 1.0);
                mainGrid.addColumnDefinition(GridDefinition.AUTO, 1.0);
                mainGrid.addColumnDefinition(GridDefinition.RATIO, 1.0);

                ItemControl itemIcon = new ItemControl();

                TexturedControl iconBackground = new TexturedControl(Textures.Tasks.TASK_ICON_BACKGROUND_RAISED);
                mainGrid.addChild(iconBackground, 0, 0);
                iconBackground.setChildrenInteractive(false);
                iconBackground.setInteractive(false);

                iconBackground.addChild(itemIcon);
                itemIcon.setWidthPercentage(1.0);
                itemIcon.setHeightPercentage(1.0);
                itemIcon.setItemScale(0.8f);
                itemIcon.setItem(itemInfo.getItem());

                GridPanel dropdownGrid = new GridPanel()
                {
                    @Override
                    protected void onRender(@Nonnull PoseStack matrixStack, @Nonnull ScissorStack scissorStack, double mouseX, double mouseY, float partialTicks)
                    {
                        Texture texture = Textures.Common.BAR_FLAT.dy(1).dHeight(-2).width((int) getWidth());
                        Texture endTexture = Textures.Common.BAR_FLAT.dy(1).dHeight(-2).width(2).x(Textures.Common.BAR_FLAT.width - 2);

                        for (int i = 0; i < getHeight(); i += texture.height)
                        {
                            renderTextureAsBackground(matrixStack, texture, 0, i);
                            renderTextureAsBackground(matrixStack, endTexture, getWidth() - 2, i);
                        }

                        renderTextureAsBackground(matrixStack, texture.dy(18).height(1), 0, getHeight() - 1);
                        renderRectangleAsBackground(matrixStack, 0x33000000, 1.0, 0.0, (int) (getWidth() - 2), (int) (getHeight() - 1));
                    }
                };

                TexturedControl upArrow = new TexturedControl(Textures.Common.ComboBox.UP_ARROW);
                TexturedControl downArrow = new TexturedControl(Textures.Common.ComboBox.DOWN_ARROW);
                TextBlockControl name = new TextBlockControl();

                TexturedControl nameBackground = new TexturedControl(Textures.Common.BAR_RAISED)
                {
                    @Override
                    protected void onRender(@Nonnull PoseStack matrixStack, @Nonnull ScissorStack scissorStack, double mouseX, double mouseY, float partialTicks)
                    {
                        if (isHovered() && getDraggedControl() == null)
                        {
                            RenderSystem.setShaderColor(0.7f, 0.9f, 1.0f, 1.0f);
                        }

                        Texture texture = getBackgroundTexture();

                        renderTextureAsBackground(matrixStack, texture.dx(1).width((int) (getWidth() - 2)));
                        renderTextureAsBackground(matrixStack, texture.x(texture.width - 2).width(2), getWidth() - 2, 0);

                        // [final-release-1.20.1] motivo: restaura estado global de color para evitar iconos oscuros en controles siguientes.
                        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
                    }

                    @Override
                    public void onRenderTooltip(@Nonnull PoseStack matrixStack, double mouseX, double mouseY, float partialTicks)
                    {
                        renderTooltip(matrixStack, mouseX, mouseY, name.getText());
                    }

                    @Override
                    public void forwardTryDrag(@Nonnull TryDragEvent e)
                    {
                        super.forwardTryDrag(e);
                    }

                    @Override
                    protected void onMouseReleased(@Nonnull MouseReleasedEvent e)
                    {
                        if (isPressed())
                        {
                            dropdownGrid.setVisibility(dropdownGrid.getVisibility() == Visibility.VISIBLE ? Visibility.COLLAPSED : Visibility.VISIBLE);
                            upArrow.setVisibility(dropdownGrid.getVisibility());
                            downArrow.setVisibility(dropdownGrid.getVisibility() == Visibility.VISIBLE ? Visibility.COLLAPSED : Visibility.VISIBLE);
                        }
                    }
                };
                mainGrid.addChild(nameBackground, 0, 1);
                nameBackground.setWidthPercentage(1.0);

                GridPanel nameGrid = new GridPanel();
                mainGrid.addChild(nameGrid, 0, 1);
                nameGrid.setWidthPercentage(1.0);
                nameGrid.setFitHeightToContent(true);
                nameGrid.setVerticalAlignment(0.5);
                nameGrid.addRowDefinition(GridDefinition.AUTO, 1.0);
                nameGrid.addColumnDefinition(GridDefinition.RATIO, 1.0);
                nameGrid.addColumnDefinition(GridDefinition.AUTO, 1.0);
                nameGrid.setInteractive(false);

                nameGrid.addChild(name, 0, 0);
                name.setText(new ItemStack(itemInfo.getItem()).getHoverName().getString());
                name.setWidthPercentage(1.0);
                name.setMarginLeft(4.0);

                nameGrid.addChild(upArrow, 0, 1);
                upArrow.setVerticalAlignment(0.5);
                upArrow.setMargins(4.0, 0.0, 5.0, 0.0);
                upArrow.setVisibility(Visibility.COLLAPSED);

                nameGrid.addChild(downArrow, 0, 1);
                downArrow.setVerticalAlignment(0.5);
                downArrow.setMargins(4.0, 0.0, 5.0, 0.0);

                grid.addChild(dropdownGrid, 1, 0);
                dropdownGrid.setWidthPercentage(1.0);
                dropdownGrid.setFitHeightToContent(true);
                dropdownGrid.addRowDefinition(GridDefinition.AUTO, 1.0);
                dropdownGrid.addRowDefinition(GridDefinition.AUTO, 1.0);
                dropdownGrid.addRowDefinition(GridDefinition.AUTO, 1.0);
                dropdownGrid.addRowDefinition(GridDefinition.AUTO, 1.0);
                dropdownGrid.addColumnDefinition(GridDefinition.AUTO, 1.0);
                dropdownGrid.setDebugName("Dropdown Grid");
                dropdownGrid.setShouldPropagateDrag(false);
                dropdownGrid.setPaddingBottom(4.0);
                dropdownGrid.setVisibility(Visibility.COLLAPSED);

                int maxSymbolWidth = GuiUtil.get().getTextWidth(">=") + 8;

                TextBlockControl startText = new TextBlockControl();
                dropdownGrid.addChild(startText, 0, 0);
                startText.setWidthPercentage(1.0);
                startText.setText(new BlocklingsComponent("config.item.start_at"));
                startText.setMarginLeft(4.0);
                startText.setMarginRight(4.0);
                startText.setMarginTop(4.0);
                startText.setMarginBottom(3.0);

                GridPanel startGrid = new GridPanel();
                dropdownGrid.addChild(startGrid, 1, 0);
                startGrid.setWidthPercentage(1.0);
                startGrid.setFitHeightToContent(true);
                startGrid.addRowDefinition(GridDefinition.AUTO, 1.0);
                startGrid.addColumnDefinition(GridDefinition.FIXED, maxSymbolWidth);
                startGrid.addColumnDefinition(GridDefinition.RATIO, 1.0);
                startGrid.addColumnDefinition(GridDefinition.FIXED, maxSymbolWidth);
                startGrid.addColumnDefinition(GridDefinition.RATIO, 1.0);

                TextBlockControl startInventoryText = new TextBlockControl();
                startGrid.addChild(startInventoryText, 0, 0);
                startInventoryText.setFitWidthToContent(true);
                startInventoryText.setText(isTakeItems ? "<" : ">");
                startInventoryText.setMarginLeft(4.0);
                startInventoryText.setMarginRight(4.0);
                startInventoryText.setHorizontalAlignment(0.5);
                startInventoryText.setVerticalAlignment(0.5);

                startInventoryField = new NullableIntFieldControl()
                {
                    @Override
                    public void onRenderTooltip(@Nonnull PoseStack matrixStack, double mouseX, double mouseY, float partialTicks)
                    {
                        List<FormattedCharSequence> tooltip = new ArrayList<>();
                        tooltip.add(new BlocklingsComponent("config.item.inventory_start_amount.name").getVisualOrderText());
                        tooltip.addAll(GuiUtil.get().split(new BlocklingsComponent("config.item.inventory_start_amount.desc").withStyle(ChatFormatting.GRAY), 200));

                        renderTooltip(matrixStack, mouseX, mouseY, tooltip);
                    }
                };
                startGrid.addChild(startInventoryField, 0, 1);
                startInventoryField.setWidthPercentage(1.0);
                startInventoryField.setHeight(16);
                startInventoryField.setMarginRight(4.0);
                startInventoryField.setHorizontalContentAlignment(0.5);
                startInventoryField.setValue(itemInfo.getStartInventoryAmount());
                startInventoryField.setMinVal(0);
                startInventoryField.setMaxVal(99999);
                startInventoryField.eventBus.subscribe((BaseControl c, ValueChangedEvent<Integer> e) ->
                {
                    updateItemInfo(info -> info.setStartInventoryAmount(e.newValue));
                });

                TextBlockControl startContainerText = new TextBlockControl();
                startGrid.addChild(startContainerText, 0, 2);
                startContainerText.setFitWidthToContent(true);
                startContainerText.setText(isTakeItems ? ">" : "<");
                startContainerText.setMarginLeft(4.0);
                startContainerText.setMarginRight(4.0);
                startContainerText.setHorizontalAlignment(0.5);
                startContainerText.setVerticalAlignment(0.5);

                startContainerField = new NullableIntFieldControl()
                {
                    @Override
                    public void onRenderTooltip(@Nonnull PoseStack matrixStack, double mouseX, double mouseY, float partialTicks)
                    {
                        List<FormattedCharSequence> tooltip = new ArrayList<>();
                        tooltip.add(new BlocklingsComponent("config.item.container_start_amount.name").getVisualOrderText());
                        tooltip.addAll(GuiUtil.get().split(new BlocklingsComponent("config.item.container_start_amount.desc").withStyle(ChatFormatting.GRAY), 200));

                        renderTooltip(matrixStack, mouseX, mouseY, tooltip);
                    }
                };
                startGrid.addChild(startContainerField, 0, 3);
                startContainerField.setWidthPercentage(1.0);
                startContainerField.setHeight(16);
                startContainerField.setMarginRight(4.0);
                startContainerField.setHorizontalContentAlignment(0.5);
                startContainerField.setValue(itemInfo.getStartContainerAmount());
                startContainerField.setMinVal(0);
                startContainerField.setMaxVal(99999);
                startContainerField.eventBus.subscribe((BaseControl c, ValueChangedEvent<Integer> e) ->
                {
                    updateItemInfo(info -> info.setStartContainerAmount(e.newValue));
                });

                TextBlockControl stopText = new TextBlockControl();
                dropdownGrid.addChild(stopText, 2, 0);
                stopText.setWidthPercentage(1.0);
                stopText.setText(new BlocklingsComponent("config.item.stop_at"));
                stopText.setMarginLeft(4.0);
                stopText.setMarginRight(4.0);
                stopText.setMarginTop(6.0);
                stopText.setMarginBottom(3.0);

                GridPanel stopGrid = new GridPanel();
                dropdownGrid.addChild(stopGrid, 3, 0);
                stopGrid.setWidthPercentage(1.0);
                stopGrid.setFitHeightToContent(true);
                stopGrid.addRowDefinition(GridDefinition.AUTO, 1.0);
                stopGrid.addColumnDefinition(GridDefinition.FIXED, maxSymbolWidth);
                stopGrid.addColumnDefinition(GridDefinition.RATIO, 1.0);
                stopGrid.addColumnDefinition(GridDefinition.FIXED, maxSymbolWidth);
                stopGrid.addColumnDefinition(GridDefinition.RATIO, 1.0);

                TextBlockControl stopInventoryText = new TextBlockControl();
                stopGrid.addChild(stopInventoryText, 0, 0);
                stopInventoryText.setFitWidthToContent(true);
                stopInventoryText.setText(isTakeItems ? ">=" : "<=");
                stopInventoryText.setMarginLeft(4.0);
                stopInventoryText.setMarginRight(4.0);
                stopInventoryText.setHorizontalAlignment(0.5);
                stopInventoryText.setVerticalAlignment(0.5);

                stopInventoryField = new NullableIntFieldControl()
                {
                    @Override
                    public void onRenderTooltip(@Nonnull PoseStack matrixStack, double mouseX, double mouseY, float partialTicks)
                    {
                        List<FormattedCharSequence> tooltip = new ArrayList<>();
                        tooltip.add(new BlocklingsComponent("config.item.inventory_stop_amount.name").getVisualOrderText());
                        tooltip.addAll(GuiUtil.get().split(new BlocklingsComponent("config.item.inventory_stop_amount.desc").withStyle(ChatFormatting.GRAY), 200));

                        renderTooltip(matrixStack, mouseX, mouseY, tooltip);
                    }
                };
                stopGrid.addChild(stopInventoryField, 0, 1);
                stopInventoryField.setWidthPercentage(1.0);
                stopInventoryField.setHeight(16);
                stopInventoryField.setMarginRight(4.0);
                stopInventoryField.setHorizontalContentAlignment(0.5);
                stopInventoryField.setValue(itemInfo.getStopInventoryAmount());
                stopInventoryField.setMinVal(0);
                stopInventoryField.setMaxVal(99999);
                stopInventoryField.eventBus.subscribe((BaseControl c, ValueChangedEvent<Integer> e) ->
                {
                    updateItemInfo(info -> info.setStopInventoryAmount(e.newValue));
                });

                TextBlockControl stopContainerText = new TextBlockControl();
                stopGrid.addChild(stopContainerText, 0, 2);
                stopContainerText.setFitWidthToContent(true);
                stopContainerText.setText(isTakeItems ? "<=" : ">=");
                stopContainerText.setMarginLeft(4.0);
                stopContainerText.setMarginRight(4.0);
                stopContainerText.setHorizontalAlignment(0.5);
                stopContainerText.setVerticalAlignment(0.5);

                stopContainerField = new NullableIntFieldControl()
                {
                    @Override
                    public void onRenderTooltip(@Nonnull PoseStack matrixStack, double mouseX, double mouseY, float partialTicks)
                    {
                        List<FormattedCharSequence> tooltip = new ArrayList<>();
                        tooltip.add(new BlocklingsComponent("config.item.container_stop_amount.name").getVisualOrderText());
                        tooltip.addAll(GuiUtil.get().split(new BlocklingsComponent("config.item.container_stop_amount.desc").withStyle(ChatFormatting.GRAY), 200));

                        renderTooltip(matrixStack, mouseX, mouseY, tooltip);
                    }
                };
                stopGrid.addChild(stopContainerField, 0, 3);
                stopContainerField.setWidthPercentage(1.0);
                stopContainerField.setHeight(16);
                stopContainerField.setMarginRight(4.0);
                stopContainerField.setHorizontalContentAlignment(0.5);
                stopContainerField.setValue(itemInfo.getStopContainerAmount());
                stopContainerField.setMinVal(0);
                stopContainerField.setMaxVal(99999);
                stopContainerField.eventBus.subscribe((BaseControl c, ValueChangedEvent<Integer> e) ->
                {
                    updateItemInfo(info -> info.setStopContainerAmount(e.newValue));
                });
            }

            private void updateItemInfo(@Nonnull Consumer<ItemInfo> updater)
            {
                List<ItemInfo> currentItemInfos = itemInfoSet.getItemInfos();

                for (int i = 0; i < currentItemInfos.size(); i++)
                {
                    ItemInfo current = currentItemInfos.get(i);

                    if (current.getItem() != itemInfo.getItem())
                    {
                        continue;
                    }

                    Integer startInventoryAmount = startInventoryField.getValue();
                    Integer stopInventoryAmount = stopInventoryField.getValue();
                    Integer startContainerAmount = startContainerField.getValue();
                    Integer stopContainerAmount = stopContainerField.getValue();

                    ItemInfo updated = new ItemInfo(current.getItem(), startInventoryAmount, stopInventoryAmount,
                            startContainerAmount, stopContainerAmount);
                    updater.accept(updated);
                    itemInfoSet.set(i, updated);
                    return;
                }
            }

            private void syncFromItemInfo(@Nonnull ItemInfo updatedItemInfo)
            {
                startInventoryField.setValue(updatedItemInfo.getStartInventoryAmount());
                startContainerField.setValue(updatedItemInfo.getStartContainerAmount());
                stopInventoryField.setValue(updatedItemInfo.getStopInventoryAmount());
                stopContainerField.setValue(updatedItemInfo.getStopContainerAmount());
            }
        }
    }
}
