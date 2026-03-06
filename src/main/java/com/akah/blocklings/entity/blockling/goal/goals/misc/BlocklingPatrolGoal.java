package com.akah.blocklings.entity.blockling.goal.goals.misc;

import com.mojang.blaze3d.vertex.PoseStack;
import com.akah.blocklings.client.gui.control.BaseControl;
import com.akah.blocklings.client.gui.control.Control;
import com.akah.blocklings.client.gui.control.controls.TexturedControl;
import com.akah.blocklings.client.gui.control.controls.config.PatrolPointControl;
import com.akah.blocklings.client.gui.control.controls.panels.StackPanel;
import com.akah.blocklings.client.gui.control.controls.panels.TabbedPanel;
import com.akah.blocklings.client.gui.control.event.events.ReorderEvent;
import com.akah.blocklings.client.gui.control.event.events.input.MouseReleasedEvent;
import com.akah.blocklings.client.gui.texture.Textures;
import com.akah.blocklings.client.gui.util.GuiUtil;
import com.akah.blocklings.client.gui.util.ScissorStack;
import com.akah.blocklings.entity.blockling.BlocklingEntity;
import com.akah.blocklings.entity.blockling.goal.BlocklingTargetGoal;
import com.akah.blocklings.entity.blockling.goal.config.patrol.OrderedPatrolPointList;
import com.akah.blocklings.entity.blockling.goal.config.patrol.PatrolPoint;
import com.akah.blocklings.entity.blockling.task.BlocklingTasks;
import com.akah.blocklings.entity.blockling.task.config.PatrolTypeProperty;
import com.akah.blocklings.util.BlockUtil;
import com.akah.blocklings.util.BlocklingsComponent;
import com.akah.blocklings.util.PathUtil;
import com.akah.blocklings.util.Version;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BlocklingPatrolGoal extends BlocklingTargetGoal<PatrolPoint> implements OrderedPatrolPointList.IOrderedPatrolPointListProvider
{
    /**
     * The max number of patrol points that can be added.
     */
    public static final int MAX_PATROL_POINTS = 8;

    /**
     * The list of patrol points.
     */
    @Nonnull
    public final OrderedPatrolPointList patrolPointList;

    public final PatrolTypeProperty patrolTypeProperty;

    /**
     * The time in ticks that the blockling has waited at the current patrol point.
     */
    private int timeWaited = 0;

    /**
     * Whether the blockling has successfully waited at the current patrol point.
     */
    private boolean successfullyWaitedAtLastPatrolPoint = false;

    /**
     * If a previously marked bad target is no longer bad, we want to give it a chance to be set as the target. So after
     * a certain amount of recalcs have taken place, release all bad targets, so they can be reevaluated.
     */
    private int numberOfRecalcsSinceBadTargetsReset = 20;

    /**
     * The direction in which we want to traverse the patrol points.
     */
    private boolean traverseFowards = true;

    /**
     * Whether the patrol points should loop.
     */
    private boolean loop = false;

    /**
     * @param id        the id associated with the goal's task.
     * @param blockling the blockling.
     * @param tasks     the blockling tasks.
     */
    public BlocklingPatrolGoal(@Nonnull UUID id, @Nonnull BlocklingEntity blockling, @Nonnull BlocklingTasks tasks)
    {
        super(id, blockling, tasks);

        patrolPointList = new OrderedPatrolPointList(this);
        patrolPointList.onPatrolPointRemoved.subscribe((e) ->
        {
            if (getTarget() == e.removedPatrolPoint)
            {
                setTarget(e.nextPatrolPoint);
            }
        });

        properties.add(patrolTypeProperty = new PatrolTypeProperty(
                "76608784-b6eb-4291-9f0a-1efb989eb4fd", this,
                new BlocklingsComponent("task.property.patrol_type.name"),
                new BlocklingsComponent("task.property.patrol_type.desc")
        ));

        patrolTypeProperty.onTypeChanged.subscribe((e) ->
        {
            loop = e == PatrolTypeProperty.Type.LOOP;
        });
    }

    @Override
    public void writeToNBT(@Nonnull CompoundTag taskTag)
    {
        super.writeToNBT(taskTag);

        CompoundTag patrolPointListTag = new CompoundTag();
        patrolPointList.writeToNBT(patrolPointListTag);
        taskTag.put("patrol_point_list", patrolPointListTag);
    }

    @Override
    public void readFromNBT(@Nonnull CompoundTag taskTag, @Nonnull Version tagVersion)
    {
        super.readFromNBT(taskTag, tagVersion);

        CompoundTag patrolPointListTag = taskTag.getCompound("patrol_point_list");

        if (patrolTypeProperty != null)
        {
            patrolPointList.readFromNBT(patrolPointListTag, tagVersion);
        }
    }

    @Override
    public void encode(@Nonnull FriendlyByteBuf buf)
    {
        super.encode(buf);

        patrolPointList.encode(buf);
    }

    @Override
    public void decode(@Nonnull FriendlyByteBuf buf)
    {
        super.decode(buf);

        patrolPointList.decode(buf);
    }

    @Override
    public boolean canUse()
    {
        if (!super.canUse())
        {
            return false;
        }

        if (patrolPointList.isEmpty())
        {
            return false;
        }

        return true;
    }

    @Override
    public boolean canContinueToUse()
    {
        if (!super.canContinueToUse())
        {
            return false;
        }

        if (patrolPointList.isEmpty())
        {
            return false;
        }

        return true;
    }

    @Override
    public void start()
    {
        super.start();
    }

    @Override
    public void stop()
    {
        super.stop();
    }

    @Override
    public void recalcTarget()
    {
        if (numberOfRecalcsSinceBadTargetsReset++ > 100)
        {
            numberOfRecalcsSinceBadTargetsReset = 0;

            badTargets.clear();
        }

        if (isTargetValid())
        {
            return;
        }

        if (successfullyWaitedAtLastPatrolPoint)
        {
            successfullyWaitedAtLastPatrolPoint = false;

            if (getPrevTarget() != null)
            {
                PatrolPoint patrolPoint = getPrevTarget();
                int indexOfPatrolPoint = patrolPointList.indexOf(patrolPoint);
                int amountToIterate = patrolPointList.size();
                boolean wasFirstPatrolPoint = indexOfPatrolPoint == 0;
                boolean wasLastPatrolPoint = indexOfPatrolPoint == patrolPointList.size() - 1;
                boolean foundValidPatrolPoint = false;

                if (!loop)
                {
                    if (wasFirstPatrolPoint)
                    {
                        traverseFowards = true;
                    }
                    else if (wasLastPatrolPoint)
                    {
                        traverseFowards = false;
                    }

                    if (traverseFowards)
                    {
                        amountToIterate = patrolPointList.size() - indexOfPatrolPoint - 1;
                    }
                    else
                    {
                        amountToIterate = indexOfPatrolPoint;
                    }
                }

                // If we are not looping and don't find a valid patrol point, try the other direction.
                for (int j = 0; j < (loop ? 1 : 2) && !foundValidPatrolPoint; j++)
                {
                    if (j == 1)
                    {
                        traverseFowards = !traverseFowards;
                    }

                    // Attempt to traverse the patrol points until we find a valid one.
                    for (int i = 0; i < amountToIterate; i++)
                    {
                        if (traverseFowards)
                        {
                            patrolPoint = patrolPointList.next(patrolPoint);
                        }
                        else
                        {
                            patrolPoint = patrolPointList.prev(patrolPoint);
                        }

                        if (!isValidTarget(patrolPoint))
                        {
                            markBad(patrolPoint);

                            continue;
                        }

                        setTarget(patrolPoint);
                        recalcPathTargetPosAndPath(true);

                        if ((getPathTarget() != null && isStuck(true))
                         || (getPathTarget() == null && !isInRange(patrolPoint.asBlockPos(), getPathTargetRangeSq())))
                        {
                            markBad(patrolPoint);
                        }
                        else
                        {
                            foundValidPatrolPoint = true;

                            break;
                        }
                    }
                }
            }
        }
        // If the target is not valid, and not because we just successfully waited, then find the closest valid patrol point.
        else
        {
            PatrolPoint closest = getTarget();
            double closestDistance = Double.MAX_VALUE;

            for (PatrolPoint patrolPoint : patrolPointList)
            {
                if (!isValidTarget(patrolPoint))
                {
                    markBad(patrolPoint);

                    continue;
                }

                double distance = patrolPoint.asBlockPos().distSqr(blockling.blockPosition());

                if (distance >= closestDistance)
                {
                    continue;
                }

                setTarget(patrolPoint);
                recalcPathTargetPosAndPath(true);

                if ((getPathTarget() != null && isStuck(true))
                 || (getPathTarget() == null && !isInRange(patrolPoint.asBlockPos(), getPathTargetRangeSq())))
                {
                    markBad(patrolPoint);

                    continue;
                }

                closest = patrolPoint;
                closestDistance = distance;
            }

            setTarget(closest);
            recalcPathTargetPosAndPath(true);
        }
    }

    @Override
    protected void recalcPathTargetPosAndPath(boolean force)
    {
        if (!blockling.onGround()) // TODO: Should probably but everywhere.
        {
            return;
        }

        Path path = findPathTo(getTarget());

        if (path != null)
        {
            // If setting the path target fails, then this target is a bad target.
            if (!trySetPathTarget(getTarget().asBlockPos(), path))
            {
                markTargetBad();
            }
        }
        else
        {
            markTargetBad();
            trySetPathTarget(null, null);
        }
    }

    @Override
    protected void tickGoal()
    {
        if (getTarget() == null)
        {
            return;
        }

        // If we are in range of the target, and not moving, increment the time waited.
        if (isInRangeOfPathTargetPos() && blockling.getDeltaMovement().lengthSqr() < 0.01)
        {
            timeWaited++;
        }

        if (timeWaited > getTarget().getWaitTime())
        {
            successfullyWaitedAtLastPatrolPoint = true;

            setTarget(null);
        }
    }

    @Override
    protected void setTarget(@Nullable PatrolPoint target)
    {
        // Reset the time waited if the target changes.
        if (target == null || target != getTarget())
        {
            timeWaited = 0;
        }

        super.setTarget(target);
    }

    /**
     * Finds a path to the given patrol point.
     *
     * @param patrolPoint the patrol point.
     * @return the path to the patrol point, or null if no path could be found.
     */
    @Nullable
    private Path findPathTo(@Nullable PatrolPoint patrolPoint)
    {
        if (patrolPoint == null || !patrolPoint.isConfigured())
        {
            return null;
        }

        return PathUtil.createPathTo(blockling, patrolPoint.asBlockPos(), getPathTargetRangeSq(), true);
    }

    @Override
    protected void checkForAndHandleInvalidTargets()
    {
        if (!isTargetValid())
        {
            markTargetBad();
        }
    }

    @Override
    public void markEntireTargetBad()
    {
        for (PatrolPoint patrolPoint : patrolPointList)
        {
            markBad(patrolPoint);
        }
    }

    @Override
    public boolean isValidTarget(@Nullable PatrolPoint patrolPoint)
    {
        return patrolPoint != null && patrolPoint.isConfigured() && !badTargets.contains(patrolPoint) && patrolPointList.contains(patrolPoint);
    }

    @Override
    protected boolean isValidPathTargetPos(@Nonnull BlockPos blockPos)
    {
        return getTarget() != null && getTarget().asBlockPos().equals(blockPos) && !BlockUtil.areAllAdjacentBlocksSolid(world, blockPos);
    }

    @Override
    public float getPathTargetRangeSq()
    {
        return 2.0f;
    }

    @Nonnull
    @Override
    public BlocklingEntity getBlockling()
    {
        return blockling;
    }

    @Nonnull
    @Override
    public OrderedPatrolPointList getOrderedPatrolPointList()
    {
        return patrolPointList;
    }

    /**
     * @return whether the patrol points list is full.
     */
    public boolean isPatrolPointListFull()
    {
        return getOrderedPatrolPointList().size() >= MAX_PATROL_POINTS;
    }

    @Nonnull
    @Override
    @OnlyIn(Dist.CLIENT)
    public void addConfigTabControls(@Nonnull Object tabbedPanelObj)
    {
        TabbedPanel tabbedPanel = (TabbedPanel) tabbedPanelObj;

        super.addConfigTabControls(tabbedPanelObj);

        BaseControl pointsTabContainer = tabbedPanel.addTab(new BlocklingsComponent("config.patrol.points"));
        pointsTabContainer.setCanScrollVertically(true);

        StackPanel stackPanel = new StackPanel();
        stackPanel.setParent(pointsTabContainer);
        stackPanel.setWidthPercentage(1.0);
        stackPanel.setFitHeightToContent(true);
        stackPanel.setMargins(5.0, 9.0, 5.0, 5.0);
        stackPanel.setSpacing(4.0);
        stackPanel.setClipContentsToBounds(false);
        stackPanel.setScrollFromDragControl(pointsTabContainer);
        stackPanel.eventBus.subscribe((BaseControl c, ReorderEvent e) ->
        {
            PatrolPoint draggedPatrolPoint = ((PatrolPointControl) e.draggedControl).patrolPoint;
            PatrolPoint closestPatrolPoint = ((PatrolPointControl) e.closestControl).patrolPoint;
            getOrderedPatrolPointList().move(getOrderedPatrolPointList().indexOf(draggedPatrolPoint), getOrderedPatrolPointList().indexOf(closestPatrolPoint), e.insertBefore);
        });

        // Add the existing patrol points.
        for (PatrolPoint patrolPoint : patrolPointList)
        {
            stackPanel.addChild(new PatrolPointControl(patrolPoint));
        }

        Control addPointControl = new Control();
        addPointControl.setParent(stackPanel);
        addPointControl.setWidthPercentage(1.0);
        addPointControl.setFitHeightToContent(true);
        addPointControl.setReorderable(false);

        TexturedControl addContainerButton = new TexturedControl(Textures.Common.PLUS_ICON)
        {
            @Override
            protected void onRender(@Nonnull PoseStack matrixStack, @Nonnull ScissorStack scissorStack, double mouseX, double mouseY, float partialTicks)
            {
                if (isPatrolPointListFull())
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
                tooltip.add(new BlocklingsComponent("config.patrol.add").withStyle(isPatrolPointListFull() ? ChatFormatting.GRAY : ChatFormatting.WHITE).getVisualOrderText());
                tooltip.add(new BlocklingsComponent("config.patrol.amount", getOrderedPatrolPointList().size(), MAX_PATROL_POINTS).withStyle(ChatFormatting.GRAY).getVisualOrderText());
                tooltip.add(Component.empty().getVisualOrderText());
                tooltip.addAll(GuiUtil.get().split(new BlocklingsComponent("config.patrol.add.help", Component.literal(Minecraft.getInstance().options.keyShift.getTranslatedKeyMessage().getString()).withStyle(ChatFormatting.ITALIC)).withStyle(ChatFormatting.GRAY), 200));
                renderTooltip(matrixStack, mouseX, mouseY, tooltip);
            }

            @Override
            protected void onMouseReleased(@Nonnull MouseReleasedEvent e)
            {
                if (isPressed() && !isPatrolPointListFull())
                {
                    boolean shouldAddManually = GuiUtil.get().isControlKeyDown();

                    PatrolPoint patrolPoint = new PatrolPoint();
                    patrolPointList.add(patrolPoint, !shouldAddManually, true);

                    PatrolPointControl patrolPointControl = new PatrolPointControl(patrolPoint);
                    stackPanel.insertChildBefore(patrolPointControl, addPointControl);

                    if (!shouldAddManually)
                    {
                        PatrolPointControl.currentlyConfiguredPatrolPointControl = patrolPointControl;
                        PatrolPointControl.screenToGoBackTo = Minecraft.getInstance().screen;
                        getScreen().setShouldReallyClose(false);
                        PatrolPointControl.screenToGoBackTo.onClose();
                        getScreen().setShouldReallyClose(true);
                    }

                    e.setIsHandled(true);
                }
            }
        };
        addContainerButton.setParent(addPointControl);
        addContainerButton.setHorizontalAlignment(0.5);
        addContainerButton.setMargins(0.0, 1.0, 0.0, 1.0);
    }
}
