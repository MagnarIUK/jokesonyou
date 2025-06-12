package com.magnariuk;

import com.magnariuk.abstracts.PlayerPickupItemCallback;
import com.magnariuk.records.TimeEntry;
import com.magnariuk.records.TimeMap;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;


import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

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
	public static final Supplier<TimeMap> TIME_DATA_SUPPLIER = ()  -> new TimeMap("none",  false, new ArrayList<>());



    @Override
	public void onInitialize() {
		PlayerPickupItemCallback.EVENT.register((inventory, slot, stack) -> {
			if(ItemStack.areEqual(stack, getJokeCard())) {
				MinecraftServer server = inventory.player.getServer();
				if (server != null) {
					ServerWorld world = server.getWorld(World.OVERWORLD);
					TimeMap timeMap = world.getAttachedOrCreate(TIME_DATA, TIME_DATA_SUPPLIER);

					if (timeMap != null) {
						if (timeMap.isPaused()) {
							inventory.player.getCommandSource().sendMessage(Text.of("Game is paused. Please, return The Joke to " + timeMap.current()));
						} else {
							if (!timeMap.current().equals(inventory.player.getGameProfile().getName())) {

								world.setAttached(TIME_DATA, timeMap.update(inventory.player.getGameProfile().getName(), new TimeEntry(inventory.player.getGameProfile().getName(), Instant.now().getEpochSecond())));
								sendTitle(server.getPlayerManager().getPlayer(inventory.player.getGameProfile().getId()), "Joke's on you!", Formatting.RED, false);
							}
						}
					} else {
						inventory.player.getCommandSource().sendMessage(Text.of("Game is not running."));
					}

				} else {
					LOGGER.warn("Server is null");
				}
			}
			return ActionResult.PASS;
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(CommandManager.literal("jokesonyou")
					.then(CommandManager.literal("startTheGame").executes(JokesOnYou::startTheGame)
							.requires(source -> source.hasPermissionLevel(2)))
					.then(CommandManager.literal("getData").executes(JokesOnYou::getData)
							.requires(source -> source.hasPermissionLevel(1)))
					.then(CommandManager.literal("getResults").executes(JokesOnYou::getResults)
							.requires(source -> source.hasPermissionLevel(1)))
					.then(CommandManager.literal("giveJokeCard").executes(JokesOnYou::giveJokeCard))
					.then(CommandManager.literal("amIJoke").executes(JokesOnYou::amIJoke))
					.then(CommandManager.literal("stopTheGame").then(argument("stop", BoolArgumentType.bool())
							.executes(context -> JokesOnYou.stopGame(context, BoolArgumentType.getBool(context, "stop")))))
							.requires(source -> source.hasPermissionLevel(2))
					.then(CommandManager.literal("remove").requires(source -> source.hasPermissionLevel(2))
							.executes(JokesOnYou::removeJoke)));
				});


		LOGGER.info("Jokes on you initialized!");
	}



	private static int removeJoke(CommandContext<ServerCommandSource> context){
		ServerCommandSource source = context.getSource();
		MinecraftServer server = source.getServer();
		ServerWorld world = server.getWorld(World.OVERWORLD);
		TimeMap tm = world.getAttachedOrElse(TIME_DATA, null);
		if(tm!=null) {
			PlayerEntity cur =  server.getPlayerManager().getPlayer(tm.current());
			if(cur!=null) {
				cur.getInventory().removeStack(cur.getInventory().getSlotWithStack(getJokeCard()));
			} else{
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
			for (TimeEntry timeEntry : te.entries()) {
				v.append(String.format("Player: %s, Time: %d\n", timeEntry.player(), timeEntry.time()));
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

				Map<String, Long> players = calculateDurations(te.entries());
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