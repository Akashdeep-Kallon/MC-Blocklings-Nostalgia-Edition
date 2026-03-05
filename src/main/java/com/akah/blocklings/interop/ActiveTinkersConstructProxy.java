package com.akah.blocklings.interop;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.DoubleSupplier;

/**
 * Safe fallback active proxy while Tinkers API migration is pending.
 */
public class ActiveTinkersConstructProxy extends TinkersConstructProxy {
    @Nonnull @Override public List<Item> findAllWeapons() { return super.findAllWeapons(); }
    @Override public boolean isTinkersTool(@Nonnull Item item) { return false; }
    @Override public boolean isToolBroken(@Nonnull ItemStack stack) { return false; }
    @Override public boolean canToolHarvest(@Nonnull ItemStack stack, @Nonnull BlockState blockState) { return false; }
    @Override public float getToolHarvestSpeed(@Nonnull ItemStack stack, @Nonnull BlockState blockState) { return 0.0f; }
    @Override public boolean attackEntity(@Nonnull ItemStack stack, @Nonnull LivingEntity attackerLiving, @Nonnull InteractionHand hand, @Nonnull Entity targetEntity, @Nonnull DoubleSupplier cooldownFunction, boolean isExtraAttack) { return false; }
    @Override public boolean damageTool(@Nonnull ItemStack stack, int damage, @Nonnull LivingEntity entity) { return false; }
}
