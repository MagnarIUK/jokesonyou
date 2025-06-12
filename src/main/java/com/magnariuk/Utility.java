package com.magnariuk;

import com.magnariuk.records.TimeEntry;
import com.magnariuk.records.TimeMap;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;

import static com.magnariuk.records.TimeEntry.TIME_ENTRY_CODEC;

public class Utility {

    public static boolean checkJokeCard(CommandContext<ServerCommandSource> context) {
        for (ItemStack itemStack: context.getSource().getPlayer().getInventory().main){
            if(ItemStack.areItemsEqual(itemStack, getJokeCard())){
                return true;
            }
        }
        for (ItemStack itemStack: context.getSource().getPlayer().getInventory().offHand){
            if(ItemStack.areItemsEqual(itemStack, getJokeCard())){
                return true;
            }
        }
        for (ItemStack itemStack: context.getSource().getPlayer().getInventory().armor){
            if(ItemStack.areItemsEqual(itemStack, getJokeCard())){
                return true;
            }
        }

        return false;
    }

    public static ItemStack getJokeCard() {
        ItemStack jokeCard = new ItemStack(Items.PAPER, 1);
        jokeCard.set(DataComponentTypes.ITEM_NAME, Text.literal("Joke's On You!").formatted(Formatting.RED, Formatting.BOLD));
        jokeCard.set(DataComponentTypes.RARITY, Rarity.EPIC);
        jokeCard.set(DataComponentTypes.MAX_STACK_SIZE, 1);
        jokeCard.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(69));
        List<Text> lore = new ArrayList<>();
        lore.add(Text.literal("All the Jokes are on you!"));
        lore.add(Text.literal("To put them on someone else"));
        lore.add(Text.literal("Try to Drop this into someone else's inventory"));
        lore.add(Text.literal("Players who had the Joke on them the least, will win"));
        jokeCard.set(DataComponentTypes.LORE, new LoreComponent(lore));

        NbtCompound tag = new NbtCompound();
        tag.putBoolean("joker", true);
        jokeCard.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(tag));

        return jokeCard;
    }
    static String convertToTime(Long time){
        long hours = time / 3600;
        long minutes = (time % 3600) / 60;
        long seconds = time % 60;

        return String.format("%d:%02d:%02d", hours, minutes, seconds);

    }


    public static void sendTitle(ServerPlayerEntity player, String message, Formatting color, boolean isBold) {
        Text titleText;
        if (isBold) {
            titleText= Text.literal(message).styled(style -> style.withColor(color).withBold(isBold));
        } else{
            titleText = Text.literal(message).styled(style -> style.withColor(color));
        }
        TitleS2CPacket titlePacket = new TitleS2CPacket(titleText);
        player.networkHandler.sendPacket(titlePacket);

    }
    public static void sendTitle(Collection<ServerPlayerEntity> targets, String message, Formatting color, boolean isBold) {
        Text titleText;
        if (isBold) {
            titleText= Text.literal(message).styled(style -> style.withColor(color).withBold(isBold));
        } else{
            titleText = Text.literal(message).styled(style -> style.withColor(color));
        }
        for (ServerPlayerEntity player : targets) {
            TitleS2CPacket titlePacket = new TitleS2CPacket(titleText);
            player.networkHandler.sendPacket(titlePacket);
        }
    }


    public static Map<String, Long> calculateDurations(List<TimeEntry> entries) {
        Map<String, Long> durations = new HashMap<>();
        if(entries.isEmpty()) return durations;

        for (int i = 0; i < entries.size()-1; i++) {
            TimeEntry entry = entries.get(i);
            TimeEntry next = entries.get(i + 1);
            long duration = next.time() - entry.time();
            durations.merge(entry.player(), duration, Long::sum);
        }
        TimeEntry last = entries.get(entries.size() - 1);
        long duration = Instant.now().getEpochSecond() - last.time();
        durations.merge(last.player(), duration, Long::sum);

        return  durations;
    }



}
