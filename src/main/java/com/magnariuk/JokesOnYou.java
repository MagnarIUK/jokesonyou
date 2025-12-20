package com.magnariuk;

import com.magnariuk.records.PenaltyEntry;
import com.magnariuk.records.TimeEntry;
import com.magnariuk.records.TimeMap;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;


import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
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
			AttachmentRegistry.createPersistent(Identifier.of(JokesOnYou.MOD_ID, "time_data"), TimeMap.TIME_MAP_CODEC);
	public static final Supplier<TimeMap> TIME_DATA_SUPPLIER = ()  -> new TimeMap("none",  false, new ArrayList<>(), new ArrayList<>());



    @Override
	public void onInitialize() {

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(CommandManager.literal("jokesonyou")

					.then(CommandManager.literal("startTheGame")
							.requires(source -> source.hasPermissionLevel(2))
							.executes(JokesOnYou::startTheGame)
					)

					.then(CommandManager.literal("getData")
							.requires(source -> source.hasPermissionLevel(2))
							.executes(JokesOnYou::getData)
					)

					.then(CommandManager.literal("getResults")
							.requires(source -> source.hasPermissionLevel(2))
							.executes(JokesOnYou::getResults)
					)

					.then(CommandManager.literal("giveJokeCard").executes(JokesOnYou::giveJokeCard))

					.then(CommandManager.literal("amIJoke").executes(JokesOnYou::amIJoke))

					.then(CommandManager.literal("pauseTheGame")
							.requires(source -> source.hasPermissionLevel(2))
							.then(CommandManager.argument("pause", BoolArgumentType.bool())
									.executes(context -> JokesOnYou.stopGame(context, BoolArgumentType.getBool(context, "pause")))
							)
					)

					.then(CommandManager.literal("rules")
							.executes(context -> JokesOnYou.gameRules(context, false))

							.then(CommandManager.argument("broadcast", BoolArgumentType.bool())
									.requires(source -> source.hasPermissionLevel(2))
									.executes(context -> JokesOnYou.gameRules(context, BoolArgumentType.getBool(context, "broadcast")))
							)
					)

					.then(CommandManager.literal("remove")
							.requires(source -> source.hasPermissionLevel(2))
							.executes(JokesOnYou::removeJoke))
			);
		});


		LOGGER.info("Jokes on you initialized!");
	}


	public static void handleJokePass(ServerPlayerEntity player, long penalty) {
		MinecraftServer server = player.getServer();
		if (server == null) return;

		ServerWorld world = server.getWorld(World.OVERWORLD);
		TimeMap timeMap = world.getAttachedOrCreate(TIME_DATA, TIME_DATA_SUPPLIER);

		if (timeMap != null) {
			String playerName = player.getGameProfile().getName();

			if (timeMap.isPaused() && !timeMap.current().equals(playerName)) {
				player.sendMessage(Text.of("Game is paused. Please, return The Joke to " + timeMap.current()), true);
				return;
			}

			if (!timeMap.current().equals(playerName)) {
					TimeMap newState = timeMap.update(playerName, new TimeEntry(playerName, Instant.now().getEpochSecond()));

					if (penalty > 0) {
						newState = newState.updatePenalty(playerName, penalty);
					}

					world.setAttached(TIME_DATA, newState);
				Utility.sendTitle(player, "Joke's on you!", Formatting.RED, true);
				if (penalty > 0) {
					Utility.sendTitle(player, String.format("You got penalty: %d seconds", penalty), Formatting.YELLOW, false, true);
				}

				ServerPlayerEntity oldOwner = server.getPlayerManager().getPlayer(timeMap.current());
				if (oldOwner != null) {
					Utility.sendTitle(oldOwner, "Joke's not on you!", Formatting.GREEN, false);
				}
			}
		} else {
			player.sendMessage(Text.of("Game is not running."), true);
		}
	}

	public enum PickupResult {
		YOU_ARE_JOKE,
		NOT_JOKE,
		GAME_NOT_RUNNING,
		GAME_PAUSED_RETURN_TO_JOKE
	}

	public static PickupResult validatePickup(ServerPlayerEntity player) {
		MinecraftServer server = player.getServer();
		if (server == null) return PickupResult.GAME_NOT_RUNNING;

		ServerWorld world = server.getWorld(World.OVERWORLD);
		TimeMap timeMap = world.getAttachedOrCreate(TIME_DATA, TIME_DATA_SUPPLIER);

		if (timeMap == null) {
			return PickupResult.GAME_NOT_RUNNING;
		}

		String playerName = player.getGameProfile().getName();
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

	private static int removeJoke(CommandContext<ServerCommandSource> context){
		ServerCommandSource source = context.getSource();
		MinecraftServer server = source.getServer();
		ServerWorld world = server.getWorld(World.OVERWORLD);
		TimeMap tm = world.getAttachedOrElse(TIME_DATA, null);
		if(tm!=null) {
			PlayerEntity cur =  server.getPlayerManager().getPlayer(tm.current());
			if(cur!=null) {
				PlayerInventory inv = cur.getInventory();
				for (int i = 0; i < inv.size(); i++) {
					ItemStack stack = inv.getStack(i);
					if (Utility.isJokeCard(stack)) {
						inv.removeStack(i);
						break;
					}
				}			} else{
				source.sendMessage(Text.of("Joke isn't on the server, card isn't removed!"));
			}

			getResults(context);
			world.removeAttached(TIME_DATA);
			source.sendMessage(Text.of("Game has ended!"));
		} else{
			source.sendMessage(Text.of("Game isn't started yet!"));
		}


		return 1;
	}

	private static int stopGame(CommandContext<ServerCommandSource> context, boolean paused) {
		ServerCommandSource source = context.getSource();
		MinecraftServer server = source.getServer();
		ServerWorld world = server.getWorld(World.OVERWORLD);
		TimeMap timeMap =  world.getAttachedOrElse(TIME_DATA, null);

		if(timeMap!=null) {
			if(timeMap.isPaused()){
				if(paused){
					source.sendMessage(Text.of("Game is already paused!"));
				} else{
					world.setAttached(TIME_DATA, timeMap.setPaused(false));
					source.sendMessage(Text.of("Game is resumed!"));
				}
			} else{
				if(paused){
					world.setAttached(TIME_DATA, timeMap.setPaused(true));
					source.sendMessage(Text.of("Game is paused!"));
				} else{
					source.sendMessage(Text.of("Game already resumed!"));
				}
			}
		} else{
			source.sendMessage(Text.of("Game isn't started yet!"));
		}

		return 1;
	}

	private static int gameRules(CommandContext<ServerCommandSource> context, boolean broadcast) {
		ServerCommandSource source = context.getSource();
		MinecraftServer server = source.getServer();

		Collection<ServerPlayerEntity> targets;
		if (broadcast) {
			targets = server.getPlayerManager().getPlayerList();
		} else {
			ServerPlayerEntity player = source.getPlayer();
			if (player == null) return 0;
			targets = Collections.singletonList(player);
		}

		for (ServerPlayerEntity player : targets) {
			MutableText message = Text.literal("Hi ")
					.append(player.getDisplayName().copy().formatted(Formatting.GOLD))
					.append(Text.literal("... Here are the rules of the game:").formatted(Formatting.GRAY))
					.append(Text.literal("\n1. One of you has the Joke.").formatted(Formatting.WHITE))
					.append(Text.literal("\n2. As soon as you get the Joke, time starts ticking...").formatted(Formatting.RED))
					.append(Text.literal("\n3. Throw the card to another player to pass it.").formatted(Formatting.WHITE))
					.append(Text.literal("\n   WAIT FOR THE 'JOKE IS NOT ON YOU' TITLE!").formatted(Formatting.RED))
					.append(Text.literal("\n   (Or check manually: '/jokesonyou amIJoke')").formatted(Formatting.RED))
					.append(Text.literal("\n4. Player with the LEAST time holding the Joke wins.").formatted(Formatting.GREEN))
					.append(Text.literal("\n5. Keep 1 inventory slot empty to catch the Joke.").formatted(Formatting.YELLOW))
					.append(Text.literal("\n6. Filling your inventory to block the Joke is CHEATING.").formatted(Formatting.RED))
					.append(Text.literal("\n7. Time DOES NOT STOP if you disconnect or hide the card.").formatted(Formatting.DARK_RED))
					.append(Text.literal("\n   (You are only punishing yourself!)").formatted(Formatting.GRAY).formatted(Formatting.ITALIC))
					.append(Text.literal("\n8. Have fun!").formatted(Formatting.GOLD));

			player.sendMessage(message, false);
		}
		return 1;

	}

	private static int startTheGame(CommandContext<ServerCommandSource> context){
		ServerCommandSource source = context.getSource();
		MinecraftServer server = source.getServer();
		ServerWorld world = server.getWorld(World.OVERWORLD);
		TimeMap timeMap =  world.getAttachedOrElse(TIME_DATA, null);
		if(timeMap == null) {
			TimeMap FtimeMap = world.getAttachedOrCreate(TIME_DATA, TIME_DATA_SUPPLIER);
			List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList();
			Random random = new Random();
			int size = players.size();
			int selected_player = random.nextInt(0, size);

			sendTitle(players, "Who's gonna play today?", Formatting.GREEN, false);
			scheduler.schedule( () -> {
				sendTitle(players, "Hm...", Formatting.GREEN, false);
			}, 5, TimeUnit.SECONDS );
			scheduler.schedule( () -> {
				sendTitle(players, "Hm.....", Formatting.RED, false);
			}, 10, TimeUnit.SECONDS );
			scheduler.schedule( () -> {
				sendTitle(players, "Oh, I know!", Formatting.GREEN, false);
			}, 15, TimeUnit.SECONDS );
			scheduler.schedule( () -> {
				try{
					world.setAttached(TIME_DATA, FtimeMap.update(players.get(selected_player).getGameProfile().getName(), new TimeEntry(players.get(selected_player).getGameProfile().getName(), Instant.now().getEpochSecond())));
					players.get(selected_player).getInventory().insertStack(getJokeCard());
					sendTitle(players.get(selected_player), "JOKE'S ON YOU!!", Formatting.DARK_RED, true);
				} catch(Exception e){
					e.printStackTrace();
				}
				gameRules(context, true);

			}, 17, TimeUnit.SECONDS );


		}else{
			source.sendMessage(Text.of("Game is already running!"));
		}

		return 1;
	}
	private static int amIJoke(CommandContext<ServerCommandSource> context){
		ServerCommandSource source = context.getSource();
		MinecraftServer server = source.getServer();
		ServerWorld world = server.getWorld(World.OVERWORLD);
		TimeMap timeMap = world.getAttachedOrElse(TIME_DATA, null);

		if (timeMap != null) {
			if(!timeMap.isPaused()){
				if(timeMap.current().equals(context.getSource().getPlayer().getName().getLiteralString())) {
					source.sendMessage(Text.of("You are a Joke!"));
				} else{
					source.sendMessage(Text.of("You are not a Joke!"));
				}
			} else{
				if(timeMap.current().equals(context.getSource().getPlayer().getName().getLiteralString())) {
					source.sendMessage(Text.of("You are a Joke! (Game is paused)"));
				} else{
					source.sendMessage(Text.of("You are not a Joke! (Game is paused)"));
				}
			}
		} else{
			source.sendMessage(Text.of("Game isn't started yet!"));
		}


		return 1;
	}

	private static int giveJokeCard(CommandContext<ServerCommandSource> context){
		ServerCommandSource source = context.getSource();
		MinecraftServer server = source.getServer();
		ServerWorld world = server.getWorld(World.OVERWORLD);
		TimeMap timeMap = world.getAttachedOrElse(TIME_DATA, null);
		if(timeMap != null) {
			if(!timeMap.isPaused()){
				if(timeMap.current().equals(context.getSource().getPlayer().getName().getLiteralString())) {
					LOGGER.info(String.valueOf(checkJokeCard(context)));
					if(!checkJokeCard(context)) {
						source.getPlayer().getInventory().insertStack(getJokeCard());
					} else{
						source.sendMessage(Text.of("You already have a Joke!"));
					}
				} else {
					source.sendMessage(Text.of("Joke needs to execute this"));
				}
			} else{
				source.sendMessage(Text.of("Game is paused!"));
			}
		} else{
			source.sendMessage(Text.of("Game isn't started yet!"));
		}


		return 1;
	}

	private static int getData(CommandContext<ServerCommandSource> context){
		ServerCommandSource source = context.getSource();
		MinecraftServer server = source.getServer();
		ServerWorld world = server.getWorld(World.OVERWORLD);
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

			source.sendMessage( Text.literal(v.toString()) );
		} else{
			source.sendMessage( Text.of("Game isn't started yet!"));
		}
		return 1;
	}

	private static int getResults(CommandContext<ServerCommandSource> context){
		try{
			ServerCommandSource source = context.getSource();
			MinecraftServer server = source.getServer();
			ServerWorld world = server.getWorld(World.OVERWORLD);
			TimeMap te = world.getAttachedOrElse(TIME_DATA, null);
			if(te != null) {

				Map<String, Long> players = calculateDurations(te.entries(), te.penalties());
				StringBuilder v = new StringBuilder();
				v.append("Results\n");
				players.forEach((playerName, duration) -> {
					v.append(String.format("Player: %s, Time: %s\n", playerName, convertToTime(duration)));
				});
				source.sendMessage( Text.literal(v.toString()) );
			} else{
				source.sendMessage( Text.of("Game isn't started yet!"));
			}
		} catch(Exception e){
			e.printStackTrace();
		}
		return 1;
	}
}