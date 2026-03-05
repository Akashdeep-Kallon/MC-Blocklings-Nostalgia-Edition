package com.akah.blocklings.client.gui.control.controls;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.math.Axis;
import com.akah.blocklings.client.gui.control.Control;
import com.akah.blocklings.client.gui.util.GuiUtil;
import com.akah.blocklings.client.gui.util.ScissorStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

import javax.annotation.Nonnull;

/**
 * A control that renders a block as a 3D interactive model inside a GUI panel.
 *
 * <h3>Chest / ENTITYBLOCK_ANIMATED fix</h3>
 * {@code BlockRenderDispatcher.renderSingleBlock()} silently skips blocks whose
 * {@link RenderShape} is {@code ENTITYBLOCK_ANIMATED} (chests, barrels,
 * furnaces …).
 * The previous workaround called {@code BlockEntityRenderer.render()} directly,
 * but
 * that renderer calls
 * {@code ChestBlock.opennessCombiner(blockEntity).acceptNone()}
 * which returns {@code FloatUnaryOperator.identity()} — so
 * {@code openNess = partialTick}
 * (~0.2–1.0 each frame), making the chest appear permanently open / hollow.
 *
 * <p>
 * The correct fix is to render the block's <em>item</em> form via
 * {@code ItemRenderer.renderStatic()} with {@link ItemDisplayContext#NONE}.
 * The item model is a static, always-closed representation that requires no
 * {@code Level} or live {@code BlockEntity}, and uses our custom
 * {@code PoseStack}
 * directly so the rotation / scale we have set up is respected.
 * </p>
 *
 * <h3>Shared-buffer flush fix</h3>
 * Using {@code mc.renderBuffers().bufferSource()} and then calling
 * {@code endBatch()}
 * on it flushes ALL pending GUI renders (text labels, tooltips, etc.) that were
 * batched earlier in the current frame — while our scissor is disabled —
 * causing
 * those pending draws to appear as stray glyphs / artefacts on screen.
 *
 * <p>
 * The fix is to use an <em>isolated</em> {@link MultiBufferSource.BufferSource}
 * built from a fresh {@link Tesselator} so that our {@code endBatch()} only
 * flushes
 * the block geometry we just submitted, leaving every other pending batch
 * untouched.
 * </p>
 */
@OnlyIn(Dist.CLIENT)
public class BlockControl extends Control {

    private double previousMouseX = 0.0;
    private double previousMouseY = 0.0;
    protected double dragAmount = 0.0;

    @Nonnull
    protected Quaternionf rotationQuat = new Quaternionf();

    @Nonnull
    private Block block = Blocks.AIR;
    private float blockScale = 0.6f;
    private boolean canMouseRotate = false;

    // Computed each frame by onRender(); exposed so subclasses
    // (BlockSideSelectionControl)
    // can use them for overlays that must be coplanar with the rendered block.
    protected float x;
    protected float y;
    protected float z;
    protected float scale;

    public BlockControl() {
        rotationQuat.mul(Axis.XP.rotationDegrees(30.0f));
        rotationQuat.mul(Axis.YP.rotationDegrees(45.0f));
    }

