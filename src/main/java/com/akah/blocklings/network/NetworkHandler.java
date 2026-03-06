package com.akah.blocklings.network;

import com.akah.blocklings.Blocklings;
import com.akah.blocklings.client.gui.BlocklingGuiHandler;
import com.akah.blocklings.entity.blockling.action.Action;
import com.akah.blocklings.entity.blockling.attribute.Attribute;
import com.akah.blocklings.entity.blockling.attribute.attributes.EnumAttribute;
import com.akah.blocklings.entity.blockling.attribute.attributes.numbers.FloatAttribute;
import com.akah.blocklings.entity.blockling.attribute.attributes.numbers.IntAttribute;
import com.akah.blocklings.entity.blockling.attribute.attributes.numbers.ModifiableFloatAttribute;
import com.akah.blocklings.entity.blockling.attribute.attributes.numbers.ModifiableIntAttribute;
import com.akah.blocklings.entity.blockling.goal.config.iteminfo.OrderedItemInfoSet;
import com.akah.blocklings.entity.blockling.goal.config.patrol.OrderedPatrolPointList;
import com.akah.blocklings.entity.blockling.goal.goals.container.BlocklingContainerGoal;
import com.akah.blocklings.entity.blockling.task.config.Property;
import com.akah.blocklings.network.messages.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

public class NetworkHandler
{
    /**
     * The protocol version.
     */
    private static final String PROTOCOL_VERSION = Integer.toString(1);

