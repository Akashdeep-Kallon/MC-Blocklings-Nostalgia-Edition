package com.akah.blocklings.entity.blockling.task.config;

import com.akah.blocklings.entity.blockling.BlocklingEntity;
import com.akah.blocklings.entity.blockling.goal.BlocklingGoal;
import com.akah.blocklings.entity.blockling.task.Task;
import com.akah.blocklings.network.BlocklingMessage;
import com.akah.blocklings.util.IReadWriteNBT;
import com.akah.blocklings.util.Version;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;

public abstract class Property implements IReadWriteNBT {
    @Nonnull public final UUID id;
    @Nonnull public final BlocklingGoal goal;
    @Nonnull public final Component name;
    @Nonnull public final Component desc;
    private boolean isEnabled = true;

    public Property(@Nonnull String id, @Nonnull BlocklingGoal goal, @Nonnull Component name, @Nonnull Component desc) {
        this.id = UUID.fromString(id);
        this.goal = goal;
        this.name = name;
        this.desc = desc;
    }

    @Override
    public CompoundTag writeToNBT(@Nonnull CompoundTag propertyTag) {
        propertyTag.putUUID("id", id);
        propertyTag.putBoolean("is_enabled", isEnabled);
        return propertyTag;
    }

    @Override
    public void readFromNBT(@Nonnull CompoundTag propertyTag, @Nonnull Version tagVersion) {
        setEnabled(propertyTag.getBoolean("is_enabled"), false);
    }

    public void encode(@Nonnull FriendlyByteBuf buf) { buf.writeBoolean(isEnabled); }
    public void decode(@Nonnull FriendlyByteBuf buf) { setEnabled(buf.readBoolean(), false); }
    public boolean isEnabled() { return isEnabled; }
    public void setEnabled(boolean enabled) { isEnabled = enabled; }
    public void setEnabled(boolean enabled, boolean sync) {
        isEnabled = enabled;
        if (sync) { new TaskPropertyMessage(this).sync(); }
    }

    @OnlyIn(Dist.CLIENT)
    @Nonnull
    public abstract Object createControl();

    public static class TaskPropertyMessage extends BlocklingMessage<TaskPropertyMessage> {
        @Nullable private FriendlyByteBuf buf;
        @Nullable protected Property property;
        @Nullable private UUID taskId;
        private int propertyIndex;

        public TaskPropertyMessage() { super(null); }
        public TaskPropertyMessage(@Nonnull Property property) {
            super(property.goal.blockling);
            this.property = property;
        }

        @Override
        public void encode(@Nonnull FriendlyByteBuf buf) {
            super.encode(buf);
            buf.writeUUID(property.goal.getTask().id);
            buf.writeInt(property.goal.getTask().getGoal().properties.indexOf(property));
            property.encode(buf);
        }

        @Override
        public void decode(@Nonnull FriendlyByteBuf buf) {
            super.decode(buf);
            taskId = buf.readUUID();
            propertyIndex = buf.readInt();
            // FIX: buf.copy() crea una copia independiente de los bytes restantes.
            // El buf original de Netty se libera cuando setPacketHandled(true) es llamado
            // en el hilo de Netty, ANTES de que enqueueWork ejecute handle() en el
            // hilo del servidor. Sin esta copia, property.decode(buf) lanza
            // ByteBufAlreadyReleasedException (silenciosa) y el valor nunca se actualiza.
            this.buf = new FriendlyByteBuf(buf.copy());
        }

        @Override
        protected void handle(@Nonnull Player player, @Nonnull BlocklingEntity blockling) {
            try {
                Task task = blockling.getTasks().getTask(taskId);
                if (task != null && task.isConfigured()) {
                    property = task.getGoal().properties.get(propertyIndex);
                    property.decode(Objects.requireNonNull(buf));
                }
            } finally {
                // Liberar la copia para evitar memory leak
                if (buf != null) { buf.release(); buf = null; }
            }
        }
    }
}