    @Override
    protected void onRender(@Nonnull PoseStack matrixStack, @Nonnull ScissorStack scissorStack,
            double mouseX, double mouseY, float partialTicks) {
        super.onRender(matrixStack, scissorStack, mouseX, mouseY, partialTicks);

        // ── mouse-drag rotation ────────────────────────────────────────────────────
        float mouseDeltaX = (float) ((mouseX - previousMouseX) / getPixelScaleX());
        float mouseDeltaY = (float) ((mouseY - previousMouseY) / getPixelScaleY());
        if (isPressed() && canMouseRotate()) {
            Quaternionf quat = Axis.YP.rotationDegrees((float) (mouseDeltaX * getPixelScaleX()) * 0.4f);
            quat.mul(Axis.XP.rotationDegrees((float) (mouseDeltaY * getPixelScaleY()) * 0.4f));
            quat.mul(rotationQuat);
            rotationQuat = quat;
            dragAmount += Math.abs(mouseDeltaX) + Math.abs(mouseDeltaY);
        } else {
            dragAmount = 0.0;
        }

        // ── shared positional state (used by subclasses) ───────────────────────────
        scale = (float) (Math.min(getWidth(), getHeight()) * getScaleX()) * getBlockScale();
        x = (float) ((getPixelX() / getPixelScaleX()) * getScaleX());
        y = (float) ((getPixelY() / getPixelScaleY()) * getScaleY());
        z = (float) getRenderZ();

        // ── render ─────────────────────────────────────────────────────────────────
        if (block != Blocks.AIR) {
            GuiUtil.disableScissor();
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

            // Build the pose: GUI-centre → our 3D rotation → unit-scale → block-corner
            // offset.
            PoseStack blockPoseStack = new PoseStack();
            blockPoseStack.translate(
                    (getPixelX() + getPixelWidth() / 2.0) / getGuiScale(),
                    (getPixelY() + getPixelHeight() / 2.0) / getGuiScale(),
                    getRenderZ());
            blockPoseStack.mulPose(rotationQuat);
            blockPoseStack.scale(scale, scale, scale);
            blockPoseStack.translate(-0.5, -0.5, -0.5);

            Minecraft mc = Minecraft.getInstance();
            BlockState state = getBlockState();

            // ── ISOLATED buffer source ─────────────────────────────────────────────
            // CRITICAL: do NOT use mc.renderBuffers().bufferSource() here.
            // Calling endBatch() on the shared source flushes every pending GUI
            // render (text, icons, etc.) that was batched earlier this frame —
            // while our scissor is disabled — producing stray glyph artefacts.
            // An immediate/isolated source only holds our own geometry.
            MultiBufferSource.BufferSource buf = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());

            if (state.getRenderShape() == RenderShape.ENTITYBLOCK_ANIMATED) {
                // ── ENTITYBLOCK_ANIMATED path (chest, barrel, furnace, hopper …) ──────
                //
                // WHY NOT BlockEntityRenderer:
                // ChestBlockEntityRenderer with a null-level BlockEntity falls into the
                // acceptNone() branch of opennessCombiner, which returns
                // FloatUnaryOperator.identity() → openNess == partialTick (~0–1).
                // The chest therefore renders with the lid raised on every frame.
                //
                // FIX: render the block's item form via ItemRenderer.
                // • ItemDisplayContext.NONE → no extra transform from the model's
                // "display" section; our blockPoseStack controls everything.
                // • The item model is always the closed/static representation.
                // • No Level or BlockEntity needed.
                ItemStack stack = new ItemStack(block.asItem());
                if (!stack.isEmpty()) {
                    mc.getItemRenderer().renderStatic(
                            stack,
                            ItemDisplayContext.NONE,
                            LightTexture.FULL_BRIGHT,
                            OverlayTexture.NO_OVERLAY,
                            blockPoseStack,
                            buf,
                            mc.level, // may be null — ItemRenderer handles it gracefully
                            0 // seed for model randomisation
                    );
                    buf.endBatch();
                }
            } else {
                // ── Standard MODEL block ───────────────────────────────────────────────
                BlockRenderDispatcher dispatcher = mc.getBlockRenderer();
                dispatcher.renderSingleBlock(state, blockPoseStack, buf,
                        LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                buf.endBatch();
            }

            GuiUtil.enableScissor();
        }

        previousMouseX = mouseX;
        previousMouseY = mouseY;
    }

    // ── accessors
    // ──────────────────────────────────────────────────────────────────

    @Nonnull
    public BlockState getBlockState() {
        return block.defaultBlockState();
    }

    @Nonnull
    public Block getBlock() {
        return block;
    }

    public void setBlock(@Nonnull Block block) {
        this.block = block;
    }

    public float getBlockScale() {
        return blockScale;
    }

    public void setBlockScale(float blockScale) {
        this.blockScale = blockScale;
    }

    public boolean canMouseRotate() {
        return canMouseRotate;
    }

    public void setCanMouseRotate(boolean canMouseRotate) {
        this.canMouseRotate = canMouseRotate;
    }

    @Nonnull
    public Quaternionf getRotationQuat() {
        return rotationQuat;
    }

    public void setRotationQuat(@Nonnull Quaternionf q) {
        this.rotationQuat = q;
    }
}