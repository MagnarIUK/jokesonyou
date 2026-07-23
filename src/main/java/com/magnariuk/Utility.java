package com.magnariuk;

import com.magnariuk.records.PenaltyEntry;
import com.magnariuk.records.TimeEntry;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;


import java.time.Instant;
import java.util.*;

public class Utility {

    public static boolean checkJokeCard(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return context.getSource().getPlayerOrException().getInventory().contains(getJokeCard());
    }

    public static ItemStack getJokeCard() {
        ItemStack jokeCard = new ItemStack(Items.PAPER, 1);
        jokeCard.set(DataComponents.ITEM_NAME, Component.translatable("jokesonyou.name").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        jokeCard.set(DataComponents.RARITY, Rarity.EPIC);
        jokeCard.set(DataComponents.MAX_STACK_SIZE, 1);

        List<Component> lore = new ArrayList<>();
        lore.add(Component.translatable("jokesonyou.item.lore.0"));
        lore.add(Component.translatable("jokesonyou.item.lore.1"));
        lore.add(Component.translatable("jokesonyou.item.lore.2"));
        lore.add(Component.translatable("jokesonyou.item.lore.3"));
        lore.add(Component.translatable("jokesonyou.item.lore.4"));
        jokeCard.set(DataComponents.LORE, new ItemLore(lore));

        CompoundTag tag = new CompoundTag();
        tag.putBoolean("joker", true);
        jokeCard.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

        return jokeCard;
    }

    private static final CompoundTag JOKER_TEMPLATE = new CompoundTag();
    static {
        JOKER_TEMPLATE.putBoolean("joker", true);
    }

    public static boolean isJokeCard(ItemStack stack) {
        if (stack.isEmpty()) return false;

        var customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData != null && customData.matchedBy(JOKER_TEMPLATE);
    }

    static String convertToTime(Long time){
        long hours = time / 3600;
        long minutes = (time % 3600) / 60;
        long seconds = time % 60;

        return String.format("%d:%02d:%02d", hours, minutes, seconds);

    }

    public static void sendTitle(Collection<ServerPlayer> targets, Component component, boolean isSubtitle) {
        JokesOnYou.LOGGER.debug("sending title: {}", component.getString());
        if (targets == null || targets.isEmpty()) return;

        if (isSubtitle) {
            ClientboundSetSubtitleTextPacket packet = new ClientboundSetSubtitleTextPacket(component);
            for (ServerPlayer player : targets) {
                if (player != null) player.connection.send(packet);
            }
        } else {
            ClientboundSetTitleTextPacket packet = new ClientboundSetTitleTextPacket(component);
            for (ServerPlayer player : targets) {
                if (player != null) player.connection.send(packet);
            }
        }
    }

    public static void sendTitle(ServerPlayer player, Component component, boolean isSubtitle) {
        if (player == null) return;
        //System.out.println(component.getString());
        sendTitle(List.of(player), component, isSubtitle);
    }
    public static void sendTitle(ServerPlayer player, Component component) {
        sendTitle(player, component, false);
    }
    public static void sendTitle(Collection<ServerPlayer> targets, Component component) {
        sendTitle(targets, component, false);
    }

    public static void sendTitle(Collection<ServerPlayer> targets, String message, ChatFormatting color, boolean isBold, boolean isSubtitle) {
        Component titleText = Component.literal(message).withStyle(style ->
                isBold ? style.withColor(color).withBold(true) : style.withColor(color)
        );
        sendTitle(targets, titleText, isSubtitle);
    }

    public static void sendTitle(ServerPlayer player, String message, ChatFormatting color, boolean isBold, boolean isSubtitle) {
        if (player == null) return;
        Component titleText = Component.literal(message).withStyle(style ->
                isBold ? style.withColor(color).withBold(true) : style.withColor(color)
        );
        sendTitle(List.of(player), titleText, isSubtitle);
    }

    public static void sendTitle(Collection<ServerPlayer> targets, String message, ChatFormatting color, boolean isBold) {
        sendTitle(targets, message, color, isBold, false);
    }
    public static void sendTitle(ServerPlayer player, String message, ChatFormatting color, boolean isBold) {
        sendTitle(player, message, color, isBold, false);
    }


    public static Map<String, Long> calculateDurations(List<TimeEntry> entries, List<PenaltyEntry> penalties) {
        Map<String, Long> durations = new HashMap<>();

        if (!entries.isEmpty()) {
            for (int i = 0; i < entries.size() - 1; i++) {
                TimeEntry entry = entries.get(i);
                TimeEntry next = entries.get(i + 1);
                long duration = next.time() - entry.time();
                durations.merge(entry.player(), duration, Long::sum);
            }

            TimeEntry last = entries.get(entries.size() - 1);
            long currentDuration = Instant.now().getEpochSecond() - last.time();
            durations.merge(last.player(), currentDuration, Long::sum);
        }

        if (penalties != null) {
            for (PenaltyEntry penalty : penalties) {
                durations.merge(penalty.player(), penalty.time(), Long::sum);
            }
        }

        return durations;
    }



}
