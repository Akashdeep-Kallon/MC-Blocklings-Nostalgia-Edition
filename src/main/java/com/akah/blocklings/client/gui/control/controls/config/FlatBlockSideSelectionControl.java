package com.akah.blocklings.client.gui.control.controls.config;

import com.mojang.blaze3d.vertex.PoseStack;
import com.akah.blocklings.client.gui.control.Control;
import com.akah.blocklings.client.gui.control.controls.TexturedControl;
import com.akah.blocklings.client.gui.control.controls.panels.GridPanel;
import com.akah.blocklings.client.gui.control.event.events.input.MouseReleasedEvent;
import com.akah.blocklings.client.gui.properties.GridDefinition;
import com.akah.blocklings.client.gui.texture.Textures;
import com.akah.blocklings.client.gui.util.GuiUtil;
import com.akah.blocklings.entity.blockling.goal.config.ContainerInfo;
import com.akah.blocklings.util.BlocklingsComponent;
import com.akah.blocklings.util.event.IEvent;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * A flat control used to select the side of a block.
 */
@OnlyIn(Dist.CLIENT)
public class FlatBlockSideSelectionControl extends Control
{
    /**
     * The selected directions in priority order.
     */
    @Nonnull
    private List<Direction> selectedDirections = new ArrayList<>();

    /**
     * Face cells by direction.
     */
    @Nonnull
    private final Map<Direction, FaceCellControl> faceCells = new EnumMap<>(Direction.class);

    /**
     * The block for display context.
     */
    @Nonnull
    private Block block;

    /**
     */
    public FlatBlockSideSelectionControl()
    {
        super();

        selectedDirections.addAll(ContainerInfo.DEFAULT_SIDES);

        block = net.minecraft.world.level.block.Blocks.AIR;

        GridPanel gridPanel = new GridPanel();
        addChild(gridPanel);
        // Keep event ownership in this control so hover/click/tooltip behave like the legacy 3D selector.
        gridPanel.setInteractive(false);
        gridPanel.setWidthPercentage(1.0);
        gridPanel.setHeightPercentage(1.0);

        for (int i = 0; i < 3; i++)
        {
            gridPanel.addRowDefinition(GridDefinition.RATIO, 1.0);
        }

        for (int i = 0; i < 4; i++)
        {
            gridPanel.addColumnDefinition(GridDefinition.RATIO, 1.0);
        }

        addPaddingCell(gridPanel, 0, 0);
        addFaceCell(gridPanel, 0, 1, Direction.UP);
        addPaddingCell(gridPanel, 0, 2);
        addPaddingCell(gridPanel, 0, 3);

        addFaceCell(gridPanel, 1, 0, Direction.WEST);
        addFaceCell(gridPanel, 1, 1, Direction.NORTH);
        addFaceCell(gridPanel, 1, 2, Direction.EAST);
        addFaceCell(gridPanel, 1, 3, Direction.SOUTH);

        addPaddingCell(gridPanel, 2, 0);
        addFaceCell(gridPanel, 2, 1, Direction.DOWN);
        addPaddingCell(gridPanel, 2, 2);
        addPaddingCell(gridPanel, 2, 3);
    }

    private void addPaddingCell(@Nonnull GridPanel gridPanel, int row, int col)
    {
        Control paddingControl = new Control();
        gridPanel.addChild(paddingControl, row, col);
        paddingControl.setWidthPercentage(1.0);
        paddingControl.setHeightPercentage(1.0);
        paddingControl.setInteractive(false);
    }

    private void addFaceCell(@Nonnull GridPanel gridPanel, int row, int col, @Nonnull Direction direction)
    {
        FaceCellControl faceCellControl = new FaceCellControl(direction);
        faceCells.put(direction, faceCellControl);
        gridPanel.addChild(faceCellControl, row, col);
        faceCellControl.setWidthPercentage(1.0);
        faceCellControl.setHeightPercentage(1.0);
        // All click handling is centralized in the parent control to match the 3D selector flow.
        faceCellControl.setInteractive(false);
    }

    @Nullable
    public Direction getDirectionMouseIsOver()
    {
        double mouseX = GuiUtil.get().getPixelMouseX();
        double mouseY = GuiUtil.get().getPixelMouseY();

        for (Map.Entry<Direction, FaceCellControl> entry : faceCells.entrySet())
        {
            if (entry.getValue().contains(mouseX, mouseY))
            {
                return entry.getKey();
            }
        }

        return null;
    }

    @Override
    protected void onMouseReleased(@Nonnull MouseReleasedEvent e)
    {
        if (e.button == GLFW.GLFW_MOUSE_BUTTON_1)
        {
            Direction mouseOverDirection = getDirectionMouseIsOver();

            if (mouseOverDirection != null)
            {
                List<Direction> oldSelectedDirections = new ArrayList<>(selectedDirections);
                List<Direction> newSelectedDirections = new ArrayList<>(selectedDirections);

                if (newSelectedDirections.contains(mouseOverDirection))
                {
                    newSelectedDirections.remove(mouseOverDirection);
                }
                else
                {
                    newSelectedDirections.add(mouseOverDirection);
                }

                setSelectedDirections(newSelectedDirections);
                eventBus.post(this, new DirectionListChangedEvent(oldSelectedDirections, new ArrayList<>(selectedDirections)));
                e.setIsHandled(true);
                return;
            }
        }

        super.onMouseReleased(e);
    }

