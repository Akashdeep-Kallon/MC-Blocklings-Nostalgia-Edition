package com.akah.blocklings.client.gui.control;

import com.akah.blocklings.util.event.EventBus;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * An event bus for controls that will forward events to the appropriate subscribers.
 */
@OnlyIn(Dist.CLIENT)
public class ControlEventBus extends EventBus<BaseControl>
{

}