    /**
     * The simple channel handler.
     */
    private static final SimpleChannel HANDLER = NetworkRegistry.ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(Blocklings.MODID, "channel"))
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .simpleChannel();

    /**
     * Incremented for each registered message.
     */
    private static int id = 0;

    /**
     * Initialises all message handlers.
     */
    public static void init()
    {
        HANDLER.registerMessage(id++, SetLevelCommandMessage.class, SetLevelCommandMessage::encode, SetLevelCommandMessage::decode, SetLevelCommandMessage::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        HANDLER.registerMessage(id++, SetTypeCommandMessage.class, SetTypeCommandMessage::encode, SetTypeCommandMessage::decode, SetTypeCommandMessage::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        HANDLER.registerMessage(id++, SetXpCommandMessage.class, SetXpCommandMessage::encode, SetXpCommandMessage::decode, SetXpCommandMessage::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        registerMessage(Attribute.IsEnabledMessage.class);
        registerMessage(EnumAttribute.Message.class);
        registerMessage(FloatAttribute.ValueMessage.class);
        registerMessage(ModifiableFloatAttribute.BaseValueMessage.class);
        registerMessage(IntAttribute.ValueMessage.class);
        registerMessage(ModifiableIntAttribute.BaseValueMessage.class);

        registerMessage(Action.CountMessage.class);
        registerMessage(BlocklingAttackTargetMessage.class);
        registerMessage(BlocklingGuiHandler.OpenMessage.class);
        registerMessage(BlocklingNameMessage.class);
        registerMessage(BlocklingScaleMessage.class);
        registerMessage(BlocklingTypeMessage.class);
        registerMessage(EquipmentInventoryMessage.class);
        registerMessage(GoalStateMessage.class);
        registerMessage(SkillStateMessage.class);
        registerMessage(SkillTryBuyMessage.class);

        registerMessage(TaskCreateMessage.class);
        registerMessage(TaskPriorityMessage.class);
        registerMessage(TaskRemoveMessage.class);
        registerMessage(TaskCustomNameMessage.class);
        registerMessage(Property.TaskPropertyMessage.class);
        registerMessage(TaskSwapPriorityMessage.class);
        registerMessage(TaskTypeMessage.class);
        registerMessage(TaskTypeIsUnlockedMessage.class);

        registerMessage(OrderedItemInfoSet.AddItemInfoInfoMessage.class);
        registerMessage(OrderedItemInfoSet.RemoveItemInfoInfoMessage.class);
        registerMessage(OrderedItemInfoSet.MoveItemInfoInfoMessage.class);
        registerMessage(OrderedItemInfoSet.SetItemInfoInfoMessage.class);
        registerMessage(OrderedPatrolPointList.AddPatrolPointMessage.class);
        registerMessage(OrderedPatrolPointList.RemovePatrolPointMessage.class);
        registerMessage(OrderedPatrolPointList.MovePatrolPointMessage.class);
        registerMessage(OrderedPatrolPointList.UpdatePatrolPointMessage.class);
        registerMessage(WhitelistAllMessage.class);
        registerMessage(WhitelistIsUnlockedMessage.class);
        registerMessage(WhitelistSingleMessage.class);
        registerMessage(BlocklingContainerGoal.ContainerGoalContainerAddRemoveMessage.class);
        registerMessage(BlocklingContainerGoal.ContainerGoalContainerMessage.class);
        registerMessage(BlocklingContainerGoal.ContainerGoalContainerMoveMessage.class);
    }

    /**
     * Registers a blockling message.
     *
     * @param messageType the type of the message.
     */
    public static <T extends BlocklingMessage<T>> void registerMessage(@Nonnull Class<T> messageType)
    {
        Function<FriendlyByteBuf, T> decoder = (buf) ->
        {
            try
            {
                T message = messageType.getDeclaredConstructor().newInstance();
                message.decode(buf);

                return message;
            }
            catch (ReflectiveOperationException e)
            {
                Blocklings.LOGGER.warn("Failed to decode network message {}", messageType.getName(), e);

                return null;
            }
        };

        HANDLER.registerMessage(id++, messageType, BlocklingMessage::encode, decoder, BlocklingMessage::handle);
    }

    /**
     * Sends the given message to the server.
     *
     * @param message the message to send.
     */
    public static void sendToServer(Message message)
    {
        HANDLER.sendToServer(message);
    }

    /**
     * Sends the given message to the given player's client.
     *
     * @param player the player to send the message to.
     * @param message the message to send.
     */
    public static void sendToClient(Player player, Message message)
    {
        if (player instanceof ServerPlayer serverPlayer)
        {
            HANDLER.sendTo(message, serverPlayer.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
        }
        else
        {
            Blocklings.LOGGER.warn("sendToClient called with non-ServerPlayer: {}", player.getClass().getName());
        }
    }

    /**
     * Sends the given message to every player's client except the given players.
     *
     * @param world the world the players are in.
     * @param message the message to send.
     * @param playersToIgnore the players to not send the message to.
     */
    public static void sendToAllClients(Level world, Message message, List<Player> playersToIgnore)
    {
        for (Player player : world.players())
        {
            if (!playersToIgnore.contains(player))
            {
                sendToClient(player, message);
            }
        }
    }

    public static void sendToAllClients(Level world, Message message, UUID playerIdToIgnore)
    {
        if (!world.isClientSide && world instanceof ServerLevel serverLevel)
        {
            for (ServerPlayer player : serverLevel.getServer().getPlayerList().getPlayers())
            {
                if (playerIdToIgnore != null && player.getUUID().equals(playerIdToIgnore))
                {
                    continue;
                }

                sendToClient(player, message);
            }

            return;
        }

        for (Player player : world.players())
        {
            if (playerIdToIgnore != null && player.getUUID().equals(playerIdToIgnore))
            {
                continue;
            }

            // [audit-1.20.1] motivo: evita coste O(n*m) de contains(List) al excluir únicamente al emisor.
            sendToClient(player, message);
        }
    }

    /**
     * Sends the message either to the server or all player's clients.
     *
     * @param world the world.
     * @param message the message to send.
     */
    public static void sync(Level world, Message message)
    {
        if (world.isClientSide)
        {
            sendToServer(message);
        }
        else
        {
            // [audit-1.20.1] motivo: elimina asignación de lista vacía en cada sync de red.
            sendToAllClients(world, message, (UUID) null);
        }
    }
}
