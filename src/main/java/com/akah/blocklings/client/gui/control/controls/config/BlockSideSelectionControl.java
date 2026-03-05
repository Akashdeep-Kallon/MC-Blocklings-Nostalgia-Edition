package com.akah.blocklings.client.gui.control.controls.config;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import com.akah.blocklings.client.gui.control.controls.BlockControl;
import com.akah.blocklings.client.gui.control.event.events.input.MouseReleasedEvent;
import com.akah.blocklings.client.gui.util.GuiUtil;
import com.akah.blocklings.client.gui.util.ScissorStack;
import com.akah.blocklings.util.BlocklingsComponent;
import com.akah.blocklings.util.event.IEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * A control used to select the side of a block.
 */
@OnlyIn(Dist.CLIENT)
public class BlockSideSelectionControl extends BlockControl
{
    /**
     * The selected directions in priority order.
     */
    @Nonnull
    private List<Direction> selectedDirections = new ArrayList<>();

    /**
     */
    public BlockSideSelectionControl()
    {
        super();
    }

    @Override
    protected void onRender(@Nonnull PoseStack matrixStack, @Nonnull ScissorStack scissorStack, double mouseX, double mouseY, float partialTicks)
    {
        super.onRender(matrixStack, scissorStack, mouseX, mouseY, partialTicks);

        // Block center in GUI space (matches the position used in BlockControl.onRender).
        float centerX = (float) (x + (getWidth()  / 2.0) * getScaleX());
        float centerY = (float) (y + (getHeight() / 2.0) * getScaleY());

        // Draw priority numbers on the relevant faces.
        for (int i = 0; i < selectedDirections.size(); i++)
        {
            Direction direction = selectedDirections.get(i);
            String s = Integer.toString(i + 1);

            switch (direction)
            {
                case NORTH:
                    renderTextOnSide(s, centerX, centerY, z, scale, new Quaternionf(), 0xffffffff);
                    break;
                case EAST:
                    renderTextOnSide(s, centerX, centerY, z, scale, Axis.YP.rotationDegrees(90.0f), 0xffffffff);
                    break;
                case SOUTH:
                    renderTextOnSide(s, centerX, centerY, z, scale, Axis.YP.rotationDegrees(180.0f), 0xffffffff);
                    break;
                case WEST:
                    renderTextOnSide(s, centerX, centerY, z, scale, Axis.YP.rotationDegrees(-90.0f), 0xffffffff);
                    break;
                case UP:
                    renderTextOnSide(s, centerX, centerY, z, scale, Axis.XP.rotationDegrees(90.0f), 0xffffffff);
                    break;
                case DOWN:
                    renderTextOnSide(s, centerX, centerY, z, scale, Axis.XP.rotationDegrees(-90.0f), 0xffffffff);
                    break;
            }
        }

        // Draw hover highlight on the face under the mouse cursor.
        Direction mouseOverDirection = getDirectionMouseIsOver();

        if (mouseOverDirection != null)
        {
            switch (mouseOverDirection)
            {
                case NORTH:
                    renderRectangleOnSide(centerX, centerY, z, scale, new Quaternionf(), 0x55ffffff);
                    break;
                case SOUTH:
                    renderRectangleOnSide(centerX, centerY, z, scale, Axis.YP.rotationDegrees(180.0f), 0x55ffffff);
                    break;
                case WEST:
                    renderRectangleOnSide(centerX, centerY, z, scale, Axis.YP.rotationDegrees(-90.0f), 0x55ffffff);
                    break;
                case EAST:
                    renderRectangleOnSide(centerX, centerY, z, scale, Axis.YP.rotationDegrees(90.0f), 0x55ffffff);
                    break;
                case UP:
                    renderRectangleOnSide(centerX, centerY, z, scale, Axis.XP.rotationDegrees(90.0f), 0x55ffffff);
                    break;
                case DOWN:
                    renderRectangleOnSide(centerX, centerY, z, scale, Axis.XP.rotationDegrees(-90.0f), 0x55ffffff);
                    break;
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * Calculates the dot product of a segment and a plane.
     *
     * @param segmentPoint the start/end point of the segment.
     * @param planePoint   a point on the plane.
     * @param planeNormal  the normal of the plane.
     * @return the dot product.
     */
    private float calcSegmentPlaneDotProduct(@Nonnull Vector4f segmentPoint, @Nonnull Vector4f planePoint, @Nonnull Vector4f planeNormal)
    {
        Vector3f segmentMinusPlanePoint = new Vector3f(
            segmentPoint.x - planePoint.x,
            segmentPoint.y - planePoint.y,
            segmentPoint.z - planePoint.z
        );
        Vector3f planeNormal3f = new Vector3f(planeNormal.x, planeNormal.y, planeNormal.z);
        return segmentMinusPlanePoint.dot(planeNormal3f);
    }

    /**
     * Renders the priority number directly on the given cube face.
     *
     * <p><b>Fix:</b> The previous implementation called {@code renderShadowedText} which, inside
     * {@code FullGuiUtil}, applies an additional {@code translate(0, 0, 200)} <em>after</em> the
     * 3D block rotation has already been applied to the PoseStack.  Because the rotation matrix
     * maps the local Z-axis into screen X/Y space, a 200-unit Z offset becomes a ~120 px lateral
     * shift, making the numbers appear far off the block.  We bypass that helper and call the font
     * renderer directly so no unwanted Z offset is injected.</p>
     */
    private void renderTextOnSide(String text, float x, float y, float z, float scale,
                                  @Nonnull Quaternionf rotation, int colour)
    {
        RenderSystem.disableCull();

        PoseStack matrixStack = new PoseStack();
        // Step 1 – go to block centre in GUI space.
        matrixStack.translate(x, y, z);
        // Step 2 – apply the current block rotation (XP 30°, YP ±45°, + any mouse-drag rotation).
        matrixStack.mulPose(new Quaternionf(rotationQuat));
        // Step 3 – rotate the coordinate system to face the target face.
        matrixStack.mulPose(rotation);
        // Step 4 – scale so that one "text unit" == scale/24 pixels (matches the block face size).
        matrixStack.scale(scale / 24.0f, scale / 24.0f, 1.0f);
        // Step 5 – centre the text and push it just in front of the face surface.
        matrixStack.translate(
            -GuiUtil.get().getTextWidth(text) / 2.0,
            -GuiUtil.get().getLineHeight() / 2.0,
            scale / 2.0 + 1.0 - 0.03
        );

        RenderSystem.enableDepthTest();

        // [fix] Call drawInBatch directly to avoid the translate(0,0,200) that FullGuiUtil adds.
        // That +200 is meant to keep text on top in flat GUI rendering, but here it is applied
        // inside the rotated 3D space and converts into a large lateral screen-space displacement.
        MultiBufferSource.BufferSource bufferSource =
            MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        Minecraft.getInstance().font.drawInBatch(
            Component.literal(text).getVisualOrderText(),
            0, 0,
            colour,
            true,                        // shadow
            matrixStack.last().pose(),
            bufferSource,
            Font.DisplayMode.NORMAL,
            0,                           // background colour (none)
            LightTexture.FULL_BRIGHT
        );
        bufferSource.endBatch();

        RenderSystem.enableCull();
    }

    /**
     * Renders a translucent highlight quad directly on a cube face using the Tesselator.
     *
     * <p><b>Why not {@code renderCenteredRectangle}?</b>
     * {@code GuiControl.renderRectangle} wraps {@code GuiGraphics.fill}, which
     * (a) uses {@code RenderType.gui()} — a render type that does not enable depth
     * testing regardless of any prior {@code RenderSystem.enableDepthTest()} call —
     * so the highlight always appears on top of every other GUI element; and
     * (b) calls {@code GuiGraphics.flush()} on the <em>shared</em>
     * {@code mc.renderBuffers().bufferSource()}, flushing every pending GUI batch
     * (text, icons …) while our scissor is disabled and producing stray glyph
     * artefacts.</p>
     *
     * <p>This implementation draws a raw quad via {@code POSITION_COLOR} /
     * {@code RENDERTYPE_GUI_OVERLAY} shader with depth test enabled, matching
     * exactly the 3D coordinate system used by {@link BlockControl}.</p>
     */
    private void renderRectangleOnSide(float x, float y, float z, float scale,
                                       @Nonnull Quaternionf rotation, int colour)
    {
        // Build the face-aligned pose (same logic as renderTextOnSide).
        PoseStack matrixStack = new PoseStack();
        matrixStack.translate(x, y, z);
        matrixStack.mulPose(new Quaternionf(rotationQuat));
        matrixStack.mulPose(rotation);
        // Push the quad plane just in front of the block face.
        matrixStack.translate(0.0f, 0.0f, scale / 2.0f + 0.5f);

        Matrix4f pose = matrixStack.last().pose();

        // Unpack ARGB colour.
        float a = ((colour >> 24) & 0xFF) / 255.0f;
        float r = ((colour >> 16) & 0xFF) / 255.0f;
        float g = ((colour >>  8) & 0xFF) / 255.0f;
        float b = ((colour      ) & 0xFF) / 255.0f;

        float h = scale / 2.0f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buf = tesselator.getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buf.vertex(pose, -h, -h, 0).color(r, g, b, a).endVertex();
        buf.vertex(pose, -h,  h, 0).color(r, g, b, a).endVertex();
        buf.vertex(pose,  h,  h, 0).color(r, g, b, a).endVertex();
        buf.vertex(pose,  h, -h, 0).color(r, g, b, a).endVertex();
        BufferUploader.drawWithShader(buf.end());

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Tooltip
    // ──────────────────────────────────────────────────────────────────────────────

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

    // ──────────────────────────────────────────────────────────────────────────────
    // Input
    // ──────────────────────────────────────────────────────────────────────────────

    @Override
    protected void onMouseReleased(@Nonnull MouseReleasedEvent e)
    {
        if (isPressed() && ((dragAmount < 5.0 && e.button == GLFW.GLFW_MOUSE_BUTTON_1) || dragAmount == 0.0))
        {
            Direction mouseOverDirection = getDirectionMouseIsOver();

            if (mouseOverDirection != null)
            {
                List<Direction> oldSelectedDirections = new ArrayList<>(selectedDirections);
                // [fix] Use a mutable copy to toggle the face without corrupting the original list.
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
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Hit-testing: ray-march along Z to find the first cube face under the cursor
    // ──────────────────────────────────────────────────────────────────────────────

    @Nullable
    public Direction getDirectionMouseIsOver()
    {
        float screenMouseX = (float) (GuiUtil.get().getPixelMouseX() / getGuiScale());
        float screenMouseY = (float) (GuiUtil.get().getPixelMouseY() / getGuiScale());

        // [fix] Use the real block centre, not the control's top-left corner.
        float centerX = (float) (x + (getWidth()  / 2.0) * getScaleX());
        float centerY = (float) (y + (getHeight() / 2.0) * getScaleY());

        float scale   = (float) (Math.min(getWidth(), getHeight()) * getScaleX()) * getBlockScale();
        float width   = scale / 2.0f;
        float widthSq = width * width;
        double cubeDiagFromCenterToCorner = Math.sqrt(widthSq + widthSq + widthSq);
        int z = (int) getRenderZ();

        PoseStack pointMatrixStack  = new PoseStack();
        PoseStack normalMatrixStack = new PoseStack();

        pointMatrixStack.translate(centerX, centerY, z);
        pointMatrixStack.mulPose(new Quaternionf(rotationQuat));
        normalMatrixStack.mulPose(new Quaternionf(rotationQuat));

        Vector4f frontPlanePoint  = new Vector4f(0,           0,            scale / 2.0f,  1);
        Vector4f frontPlaneNormal = new Vector4f(0,           0,            1,             1);
        Vector4f backPlanePoint   = new Vector4f(0,           0,           -scale / 2.0f,  1);
        Vector4f backPlaneNormal  = new Vector4f(0,           0,           -1,             1);
        Vector4f topPlanePoint    = new Vector4f(0,          -scale / 2.0f, 0,             1);
        Vector4f topPlaneNormal   = new Vector4f(0,          -1,            0,             1);
        Vector4f bottomPlanePoint = new Vector4f(0,           scale / 2.0f, 0,             1);
        Vector4f bottomPlaneNormal= new Vector4f(0,           1,            0,             1);
        Vector4f leftPlanePoint   = new Vector4f(-scale / 2.0f, 0,          0,             1);
        Vector4f leftPlaneNormal  = new Vector4f(-1,          0,            0,             1);
        Vector4f rightPlanePoint  = new Vector4f( scale / 2.0f, 0,          0,             1);
        Vector4f rightPlaneNormal = new Vector4f( 1,           0,            0,             1);

        frontPlanePoint .mul(pointMatrixStack .last().pose());
        frontPlaneNormal.mul(normalMatrixStack.last().pose());
        backPlanePoint  .mul(pointMatrixStack .last().pose());
        backPlaneNormal .mul(normalMatrixStack.last().pose());
        topPlanePoint   .mul(pointMatrixStack .last().pose());
        topPlaneNormal  .mul(normalMatrixStack.last().pose());
        bottomPlanePoint.mul(pointMatrixStack .last().pose());
        bottomPlaneNormal.mul(normalMatrixStack.last().pose());
        leftPlanePoint  .mul(pointMatrixStack .last().pose());
        leftPlaneNormal .mul(normalMatrixStack.last().pose());
        rightPlanePoint .mul(pointMatrixStack .last().pose());
        rightPlaneNormal.mul(normalMatrixStack.last().pose());

        float step = scale / 100.0f;
        float max  = (float) cubeDiagFromCenterToCorner + z;

        for (float f = max + step; f > -max - step; f -= step)
        {
            Vector4f segmentStart = new Vector4f(screenMouseX, screenMouseY, f,        1);
            Vector4f segmentEnd   = new Vector4f(screenMouseX, screenMouseY, f - step, 1);

            float startFrontPlaneDot  = calcSegmentPlaneDotProduct(segmentStart, frontPlanePoint,  frontPlaneNormal);
            float endFrontPlaneDot    = calcSegmentPlaneDotProduct(segmentEnd,   frontPlanePoint,  frontPlaneNormal);
            float startBackPlaneDot   = calcSegmentPlaneDotProduct(segmentStart, backPlanePoint,   backPlaneNormal);
            float endBackPlaneDot     = calcSegmentPlaneDotProduct(segmentEnd,   backPlanePoint,   backPlaneNormal);
            float startTopPlaneDot    = calcSegmentPlaneDotProduct(segmentStart, topPlanePoint,    topPlaneNormal);
            float endTopPlaneDot      = calcSegmentPlaneDotProduct(segmentEnd,   topPlanePoint,    topPlaneNormal);
            float startBottomPlaneDot = calcSegmentPlaneDotProduct(segmentStart, bottomPlanePoint, bottomPlaneNormal);
            float endBottomPlaneDot   = calcSegmentPlaneDotProduct(segmentEnd,   bottomPlanePoint, bottomPlaneNormal);
            float startLeftPlaneDot   = calcSegmentPlaneDotProduct(segmentStart, leftPlanePoint,   leftPlaneNormal);
            float endLeftPlaneDot     = calcSegmentPlaneDotProduct(segmentEnd,   leftPlanePoint,   leftPlaneNormal);
            float startRightPlaneDot  = calcSegmentPlaneDotProduct(segmentStart, rightPlanePoint,  rightPlaneNormal);
            float endRightPlaneDot    = calcSegmentPlaneDotProduct(segmentEnd,   rightPlanePoint,  rightPlaneNormal);

            if (endFrontPlaneDot  < 0 && endBackPlaneDot   < 0 &&
                endTopPlaneDot    < 0 && endBottomPlaneDot  < 0 &&
                endLeftPlaneDot   < 0 && endRightPlaneDot   < 0)
            {
                if      (startFrontPlaneDot  * endFrontPlaneDot  < 0) return Direction.NORTH;
                else if (startBackPlaneDot   * endBackPlaneDot   < 0) return Direction.SOUTH;
                else if (startTopPlaneDot    * endTopPlaneDot    < 0) return Direction.UP;
                else if (startBottomPlaneDot * endBottomPlaneDot < 0) return Direction.DOWN;
                else if (startLeftPlaneDot   * endLeftPlaneDot   < 0) return Direction.WEST;
                else if (startRightPlaneDot  * endRightPlaneDot  < 0) return Direction.EAST;
                break;
            }
        }

        return null;
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Accessors
    // ──────────────────────────────────────────────────────────────────────────────

    @Nonnull
    public List<Direction> getSelectedDirections()
    {
        return selectedDirections;
    }

    public void setSelectedDirections(@Nonnull List<Direction> selectedDirections)
    {
        this.selectedDirections = new ArrayList<>(selectedDirections);
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Events
    // ──────────────────────────────────────────────────────────────────────────────

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