package com.akah.blocklings.event;

import com.akah.blocklings.Blocklings;
import com.akah.blocklings.client.renderer.entity.BlocklingRenderer;
import com.akah.blocklings.client.renderer.entity.model.BlocklingModel;
import com.akah.blocklings.config.BlocklingsConfig;
import com.akah.blocklings.entity.BlocklingsEntityTypes;
import com.akah.blocklings.entity.blockling.BlocklingType;
import com.akah.blocklings.util.BlocklingsResourceLocation;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * Handles client-only mod bus events.
 */
@Mod.EventBusSubscriber(modid = Blocklings.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEventBusEvents
{
    /**
     * Registers entity renderers.
     */
    @SubscribeEvent
    public static void onRegisterRenderers(@Nonnull EntityRenderersEvent.RegisterRenderers event)
    {
        event.registerEntityRenderer(BlocklingsEntityTypes.BLOCKLING.get(), BlocklingRenderer::new);
    }

    /**
     * Registers model layer definitions.
     */
    @SubscribeEvent
    public static void onRegisterLayerDefinitions(@Nonnull EntityRenderersEvent.RegisterLayerDefinitions event)
    {
        event.registerLayerDefinition(BlocklingModel.LAYER_LOCATION, BlocklingModel::createBodyLayer);
    }

    /**
     * Used to prevent the merged blockling textures being created multiple times.
     */
    private static boolean blocklingTextureStitched = false;

    /**
     * Creates merged blockling type textures for each blockling type and each variant.
     */
    @SubscribeEvent
    public static void onTextureStitch(@Nonnull TextureStitchEvent.Post event)
    {
        if (blocklingTextureStitched)
        {
            return;
        }

        blocklingTextureStitched = true;

        if (BlocklingsConfig.CLIENT.disableDirtyBlocklings.get())
        {
            Blocklings.LOGGER.info("Skipping texture creation for merged blockling textures.");

            return;
        }

        ResourceLocation texture = null;

        for (int mask = 0; mask < 3; mask++)
        {
            final NativeImage maskNativeImage;

            try
            {
                texture = new BlocklingsResourceLocation("textures/entity/blockling/blockling_mask_" + mask + ".png");
                maskNativeImage = loadNativeImage(texture);
            }
            catch (IOException e)
            {
                Blocklings.LOGGER.warn("Couldn't find texture: " + texture);

                continue;
            }

            for (BlocklingType baseBlocklingType : BlocklingType.TYPES)
            {
                final NativeImage baseNativeImage;

                try
                {
                    baseNativeImage = loadNativeImage(baseBlocklingType.entityTexture);
                }
                catch (IOException e)
                {
                    Blocklings.LOGGER.warn("Couldn't find texture: " + baseBlocklingType.entityTexture);

                    continue;
                }

                for (BlocklingType outerBlocklingType : BlocklingType.TYPES)
                {
                    final NativeImage outerNativeImage;

                    try
                    {
                        outerNativeImage = loadNativeImage(outerBlocklingType.entityTexture);
                    }
                    catch (IOException e)
                    {
                        Blocklings.LOGGER.warn("Couldn't find texture: " + outerBlocklingType.entityTexture);

                        continue;
                    }

                    for (int i = 0; i < baseNativeImage.getWidth(); i++)
                    {
                        for (int j = 0; j < baseNativeImage.getHeight(); j++)
                        {
                            float maskAlpha = ((maskNativeImage.getPixelRGBA(i, j) >> 24) & 0xff) / 255.0f;

                            int baseColour = baseNativeImage.getPixelRGBA(i, j);
                            int baseBlue = (baseColour >> 16) & 0xff;
                            int baseGreen = (baseColour >> 8) & 0xff;
                            int baseRed = (baseColour & 0xff);

                            int outerColour = outerNativeImage.getPixelRGBA(i, j);
                            int outerBlue = (outerColour >> 16) & 0xff;
                            int outerGreen = (outerColour >> 8) & 0xff;
                            int outerRed = (outerColour & 0xff);

                            int a = 255;
                            int b = (int) ((baseBlue * maskAlpha) + (outerBlue * (1.0f - maskAlpha)));
                            int g = (int) ((baseGreen * maskAlpha) + (outerGreen * (1.0f - maskAlpha)));
                            int r = (int) ((baseRed * maskAlpha) + (outerRed * (1.0f - maskAlpha)));
                            int colour = (a << 24) + (b << 16) + (g << 8) + r;

                            outerNativeImage.setPixelRGBA(i, j, colour);
                        }
                    }

                    Minecraft.getInstance().textureManager.register(baseBlocklingType.getCombinedTexture(outerBlocklingType, mask), new DynamicTexture(outerNativeImage));
                }
            }
        }
    }

    private static NativeImage loadNativeImage(ResourceLocation texture) throws IOException
    {
        return NativeImage.read(Minecraft.getInstance().getResourceManager().open(texture));
    }
}
