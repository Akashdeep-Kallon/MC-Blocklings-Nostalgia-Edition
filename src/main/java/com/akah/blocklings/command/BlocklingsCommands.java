package com.akah.blocklings.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.akah.blocklings.Blocklings;
import com.akah.blocklings.entity.blockling.BlocklingType;
import com.akah.blocklings.entity.blockling.attribute.BlocklingAttributes.Level;
import com.akah.blocklings.network.NetworkHandler;
import com.akah.blocklings.network.messages.SetLevelCommandMessage;
import com.akah.blocklings.network.messages.SetTypeCommandMessage;
import com.akah.blocklings.network.messages.SetXpCommandMessage;
import com.akah.blocklings.util.BlocklingsComponent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * Handles the setup of blocklings commands.
 */
@Mod.EventBusSubscriber(modid = Blocklings.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BlocklingsCommands
{

    private static final DynamicCommandExceptionType ERROR_INVALID_LEVEL = new DynamicCommandExceptionType((obj) -> new BlocklingsComponent("command.argument.level.invalid", obj));

    private static final DynamicCommandExceptionType ERROR_INVALID_TYPE = new DynamicCommandExceptionType((obj) -> new BlocklingsComponent("command.argument.type.invalid", obj));

    /**
     * Registers argument types.
     */
    public static void init()
    {
    }

    /**
     * Registers the custom blocklings commands.
     */
    @SubscribeEvent
    public static void onRegisterCommands(@Nonnull RegisterCommandsEvent event)
    {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
                literal("blocklings")
                        .requires(source -> source.hasPermission(2))
                        .then(literal("set")
                                .then(literal("type")
                                        .then(literal("primary")
                                                .then(typeArgument("type")
                                                        .executes(context -> executeTypeCommand(context, false))))
                                        .then(literal("natural")
                                                .then(typeArgument("type")
                                                        .executes(context -> executeTypeCommand(context, true)))))
                                .then(literal("level")
                                        .then(levelArgument("level")
                                                .then(argument("value", IntegerArgumentType.integer(Level.MIN, Level.MAX))
                                                        .executes(BlocklingsCommands::executeLevelCommand))))
                                .then(literal("xp")
                                        .then(levelArgument("level")
                                                .then(argument("value", IntegerArgumentType.integer(0))
                                                        .executes(BlocklingsCommands::executeXpCommand)))))
                        );
    }

    /**
     * Executes the set blockling type commands.
     *
     * @param natural whether the type being set is the natural type or not.
     */
    private static int executeTypeCommand(@Nonnull CommandContext<CommandSourceStack> context, boolean natural) throws CommandSyntaxException
    {
        CommandSourceStack source = context.getSource();
        Player player = (Player) source.getEntity();

        if (player == null)
        {
            return 1;
        }

        String type = StringArgumentType.getString(context, "type");
        BlocklingType blocklingType = parseBlocklingType(type);

        NetworkHandler.sendToClient(player, new SetTypeCommandMessage(blocklingType.key, natural));

        return 0;
    }

    /**
     * Executes the set blockling level commands.
     */
    private static int executeLevelCommand(@Nonnull CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        CommandSourceStack source = context.getSource();
        Player player = (Player) source.getEntity();

        if (player == null)
        {
            return 1;
        }

        String levelArg = StringArgumentType.getString(context, "level");
        Level level = parseLevel(levelArg);
        int value = context.getArgument("value", Integer.class);

        NetworkHandler.sendToClient(player, new SetLevelCommandMessage(level, value));

        return 0;
    }

    /**
     * Executes the set blockling xp commands.
     */
    private static int executeXpCommand(@Nonnull CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        CommandSourceStack source = context.getSource();
        Player player = (Player) source.getEntity();

        if (player == null)
        {
            return 1;
        }

        String levelArg = StringArgumentType.getString(context, "level");
        Level level = parseLevel(levelArg);
        int value = context.getArgument("value", Integer.class);

        NetworkHandler.sendToClient(player, new SetXpCommandMessage(level, value));

        return 0;
    }


    private static RequiredArgumentBuilder<CommandSourceStack, String> typeArgument(String name)
    {
        return argument(name, StringArgumentType.word())
                .suggests((context, suggestionsBuilder) -> SharedSuggestionProvider.suggest(BlocklingType.TYPES.stream().map(blocklingType -> blocklingType.key), suggestionsBuilder));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> levelArgument(String name)
    {
        List<String> levels = Arrays.stream(Level.values()).filter(level -> level != Level.TOTAL).map(level -> level.name().toLowerCase()).collect(Collectors.toList());
        levels.add("all");

        return argument(name, StringArgumentType.word())
                .suggests((context, suggestionsBuilder) -> SharedSuggestionProvider.suggest(levels, suggestionsBuilder));
    }

    private static Level parseLevel(String arg) throws CommandSyntaxException
    {
        try
        {
            if (arg.equalsIgnoreCase("all"))
            {
                return Level.TOTAL;
            }

            Level level = Level.valueOf(arg.toUpperCase());

            if (level != Level.TOTAL)
            {
                return level;
            }
        }
        catch (IllegalArgumentException e)
        {
        }

        throw ERROR_INVALID_LEVEL.create(arg);
    }

    private static BlocklingType parseBlocklingType(String arg) throws CommandSyntaxException
    {
        String normalized = arg.toLowerCase();

        if (BlocklingType.TYPES.stream().anyMatch(blocklingType -> blocklingType.key.equals(normalized)))
        {
            return BlocklingType.find(normalized);
        }

        throw ERROR_INVALID_TYPE.create(arg);
    }
}
