package com.akah.blocklings.util;

import com.google.common.collect.Multimap;
import com.akah.blocklings.entity.blockling.BlocklingEntity;
import com.akah.blocklings.entity.blockling.BlocklingType;
import com.akah.blocklings.interop.TinkersConstructProxy;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A utility class for working with tools.
 */
public class ToolUtil
{
    /**
     * The list of tools that are classed as weapons.
     */
    private static final List<Item> WEAPONS = new ArrayList<>();

    /**
     * The list of tools that are classed as pickaxes.
     */
    private static final List<Item> PICKAXES = new ArrayList<>();

    /**
     * The list of tools that are classed as axes.
     */
    private static final List<Item> AXES = new ArrayList<>();

    /**
     * The list of tools that are classed as hoes.
     */
    private static final List<Item> HOES = new ArrayList<>();

    /**
     * The list of all items that are classed as tools.
     */
    private static final List<Item> TOOLS = new ArrayList<>();

    /**
     * Initialises the lists of tools.
     */
    public static void init()
    {
        WEAPONS.clear();
        PICKAXES.clear();
        AXES.clear();
        HOES.clear();

        WEAPONS.addAll(findAllWeapons());
        PICKAXES.addAll(BuiltInRegistries.ITEM.stream().filter(item -> item instanceof PickaxeItem).collect(Collectors.toList()));
        AXES.addAll(BuiltInRegistries.ITEM.stream().filter(item -> item instanceof AxeItem).collect(Collectors.toList()));
        HOES.addAll(BuiltInRegistries.ITEM.stream().filter(item -> item instanceof HoeItem).collect(Collectors.toList()));

        TOOLS.addAll(WEAPONS);
        TOOLS.addAll(PICKAXES);
        TOOLS.addAll(AXES);
        TOOLS.addAll(HOES);
    }

    /**
     * @return a list of all the items that are classed as weapons.
     */
    @Nonnull
    private static List<Item> findAllWeapons()
    {
        List<Item> weapons = BuiltInRegistries.ITEM.stream().filter(item -> item instanceof SwordItem).collect(Collectors.toList());
        weapons.addAll(TinkersConstructProxy.instance.findAllWeapons());

        return weapons;
    }

    /**
     * @return true if the given item is a weapon.
     */
    public static boolean isWeapon(@Nonnull ItemStack stack)
    {
        return isWeapon(stack.getItem());
    }

    /**
     * @return true if the given item is a weapon.
     */
    public static boolean isWeapon(@Nonnull Item item)
    {
        return WEAPONS.contains(item);
    }

    /**
     * @return true if the given item is a pickaxe.
     */
    public static boolean isPickaxe(@Nonnull ItemStack stack)
    {
        return isPickaxe(stack.getItem());
    }

    /**
     * @return true if the given item is a pickaxe.
     */
    public static boolean isPickaxe(@Nonnull Item item)
    {
        return PICKAXES.contains(item);
    }

    /**
     * @return true if the given item is a axe.
     */
    public static boolean isAxe(@Nonnull ItemStack stack)
    {
        return isAxe(stack.getItem());
    }

    /**
     * @return true if the given item is a axe.
     */
    public static boolean isAxe(@Nonnull Item item)
    {
        return AXES.contains(item);
    }

    /**
     * @return true if the given item is a hoe.
     */
    public static boolean isHoe(@Nonnull ItemStack stack)
    {
        return isHoe(stack.getItem());
    }

    /**
     * @return true if the given item is a hoe.
     */
    public static boolean isHoe(@Nonnull Item item)
    {
        return HOES.contains(item);
    }

    /**
     * @return true if the given item is a tool.
     */
    public static boolean isTool(@Nonnull ItemStack stack)
    {
        return isTool(stack.getItem());
    }

    /**
     * @return true if the given item is a tool.
     */
    public static boolean isTool(@Nonnull Item item)
    {
        return TOOLS.contains(item);
    }

    /**
     * @return true if the given item is a tool from Tinkers' Construct.
     */
    public static boolean isTinkersTool(@Nonnull ItemStack stack)
    {
        return isTinkersTool(stack.getItem());
    }

    /**
     * @return true if the given item is a tool from Tinkers' Construct.
     */
    public static boolean isTinkersTool(@Nonnull Item item)
    {
        return TinkersConstructProxy.instance.isTinkersTool(item);
    }

    /**
     * @return true if the given tool is in a useable state (e.g. Tinkers' tools aren't broken).
     */
    public static boolean isUseableTool(@Nonnull ItemStack stack)
    {
        if (!isTool(stack))
        {
            return false;
        }

        if (isTinkersTool(stack))
        {
            return TinkersConstructProxy.instance.isToolBroken(stack);
        }

        return true;
    }