    @Override
    public void onRenderTooltip(@Nonnull PoseStack matrixStack, double mouseX, double mouseY, float partialTicks)
    {
        Direction mouseOverDirection = getDirectionMouseIsOver();

        if (mouseOverDirection != null)
        {
            switch (mouseOverDirection)
            {
                case NORTH:
                    renderTooltip(matrixStack, mouseX, mouseY, new BlocklingsComponent("direction.front"));
                    break;
                case SOUTH:
                    renderTooltip(matrixStack, mouseX, mouseY, new BlocklingsComponent("direction.back"));
                    break;
                case WEST:
                    renderTooltip(matrixStack, mouseX, mouseY, new BlocklingsComponent("direction.left"));
                    break;
                case EAST:
                    renderTooltip(matrixStack, mouseX, mouseY, new BlocklingsComponent("direction.right"));
                    break;
                case UP:
                    renderTooltip(matrixStack, mouseX, mouseY, new BlocklingsComponent("direction.top"));
                    break;
                case DOWN:
                    renderTooltip(matrixStack, mouseX, mouseY, new BlocklingsComponent("direction.bottom"));
                    break;
            }
        }
    }

    @Nonnull
    public List<Direction> getSelectedDirections()
    {
        return selectedDirections;
    }

    public void setSelectedDirections(@Nonnull List<Direction> selectedDirections)
    {
        this.selectedDirections = new ArrayList<>();

        if (selectedDirections.isEmpty())
        {
            this.selectedDirections.addAll(ContainerInfo.DEFAULT_SIDES);
            return;
        }

        for (Direction direction : selectedDirections)
        {
            if (direction != null && !this.selectedDirections.contains(direction))
            {
                this.selectedDirections.add(direction);
            }
        }

        if (this.selectedDirections.isEmpty())
        {
            this.selectedDirections.addAll(ContainerInfo.DEFAULT_SIDES);
        }
    }

    public void setBlock(@Nonnull Block block)
    {
        this.block = block;
    }

    private class FaceCellControl extends TexturedControl
    {
        @Nonnull
        private final Direction direction;

        private FaceCellControl(@Nonnull Direction direction)
        {
            super(Textures.Tasks.TASK_ICON_BACKGROUND_RAISED);
            this.direction = direction;
            setFitWidthToContent(false);
            setFitHeightToContent(false);
        }

        @Override
        protected void onRender(@Nonnull PoseStack matrixStack, @Nonnull com.akah.blocklings.client.gui.util.ScissorStack scissorStack, double mouseX, double mouseY, float partialTicks)
        {
            super.onRender(matrixStack, scissorStack, mouseX, mouseY, partialTicks);

            if (direction == getDirectionMouseIsOver())
            {
                renderRectangleAsBackground(matrixStack, 0x55ffffff);
            }

            int index = selectedDirections.indexOf(direction);

            if (index >= 0)
            {
                String numberText = Integer.toString(index + 1);
                float textWidth = GuiUtil.get().getTextWidth(numberText);
                float textHeight = GuiUtil.get().getLineHeight();
                float maxScaleX = (float) Math.max(1.0, (getPixelWidth() - 4.0) / Math.max(1.0, textWidth));
                float maxScaleY = (float) Math.max(1.0, (getPixelHeight() - 4.0) / Math.max(1.0, textHeight));
                float numberScale = Math.min(1.6f, Math.min(maxScaleX, maxScaleY));

                float centerX = (float) (getPixelX() + getPixelWidth() / 2.0);
                float centerY = (float) (getPixelY() + getPixelHeight() / 2.0);

                matrixStack.pushPose();
                matrixStack.translate(centerX, centerY, 0.0);
                matrixStack.scale(numberScale, numberScale, 1.0f);
                renderShadowedText(
                        matrixStack,
                        Component.literal(numberText).getVisualOrderText(),
                        (int) Math.round(-textWidth / 2.0),
                        (int) Math.round(-textHeight / 2.0),
                        0xffffffff
                );
                matrixStack.popPose();
            }
        }
    }

    /**
     * Fired when the user toggles a face on or off.
     */
    public static class DirectionListChangedEvent implements IEvent
    {
        @Nonnull public final List<Direction> oldDirections;
        @Nonnull public final List<Direction> newDirections;

        public DirectionListChangedEvent(@Nonnull List<Direction> oldDirections,
                                         @Nonnull List<Direction> newDirections)
        {
            this.oldDirections = oldDirections;
            this.newDirections = newDirections;
        }
    }
}
