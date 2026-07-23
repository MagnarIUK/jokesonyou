package com.magnariuk;

import com.magnariuk.abstracts.ProtectedItemAccessor;
import com.magnariuk.records.PenaltyEntry;
import com.magnariuk.records.TimeEntry;
import com.magnariuk.records.TimeMap;
import com.magnariuk.utils.PlayerUtils;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.ModInitializer;


import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;


import static com.magnariuk.Utility.*;
import static com.magnariuk.Utility.sendTitle;



public class JokesOnYou implements ModInitializer {
	public static final String MOD_ID = "jokesonyou";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

	public static final AttachmentType<TimeMap> TIME_DATA =
			AttachmentRegistry.createPersistent(Identifier.fromNamespaceAndPath(MOD_ID, "time_data"), TimeMap.TIME_MAP_CODEC);
	public static final Supplier<TimeMap> TIME_DATA_SUPPLIER = ()  -> new TimeMap("none",  false, new ArrayList<>(), new ArrayList<>());



    @Override
	public void onInitialize() {

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(Commands.literal("jokesonyou")

					.then(Commands.literal("startTheGame")
							.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
							.executes(JokesOnYou::startTheGame)
					)

					.then(Commands.literal("getData")
							.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
							.executes(JokesOnYou::getData)
					)

					.then(Commands.literal("getResults")
							.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
							.executes(JokesOnYou::getResults)
					)

					.then(Commands.literal("giveJokeCard").executes(JokesOnYou::giveJokeCard))

					.then(Commands.literal("amIJoke").executes(JokesOnYou::amIJoke))

					.then(Commands.literal("pauseTheGame")
							.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
							.then(Commands.argument("pause", BoolArgumentType.bool())
									.executes(context -> JokesOnYou.stopGame(context, BoolArgumentType.getBool(context, "pause")))
							)
					)

					.then(Commands.literal("rules")
							.executes(context -> JokesOnYou.gameRules(context, false))

							.then(Commands.argument("broadcast", BoolArgumentType.bool())
									.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
									.executes(context -> JokesOnYou.gameRules(context, BoolArgumentType.getBool(context, "broadcast")))
							)
					)

					.then(Commands.literal("remove")
							.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
							.executes(JokesOnYou::removeJoke))
			);
		});


		LOGGER.info("Jokes on you initialized!");
	}


	public static void handleJokePass(ServerPlayer player, long penalty) {
		MinecraftServer server = player.level().getServer();
		if (server == null) return;

		ServerLevel world = server.getLevel(Level.OVERWORLD);
        assert world != null;
        TimeMap timeMap = world.getAttachedOrCreate(TIME_DATA, TIME_DATA_SUPPLIER);

		if (timeMap != null) {
			String playerName = player.getGameProfile().name();

			if (timeMap.isPaused() && !timeMap.current().equals(playerName)) {
				player.sendSystemMessage(Component.translatable("jokesonyou.commands.game_paused", timeMap.current()));
				return;
			}

			if (!timeMap.current().equals(playerName)) {
					TimeMap newState = timeMap.update(playerName, new TimeEntry(playerName, Instant.now().getEpochSecond()));

					if (penalty > 0) {
						newState = newState.updatePenalty(playerName, penalty);
					}

					world.setAttached(TIME_DATA, newState);
				Utility.sendTitle(player, Component.translatable("jokesonyou.name").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
				if (penalty > 0) {
					Utility.sendTitle(player, Component.translatable("jokesonyou.penalty", penalty).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD), true);
				}

				ServerPlayer oldOwner = server.getPlayerList().getPlayer(timeMap.current());
				if (oldOwner != null) {
					Utility.sendTitle(oldOwner, Component.translatable("jokesonyou.not_on_you").withStyle(ChatFormatting.GREEN));
				}
			}
		} else {
			PlayerUtils.sendMessage(player, Component.translatable("jokesonyou.game.not_running"));
		}
	}

	public enum PickupResult {
		YOU_ARE_JOKE,
		NOT_JOKE,
		GAME_NOT_RUNNING,
		GAME_PAUSED_RETURN_TO_JOKE
	}

	public static PickupResult validatePickup(ServerPlayer player) {
		MinecraftServer server = player.level().getServer();
		if (server == null) return PickupResult.GAME_NOT_RUNNING;

		ServerLevel world = server.getLevel(Level.OVERWORLD);
        assert world != null;
        TimeMap timeMap = world.getAttachedOrCreate(TIME_DATA, TIME_DATA_SUPPLIER);

		if (timeMap == null) {
			return PickupResult.GAME_NOT_RUNNING;
		}

		String playerName = player.getGameProfile().name();
		String currentJokeHolder = timeMap.current();

		if (timeMap.isPaused() && !currentJokeHolder.equals(playerName)) {
			return PickupResult.GAME_PAUSED_RETURN_TO_JOKE;
		}

		if (currentJokeHolder.equals(playerName)) {
			return PickupResult.YOU_ARE_JOKE;
		} else {
			return PickupResult.NOT_JOKE;
		}
	}

	private static int removeJoke(CommandContext<CommandSourceStack> context){
		CommandSourceStack source = context.getSource();
		MinecraftServer server = source.getServer();
		ServerLevel world = server.getLevel(Level.OVERWORLD);
		TimeMap tm = world.getAttachedOrElse(TIME_DATA, null);
		if(tm!=null) {
			Player cur =  server.getPlayerList().getPlayer(tm.current());
			if(cur!=null) {

				Inventory inv = cur.getInventory();
				for (int slot = 0; slot < inv.getContainerSize(); slot++) {
					ItemStack stack = inv.getItem(slot);
					if (Utility.isJokeCard(stack)){
						inv.removeItemNoUpdate(slot);
						break;
					}
				}
			} else {
				source.sendSystemMessage(Component.translatable("jokesonyou.admin.cant_be_removed"));
			}

			getResults(context);
			world.removeAttached(TIME_DATA);
			source.sendSystemMessage(Component.translatable("jokesonyou.game.ended"));
		} else{
			source.sendSystemMessage(Component.translatable("jokesonyou.game.hasnt_started"));
		}


		return 1;
	}

	private static int stopGame(CommandContext<CommandSourceStack> context, boolean paused) {
		CommandSourceStack source = context.getSource();
		MinecraftServer server = source.getServer();
		ServerLevel world = server.getLevel(Level.OVERWORLD);
		TimeMap timeMap =  world.getAttachedOrElse(TIME_DATA, null);

		if(timeMap!=null) {
			if(timeMap.isPaused()){
				if(paused){
					source.sendSystemMessage(Component.translatable("jokesonyou.game.already_paused"));
				} else{
					world.setAttached(TIME_DATA, timeMap.setPaused(false));
					source.sendSystemMessage(Component.translatable("jokesonyou.game.resumed"));
				}
			} else{
				if(paused){
					world.setAttached(TIME_DATA, timeMap.setPaused(true));
					source.sendSystemMessage(Component.translatable("jokesonyou.commands.game_paused"));
				} else{
					source.sendSystemMessage(Component.translatable("jokesonyou.game.already_paused"));
				}
			}
		} else{
			source.sendSystemMessage(Component.translatable("jokesonyou.game.hasnt_started"));
		}

		return 1;
	}

	private static int gameRules(CommandContext<CommandSourceStack> context, boolean broadcast) {
		CommandSourceStack source = context.getSource();
		MinecraftServer server = source.getServer();

		Collection<ServerPlayer> targets;
		if (broadcast) {
			targets = server.getPlayerList().getPlayers();
		} else {
			ServerPlayer player = source.getPlayer();
			if (player == null) return 0;
			targets = Collections.singletonList(player);
		}

		for (ServerPlayer player : targets) {
			MutableComponent message = Component.translatable("jokesonyou.rules.intro", player.getDisplayName());

			player.sendSystemMessage(message, false);
		}
		return 1;

	}

	private static int startTheGame(CommandContext<CommandSourceStack> context){
		CommandSourceStack source = context.getSource();
		MinecraftServer server = source.getServer();
		ServerLevel world = server.getLevel(Level.OVERWORLD);
		TimeMap timeMap =  world.getAttachedOrElse(TIME_DATA, null);
		if(timeMap == null) {
			TimeMap FtimeMap = world.getAttachedOrCreate(TIME_DATA, TIME_DATA_SUPPLIER);
			List<ServerPlayer> players = server.getPlayerList().getPlayers();
			Random random = new Random();
			int size = players.size();
			int selected_player = random.nextInt(0, size);

			//sendTitle(players, "Who's gonna play today?", ChatFormatting.GREEN, false);
			sendTitle(players, Component.translatable("jokesonyou.start.0"));
			scheduler.schedule( () -> {
				sendTitle(players, Component.translatable("jokesonyou.start.1").withStyle(ChatFormatting.GREEN));
			}, 5, TimeUnit.SECONDS );
			scheduler.schedule( () -> {
				sendTitle(players, Component.translatable("jokesonyou.start.2").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
			}, 10, TimeUnit.SECONDS );
			scheduler.schedule( () -> {
				sendTitle(players, Component.translatable("jokesonyou.start.3").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
				}, 15, TimeUnit.SECONDS );
			scheduler.schedule( () -> {
				try{
					world.setAttached(TIME_DATA, FtimeMap.update(players.get(selected_player).getGameProfile().name(), new TimeEntry(players.get(selected_player).getGameProfile().name(), Instant.now().getEpochSecond())));
					players.get(selected_player).getInventory().add(getJokeCard());
					sendTitle(players, Component.translatable("jokesonyou.start.4").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
				} catch(Exception e){
					e.printStackTrace();
				}
				gameRules(context, true);

			}, 17, TimeUnit.SECONDS );


		}else{
			source.sendSystemMessage(Component.translatable("jokesonyou.game.already_running"));
		}

		return 1;
	}
	private static int amIJoke(CommandContext<CommandSourceStack> context){
		CommandSourceStack source = context.getSource();
		MinecraftServer server = source.getServer();
		ServerLevel world = server.getLevel(Level.OVERWORLD);
		TimeMap timeMap = world.getAttachedOrElse(TIME_DATA, null);

		if (timeMap != null) {
			if(!timeMap.isPaused()){
				if(timeMap.current().equals(context.getSource().getPlayer().getName().getString())) {
					source.sendSystemMessage(Component.translatable("jokesonyou.commands.joke.you_are"));
				} else{
					source.sendSystemMessage(Component.translatable("jokesonyou.commands.joke.you_are_not"));
				}
			} else{
				if(timeMap.current().equals(context.getSource().getPlayer().getName().getString())) {
					source.sendSystemMessage(Component.literal("You are a Joke! (Game is paused)"));
					source.sendSystemMessage(
							Component.translatable("jokesonyou.commands.joke.you_are")
									.append("(")
									.append(Component.translatable("jokesonyou.game.paused")).append(")")
					);
				} else{
					source.sendSystemMessage(
							Component.translatable("jokesonyou.commands.joke.you_are_not")
									.append("(")
									.append(Component.translatable("jokesonyou.game.paused")).append(")")
					);				}
			}
		} else{
			source.sendSystemMessage(Component.translatable("jokesonyou.game.hasnt_started"));
		}


		return 1;
	}

	private static int giveJokeCard(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		MinecraftServer server = source.getServer();
		ServerLevel world = server.getLevel(Level.OVERWORLD);
		TimeMap timeMap = world.getAttachedOrElse(TIME_DATA, null);
		if(timeMap != null) {
			if(!timeMap.isPaused()){
				if(timeMap.current().equals(context.getSource().getPlayer().getName().getString())) {
					LOGGER.info(String.valueOf(checkJokeCard(context)));
					if(!checkJokeCard(context)) {
						ServerPlayer player = source.getPlayerOrException();
						boolean added = player.getInventory().add(getJokeCard());

						if (!added) {
							ItemEntity droppedItem = player.drop(getJokeCard(), false);

							if (droppedItem != null) {
								droppedItem.setUnlimitedLifetime();
								droppedItem.setTarget(player.getUUID());
								droppedItem.setGlowingTag(true);
								droppedItem.setPickUpDelay(0);
								((ProtectedItemAccessor) droppedItem).jokesOnYou$setProtected(true);
							}
						}
					} else{
						source.sendSystemMessage(Component.translatable("jokesonyou.commands.joke.you_already_have"));
					}
				} else {
					source.sendSystemMessage(Component.translatable("jokesonyou.commands.joke_needs_to_execute"));
				}
			} else{
				source.sendSystemMessage(Component.translatable("jokesonyou.game.paused"));
			}
		} else{
			source.sendSystemMessage(Component.translatable("jokesonyou.game.hasnt_started"));
		}


		return 1;
	}

	private static int getData(CommandContext<CommandSourceStack> context){
		CommandSourceStack source = context.getSource();
		MinecraftServer server = source.getServer();
		ServerLevel world = server.getLevel(Level.OVERWORLD);
		StringBuilder v = new StringBuilder();
		TimeMap te =  world.getAttachedOrElse(TIME_DATA, null);
		if(te != null) {
			v.append("Data\n");
			v.append(String.format("Paused: %b\n", te.isPaused()));
			v.append(String.format("Current Joke: %s\n", te.current()));
			v.append("Entries: \n");
			for (TimeEntry timeEntry : te.entries()) {
				v.append(String.format("Player: %s, Time: %d\n", timeEntry.player(), timeEntry.time()));
			}
			v.append("Penalties: \n");
			for (PenaltyEntry penaltyEntry : te.penalties()) {
				v.append(String.format("Player: %s, Time: %d\n", penaltyEntry.player(), penaltyEntry.time()));
			}

			source.sendSystemMessage( Component.literal(v.toString()) );
		} else{
			source.sendSystemMessage( Component.translatable("jokesonyou.game.hasnt_started"));
		}
		return 1;
	}

	private static int getResults(CommandContext<CommandSourceStack> context){
		try{
			CommandSourceStack source = context.getSource();
			MinecraftServer server = source.getServer();
			ServerLevel world = server.getLevel(Level.OVERWORLD);
			TimeMap te = world.getAttachedOrElse(TIME_DATA, null);
			if(te != null) {

				Map<String, Long> players = calculateDurations(te.entries(), te.penalties());
				StringBuilder v = new StringBuilder();
				v.append("Results\n");
				players.forEach((playerName, duration) -> {
					v.append(String.format("Player: %s, Time: %s\n", playerName, convertToTime(duration)));
				});
				source.sendSystemMessage( Component.literal(v.toString()) );
			} else{
				source.sendSystemMessage( Component.translatable("jokesonyou.game.hasnt_started"));
			}
		} catch(Exception e){
			e.printStackTrace();
		}
		return 1;
	}
}