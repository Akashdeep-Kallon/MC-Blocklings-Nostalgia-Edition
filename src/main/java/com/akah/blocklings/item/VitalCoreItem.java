package com.akah.blocklings.item;

import com.akah.blocklings.entity.BlocklingsEntityTypes;
import com.akah.blocklings.entity.blockling.BlocklingEntity;
import com.akah.blocklings.entity.blockling.BlocklingType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * The Life Core (Vital Core) item.
 *
 * Right-clicking on a compatible block removes that block and spawns
 * a wild (untamed) blockling of the matching type in its place.
 * The item is consumed on use.
 *
 * The blockling is NOT tamed — the player must tame it separately.
 */
public class VitalCoreItem extends Item
{
    /**
     * Maps each compatible block to its corresponding BlocklingType.
     * Built once in the static initialiser after all BlocklingType instances exist.
     */
    private static final Map<Block, BlocklingType> BLOCK_TO_TYPE = new HashMap<>();

    static
    {
        // Surface types
        BLOCK_TO_TYPE.put(Blocks.GRASS_BLOCK, BlocklingType.GRASS);
        BLOCK_TO_TYPE.put(Blocks.DIRT, BlocklingType.DIRT);

        // Wood types
        BLOCK_TO_TYPE.put(Blocks.OAK_LOG, BlocklingType.OAK_LOG);
        BLOCK_TO_TYPE.put(Blocks.BIRCH_LOG, BlocklingType.BIRCH_LOG);
        BLOCK_TO_TYPE.put(Blocks.SPRUCE_LOG, BlocklingType.SPRUCE_LOG);
        BLOCK_TO_TYPE.put(Blocks.JUNGLE_LOG, BlocklingType.JUNGLE_LOG);
        BLOCK_TO_TYPE.put(Blocks.DARK_OAK_LOG, BlocklingType.DARK_OAK_LOG);
        BLOCK_TO_TYPE.put(Blocks.ACACIA_LOG, BlocklingType.ACACIA_LOG);

        // Stone & ore types
        BLOCK_TO_TYPE.put(Blocks.STONE, BlocklingType.STONE);
        BLOCK_TO_TYPE.put(Blocks.COBBLESTONE, BlocklingType.STONE);
        BLOCK_TO_TYPE.put(Blocks.IRON_ORE, BlocklingType.IRON);
        BLOCK_TO_TYPE.put(Blocks.DEEPSLATE_IRON_ORE, BlocklingType.IRON);
        BLOCK_TO_TYPE.put(Blocks.IRON_BLOCK, BlocklingType.IRON);
        BLOCK_TO_TYPE.put(Blocks.NETHER_QUARTZ_ORE, BlocklingType.QUARTZ);
        BLOCK_TO_TYPE.put(Blocks.QUARTZ_BLOCK, BlocklingType.QUARTZ);
        BLOCK_TO_TYPE.put(Blocks.LAPIS_ORE, BlocklingType.LAPIS);
        BLOCK_TO_TYPE.put(Blocks.DEEPSLATE_LAPIS_ORE, BlocklingType.LAPIS);
        BLOCK_TO_TYPE.put(Blocks.LAPIS_BLOCK, BlocklingType.LAPIS);
        BLOCK_TO_TYPE.put(Blocks.GOLD_ORE, BlocklingType.GOLD);
        BLOCK_TO_TYPE.put(Blocks.DEEPSLATE_GOLD_ORE, BlocklingType.GOLD);
        BLOCK_TO_TYPE.put(Blocks.NETHER_GOLD_ORE, BlocklingType.GOLD);
        BLOCK_TO_TYPE.put(Blocks.GOLD_BLOCK, BlocklingType.GOLD);
        BLOCK_TO_TYPE.put(Blocks.EMERALD_ORE, BlocklingType.EMERALD);
        BLOCK_TO_TYPE.put(Blocks.DEEPSLATE_EMERALD_ORE, BlocklingType.EMERALD);
        BLOCK_TO_TYPE.put(Blocks.EMERALD_BLOCK, BlocklingType.EMERALD);
        BLOCK_TO_TYPE.put(Blocks.DIAMOND_ORE, BlocklingType.DIAMOND);
        BLOCK_TO_TYPE.put(Blocks.DEEPSLATE_DIAMOND_ORE, BlocklingType.DIAMOND);
        BLOCK_TO_TYPE.put(Blocks.DIAMOND_BLOCK, BlocklingType.DIAMOND);
        BLOCK_TO_TYPE.put(Blocks.ANCIENT_DEBRIS, BlocklingType.NETHERITE);
        BLOCK_TO_TYPE.put(Blocks.NETHERITE_BLOCK, BlocklingType.NETHERITE);
        BLOCK_TO_TYPE.put(Blocks.OBSIDIAN, BlocklingType.OBSIDIAN);
        BLOCK_TO_TYPE.put(Blocks.GLOWSTONE, BlocklingType.GLOWSTONE);
    }

    public VitalCoreItem()
    {
        super(new Item.Properties().stacksTo(16));
    }

    @Nonnull
    @Override
    public InteractionResult useOn(@Nonnull UseOnContext context)
    {
        Level level = context.getLevel();

        if (level.isClientSide)
        {
            return InteractionResult.SUCCESS;
        }

        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Block clickedBlock = state.getBlock();

        BlocklingType targetType = BLOCK_TO_TYPE.get(clickedBlock);

        if (targetType == null)
        {
            return InteractionResult.PASS;
        }

        BlocklingEntity blockling = new BlocklingEntity(BlocklingsEntityTypes.BLOCKLING.get(), level);

        blockling.finalizeSpawn(
                (ServerLevelAccessor) level,
                level.getCurrentDifficultyAt(pos),
                MobSpawnType.SPAWNER,
                null,
                null
        );

        blockling.setNaturalBlocklingType(targetType, false);
        blockling.setBlocklingType(targetType, false);
        blockling.getStats().init();


        blockling.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);

        level.removeBlock(pos, false);
        level.addFreshEntity(blockling);

        playSpawnEffects((ServerLevel) level, pos);

        ItemStack stack = context.getItemInHand();

        if (context.getPlayer() != null && !context.getPlayer().getAbilities().instabuild)
        {
            stack.shrink(1);
        }

        return InteractionResult.CONSUME;
    }

    /**
     * Plays a burst of particles and a sound at the conversion position.
     *
     * @param level the server level.
     * @param pos the block position where the conversion happened.
     */
    private void playSpawnEffects(@Nonnull ServerLevel level, @Nonnull BlockPos pos)
    {
        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 0.5;
        double cz = pos.getZ() + 0.5;

        level.sendParticles(ParticleTypes.HEART, cx, cy + 0.8, cz, 6, 0.3, 0.3, 0.3, 0.05);
        level.sendParticles(ParticleTypes.ENCHANT, cx, cy + 0.5, cz, 20, 0.5, 0.5, 0.5, 0.2);
        level.sendParticles(ParticleTypes.POOF, cx, cy, cz, 8, 0.2, 0.2, 0.2, 0.05);

        level.playSound(null, pos, SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.6f, 1.2f);
        level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.4f, 1.5f);
    }
}