    /**
     * @return the default attack speed of the given tool.
     */
    public static float getDefaultToolAttackSpeed(@Nonnull ItemStack stack)
    {
        return getToolAttackSpeed(stack, null);
    }

    /**
     * @param entity the target entity.
     * @return the attack speed of the given tool.
     */
    public static float getToolAttackSpeed(@Nonnull ItemStack stack, @Nullable LivingEntity entity)
    {
        if (isUseableTool(stack))
        {
            Multimap<Attribute, AttributeModifier> multimap = stack.getAttributeModifiers(EquipmentSlot.MAINHAND);
            double attackSpeedModifier = multimap.get(Attributes.ATTACK_SPEED).stream()
                    .filter(modifier -> modifier.getOperation() == AttributeModifier.Operation.ADDITION)
                    .mapToDouble(AttributeModifier::getAmount)
                    .sum();

            // Add 4.0f as this is the player's default attack speed value.
            return (float) (attackSpeedModifier + 4.0f);
        }

        return 4.0f;
    }

    /**
     * @return the default base damage of the given tool.
     */
    public static float getDefaultToolBaseDamage(@Nonnull ItemStack stack)
    {
        return getToolBaseDamage(stack, null);
    }

    /**
     * @param entity the target entity.
     * @return the base damage of the given tool.
     */
    public static float getToolBaseDamage(@Nonnull ItemStack stack, @Nullable LivingEntity entity)
    {
        if (isUseableTool(stack))
        {
            Multimap<Attribute, AttributeModifier> multimap = stack.getAttributeModifiers(EquipmentSlot.MAINHAND);

            // The tooltip the player sees includes the player's +1.0 damage as well.
            // But for mod compatibility reasons, we only use the tool-provided modifier.
            return (float) multimap.get(Attributes.ATTACK_DAMAGE).stream()
                    .filter(modifier -> modifier.getOperation() == AttributeModifier.Operation.ADDITION)
                    .mapToDouble(AttributeModifier::getAmount)
                    .sum();
        }

        return 0.0f;
    }

    /**
     * @return the additional damage of the given tool from its enchantments on the given creature type.
     */
    public static float getToolEnchantmentDamage(@Nonnull ItemStack stack, @Nonnull MobType creatureAttribute)
    {
        return EnchantmentHelper.getDamageBonus(stack, creatureAttribute);
    }

    /**
     * @return the knockback level of the given tool.
     */
    public static float getToolKnockbackLevel(@Nonnull ItemStack stack)
    {
        return EnchantmentHelper.getItemEnchantmentLevel(Enchantments.KNOCKBACK, stack);
    }

