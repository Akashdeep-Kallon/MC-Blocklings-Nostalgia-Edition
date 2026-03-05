package com.akah.blocklings.entity.blockling.goal.config;

import com.akah.blocklings.util.Version;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A class used to store information about a container.
 */
public class ContainerInfo
{
    @Nonnull
    public static final List<Direction> DEFAULT_SIDES = Arrays.asList(
            Direction.UP,
            Direction.WEST,
            Direction.EAST,
            Direction.SOUTH,
            Direction.NORTH,
            Direction.DOWN
    );

    /**
     * The position of the container.
     */
    @Nonnull
    private BlockPos blockPos = BlockPos.ZERO;

    /**
     * The block of the container.
     */
    @Nonnull
    private Block block = Blocks.AIR;

    /**
     * The list of sides to interact with in priority order.
     */
    @Nonnull
    private List<Direction> sides = new ArrayList<>();

    /**
     */
    public ContainerInfo()
    {
        this.sides.addAll(DEFAULT_SIDES);
    }

    /**
     * @param containerInfo the container info to copy.
     */
    public ContainerInfo(@Nonnull ContainerInfo containerInfo)
    {
        this(containerInfo.blockPos, containerInfo.block, containerInfo.sides);
    }

    /**
     * @param blockPos the position of the container.
     * @param block the block of the container.
     * @param sides the list of sides to interact with in priority order.
     */
    public ContainerInfo(@Nonnull BlockPos blockPos, @Nonnull Block block, @Nonnull List<Direction> sides)
    {
        this.blockPos = blockPos;
        this.block = block;
        setSides(sides);
    }

    /**
     * Writes the container info to a compound tag.
     *
     * @return the compound tag.
     */
    @Nonnull
    public CompoundTag writeToNBT()
    {
        CompoundTag compound = new CompoundTag();
        compound.putInt("x", getX());
        compound.putInt("y", getY());
        compound.putInt("z", getZ());
        compound.putString("block", BuiltInRegistries.BLOCK.getKey(block).toString());
        compound.putInt("sides", sides.size());

        for (int i = 0; i < sides.size(); i++)
        {
            compound.putInt("side" + i, sides.get(i).ordinal());
        }

        return compound;
    }

    /**
     * Reads a container info from a containerInfoTag tag.
     *
     * @param containerInfoTag the container info tag.
     * @param tagVersion the version of the tag.
     */
    @Nonnull
    public void readFromNBT(@Nonnull CompoundTag containerInfoTag, @Nonnull Version tagVersion)
    {
        setBlockPos(new BlockPos(containerInfoTag.getInt("x"), containerInfoTag.getInt("y"), containerInfoTag.getInt("z")));
        // [final-release-1.20.1] motivo: hardening NBT para evitar nulls y valores de lado corruptos.
        ResourceLocation blockId = ResourceLocation.tryParse(containerInfoTag.getString("block"));
        setBlock(blockId != null ? BuiltInRegistries.BLOCK.get(blockId) : Blocks.AIR);

        sides.clear();
        int size = containerInfoTag.getInt("sides");

        for (int i = 0; i < size; i++)
        {
            int sideOrdinal = containerInfoTag.getInt("side" + i);

            if (sideOrdinal >= 0 && sideOrdinal < Direction.values().length)
            {
                sides.add(Direction.values()[sideOrdinal]);
            }
        }

        if (sides.isEmpty())
        {
            sides.addAll(DEFAULT_SIDES);
        }
    }

    /**
     * Writes the container info to a buffer.
     *
     * @param buf the buffer to write to.
     */
    public void encode(@Nonnull FriendlyByteBuf buf)
    {
        buf.writeBlockPos(blockPos);
        buf.writeResourceLocation(BuiltInRegistries.BLOCK.getKey(block));
        buf.writeVarInt(sides.size());

        for (Direction side : sides)
        {
            buf.writeEnum(side);
        }
    }

    /**
     * Reads a container info from a buffer.
     *
     * @param buf the buffer to read from.
     * @return the container info.
     */
    @Nonnull
    public void decode(@Nonnull FriendlyByteBuf buf)
    {
        setBlockPos(buf.readBlockPos());

        // [final-release-1.20.1] motivo: corrige decode de red (encode usa ResourceLocation, no id numérico de registry).
        ResourceLocation blockId = buf.readResourceLocation();
        setBlock(BuiltInRegistries.BLOCK.get(blockId));

        sides.clear();
        int size = buf.readVarInt();

        for (int i = 0; i < size; i++)
        {
            sides.add(buf.readEnum(Direction.class));
        }

        if (sides.isEmpty())
        {
            sides.addAll(DEFAULT_SIDES);
        }
    }

    /**
     * @return whether the container info is configured with a block and sides to interact with.
     */
    public boolean isConfigured()
    {
        return getBlock() != Blocks.AIR && !getSides().isEmpty();
    }

    /**
     * @return the x coordinate of the container.
     */
    public int getX()
    {
        return blockPos.getX();
    }

    /**
     * Sets the x coordinate of the container.
     */
    public void setX(int x)
    {
        this.blockPos = new BlockPos(x, blockPos.getY(), blockPos.getZ());
    }

    /**
     * @return the y coordinate of the container.
     */
    public int getY()
    {
        return blockPos.getY();
    }

    /**
     * Sets the y coordinate of the container.
     */
    public void setY(int y)
    {
        this.blockPos = new BlockPos(blockPos.getX(), y, blockPos.getZ());
    }

    /**
     * @return the z coordinate of the container.
     */
    public int getZ()
    {
        return blockPos.getZ();
    }

    /**
     * Sets the z coordinate of the container.
     */
    public void setZ(int z)
    {
        this.blockPos = new BlockPos(blockPos.getX(), blockPos.getY(), z);
    }

    /**
     * @return the position of the container.
     */
    @Nonnull
    public BlockPos getBlockPos()
    {
        return blockPos;
    }

    /**
     * Sets the position of the container.
     */
    public void setBlockPos(@Nonnull BlockPos blockPos)
    {
        this.blockPos = blockPos;
    }

    /**
     * @return the block of the container.
     */
    @Nonnull
    public Block getBlock()
    {
        return block;
    }

    /**
     * Sets the block of the container.
     */
    public void setBlock(@Nonnull Block block)
    {
        this.block = block;
    }

    /**
     * @return the list of sides to interact with in priority order.
     */
    @Nonnull
    public List<Direction> getSides()
    {
        return sides;
    }

    /**
     * Sets the list of sides to interact with in priority order.
     */
    public void setSides(@Nonnull List<Direction> sides)
    {
        this.sides = new ArrayList<>();

        if (sides.isEmpty())
        {
            this.sides.addAll(DEFAULT_SIDES);
            return;
        }

        for (Direction side : sides)
        {
            if (side != null && !this.sides.contains(side))
            {
                this.sides.add(side);
            }
        }
    }
}
