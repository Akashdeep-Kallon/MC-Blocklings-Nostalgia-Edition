package com.akah.blocklings;

import com.akah.blocklings.block.BlocklingsBlocks;
import com.akah.blocklings.capabilities.BlocklingsCapabilities;
import com.akah.blocklings.client.gui.containers.BlocklingsMenuTypes;
import com.akah.blocklings.command.BlocklingsCommands;
import com.akah.blocklings.config.BlocklingsConfig;
import com.akah.blocklings.entity.BlocklingsEntityTypes;
import com.akah.blocklings.interop.ModProxies;
import com.akah.blocklings.item.BlocklingItem;
import com.akah.blocklings.item.BlocklingsItems;
import com.akah.blocklings.item.ModCreativeTabs;
import com.akah.blocklings.network.NetworkHandler;
import com.akah.blocklings.sound.BlocklingsSounds;
import com.akah.blocklings.util.ObjectUtil;
import com.akah.blocklings.util.Version;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Blocklings.MODID)
public class Blocklings
{
    /**
     * The mod's ID, which is also the mod's namespace.
     */
    @Nonnull
    public static final String MODID = "blocklings";

    /**
     * The mod's version.
     */
    @Nonnull
    public static final Version VERSION = new Version(ObjectUtil.coalesce(Blocklings.class.getPackage().getSpecificationVersion(), "99999.0.0.0"));

    /**
     * The mod's logger.
     */
    public static final Logger LOGGER = LogManager.getLogger();

    /**
     * The mod's constructor.
     */
    @SuppressWarnings("removal")
    public Blocklings()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        BlocklingsEntityTypes.register(modEventBus);
        BlocklingsBlocks.register(modEventBus);
        BlocklingsItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        BlocklingsSounds.register(modEventBus);
        BlocklingsMenuTypes.register(modEventBus);

        modEventBus.addListener(this::setupCommon);
        modEventBus.addListener(this::setupClient);
        modEventBus.addListener(BlocklingsCapabilities::register);

        MinecraftForge.EVENT_BUS.register(this);

        BlocklingsConfig.init();
    }

    /**
     * Setup shared between client and server.
     */
    private void setupCommon(final FMLCommonSetupEvent event)
    {
        ModProxies.init();
        NetworkHandler.init();
        BlocklingsCommands.init();

    }

    /**
     * Setup only on the client.
     */
    private void setupClient(final FMLClientSetupEvent event)
    {
        BlocklingItem.registerItemModelsProperties();
    }
}