    /**
     * @return the fire aspect level of the given tool.
     */
    public static float getToolFireAspectLevel(@Nonnull ItemStack stack)
    {
        return EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_ASPECT, stack);
    }

    /**
     * @return the harvest speed for the given tool against stone, including enchantments.
     */
    public static float getDefaultToolMiningSpeedWithEnchantments(@Nonnull ItemStack stack)
    {
        return getDefaultToolMiningSpeed(stack) + getToolEnchantmentHarvestSpeed(stack);
    }

    /**
     * @return the harvest speed for the given tool against wood, including enchantments.
     */
    public static float getDefaultToolWoodcuttingSpeedWithEnchantments(@Nonnull ItemStack stack)
    {
        return getDefaultToolWoodcuttingSpeed(stack) + getToolEnchantmentHarvestSpeed(stack);
    }

    /**
     * @return the harvest speed for the given tool against crops, including enchantments.
     */
    public static float getDefaultToolFarmingSpeedWithEnchantments(@Nonnull ItemStack stack)
    {
        return getDefaultToolFarmingSpeed(stack) + getToolEnchantmentHarvestSpeed(stack);
    }

    /**
     * @return the harvest speed for the given tool against the given block state, including enchantments.
     */
    public static float getToolHarvestSpeedWithEnchantments(@Nonnull ItemStack stack, @Nonnull BlockState blockState)
    {
        return getToolHarvestSpeed(stack, blockState) + getToolEnchantmentHarvestSpeed(stack);
    }

    /**
     * @return the harvest speed for the given tool against stone (for reference a wooden pickaxe is 2.0f and diamond pickaxe is 8.0f).
     */
    public static float getDefaultToolMiningSpeed(@Nonnull ItemStack stack)
    {
        return getToolHarvestSpeed(stack, Blocks.STONE.defaultBlockState());
    }

    /**
     * @return the harvest speed for the given tool against wood.
     */
    public static float getDefaultToolWoodcuttingSpeed(@Nonnull ItemStack stack)
    {
        return getToolHarvestSpeed(stack, Blocks.OAK_LOG.defaultBlockState());
    }

    /**
     * @return the harvest speed for the given tool against crops.
     */
    public static float getDefaultToolFarmingSpeed(@Nonnull ItemStack stack)
    {
        return getToolHarvestSpeed(stack, Blocks.HAY_BLOCK.defaultBlockState());
    }

    /**
     * @return the harvest speed of the given stack against the given block state.
     */
    public static float getToolHarvestSpeed(@Nonnull ItemStack stack, @Nonnull BlockState blockState)
    {
        if (isUseableTool(stack))
        {
            if (isTinkersTool(stack))
            {
                if (canToolHarvest(stack, blockState))
                {
                    return TinkersConstructProxy.instance.getToolHarvestSpeed(stack, blockState);
                }
            }
            else
            {
                return stack.getDestroySpeed(blockState);
            }
        }

        return 0.0f;
    }

    /**
     * @return the default attack/mining/woodcutting/farming speed for the given tool and tool type.
     */
    public static float getDefaultToolSpeed(@Nonnull ItemStack stack, @Nonnull com.akah.blocklings.util.ToolType toolType)
    {
        switch (toolType)
        {
            case WEAPON:
                return getDefaultToolAttackSpeed(stack);
            case PICKAXE:
                return getDefaultToolMiningSpeed(stack);
            case AXE:
                return getDefaultToolWoodcuttingSpeed(stack);
            case HOE:
                return getDefaultToolFarmingSpeed(stack);
            default:
                return 0.0f;
        }
    }

    /**
     * @return the attack/mining/woodcutting/farming speed for the given tool and tool type.
     */
    public static float getToolHarvestSpeed(@Nonnull ItemStack stack, @Nonnull ToolContext context)
    {
        switch (context.toolType)
        {
            case WEAPON:
                return getToolAttackSpeed(stack, context.entity);
            case PICKAXE:
            case AXE:
            case HOE:
                return getToolHarvestSpeed(stack, context.blockState);
            default:
                return 0.0f;
        }
    }

    /**
     * @return the harvest speed for the given tool from only its enchantments.
     */
    public static float getToolEnchantmentHarvestSpeed(@Nonnull ItemStack stack)
    {
        int level = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_EFFICIENCY, stack);

        if (level > 0)
        {
            return level * level + 1.0f;
        }

        return 0.0f;
    }

    /**
     * @return true if the given tool can harvest the given block.
     */
    public static boolean canToolHarvest(@Nonnull ItemStack stack, @Nonnull BlockState blockState)
    {
        if (BlockUtil.isCrop(blockState.getBlock()) && ToolUtil.isHoe(stack))
        {
            return true;
        }

        if (BlockUtil.isOre(blockState.getBlock()) && !ToolUtil.isPickaxe(stack))
        {
            return false;
        }
        else if (BlockUtil.isLog(blockState.getBlock()) && !ToolUtil.isAxe(stack))
        {
            return false;
        }

        if (isTinkersTool(stack))
        {
            return TinkersConstructProxy.instance.canToolHarvest(stack, blockState);
        }
        else
        {
            return stack.isCorrectToolForDrops(blockState);
        }

    }

    /**
     * @return a list of all the enchantments on the given item.
     */
    @Nonnull
    public static List<Enchantment> findToolEnchantments(@Nonnull ItemStack stack)
    {
        List<Enchantment> enchantments = new ArrayList<>();
        ListTag listNBT = stack.getEnchantmentTags();

        for (int i = 0; i < listNBT.size(); i++)
        {
            CompoundTag tag = listNBT.getCompound(i);
            ResourceLocation enchantmentResource = ResourceLocation.tryParse(tag.getString("id"));

            if (enchantmentResource != null)
            {
                enchantments.add(BuiltInRegistries.ENCHANTMENT.get(enchantmentResource));
            }
        }

        return enchantments;
    }

    /**
     * Damages the given tool by the given amount.
     *
     * @param stack the tool to damage.
     * @param blockling the blockling equipping the tool.
     * @param damage the amount of damage to apply.
     * @return true if the stack was destroyed.
     */
    public static boolean damageTool(@Nonnull ItemStack stack, @Nonnull BlocklingEntity blockling, int damage)
    {
        ItemStack copiedStack = stack.copy();

        if (blockling.getNaturalBlocklingType() == BlocklingType.DIAMOND || blockling.getBlocklingType() == BlocklingType.DIAMOND)
        {
            copiedStack.enchant(Enchantments.UNBREAKING, EnchantmentHelper.getItemEnchantmentLevel(Enchantments.UNBREAKING, copiedStack) + 1);
        }

        boolean destroyed = copiedStack.hurt(damage, blockling.getRandom(), null);

        stack.setDamageValue(copiedStack.getDamageValue());

        return destroyed;
    }
}
