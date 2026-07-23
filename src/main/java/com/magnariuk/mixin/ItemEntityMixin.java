package com.magnariuk.mixin;

import com.magnariuk.JokesOnYou;
import com.magnariuk.Utility;
import com.magnariuk.abstracts.ProtectedItemAccessor;


import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Random;
import java.util.UUID;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin implements ProtectedItemAccessor {
    @Shadow public abstract ItemStack getItem();
    @Shadow public abstract void setPickUpDelay(int pickupDelay);
    @Shadow private int pickupDelay;
    @Shadow public abstract void makeFakeItem();
    @Shadow private UUID target;

    @Unique private boolean jokesOnYou$isProtected = false;
    @Unique private int jokesOnYou$angerLevel = 0;

    @Override
    public void jokesOnYou$setProtected(boolean value) {
        this.jokesOnYou$isProtected = value;
    }
    @Override
    public boolean jokesOnYou$isProtected() {
        return this.jokesOnYou$isProtected;
    }
    @Override public int jokesOnYou$getAnger() { return this.jokesOnYou$angerLevel; }
    @Override public void jokesOnYou$incrementAnger() { this.jokesOnYou$angerLevel++; }

    @Unique
    private static final List<String> ALERTS = List.of(
            "jokesonyou.alert.full_joke",
            "jokesonyou.alert.cant_fit",
            "jokesonyou.alert.empty_slot",
            "jokesonyou.alert.pockets"
    );

    @Unique
    private static final List<String> TIER_1_MILD = List.of(
            "jokesonyou.tier1.0", "jokesonyou.tier1.1", "jokesonyou.tier1.2", "jokesonyou.tier1.3",
            "jokesonyou.tier1.4", "jokesonyou.tier1.5", "jokesonyou.tier1.6", "jokesonyou.tier1.7",
            "jokesonyou.tier1.8", "jokesonyou.tier1.9", "jokesonyou.tier1.10", "jokesonyou.tier1.11",
            "jokesonyou.tier1.12", "jokesonyou.tier1.13", "jokesonyou.tier1.14", "jokesonyou.tier1.15",
            "jokesonyou.tier1.16", "jokesonyou.tier1.17", "jokesonyou.tier1.18", "jokesonyou.tier1.19",
            "jokesonyou.tier1.20", "jokesonyou.tier1.21", "jokesonyou.tier1.22", "jokesonyou.tier1.23",
            "jokesonyou.tier1.24", "jokesonyou.tier1.25", "jokesonyou.tier1.26", "jokesonyou.tier1.27",
            "jokesonyou.tier1.28", "jokesonyou.tier1.29", "jokesonyou.tier1.30", "jokesonyou.tier1.31"
    );

    @Unique
    private static final List<String> TIER_2_ANNOYED = List.of(
            "jokesonyou.tier2.0", "jokesonyou.tier2.1", "jokesonyou.tier2.2", "jokesonyou.tier2.3",
            "jokesonyou.tier2.4", "jokesonyou.tier2.5", "jokesonyou.tier2.6", "jokesonyou.tier2.7",
            "jokesonyou.tier2.8", "jokesonyou.tier2.9", "jokesonyou.tier2.10", "jokesonyou.tier2.11",
            "jokesonyou.tier2.12", "jokesonyou.tier2.13", "jokesonyou.tier2.14", "jokesonyou.tier2.15",
            "jokesonyou.tier2.16", "jokesonyou.tier2.17", "jokesonyou.tier2.18", "jokesonyou.tier2.19",
            "jokesonyou.tier2.20", "jokesonyou.tier2.21", "jokesonyou.tier2.22", "jokesonyou.tier2.23",
            "jokesonyou.tier2.24", "jokesonyou.tier2.25", "jokesonyou.tier2.26", "jokesonyou.tier2.27",
            "jokesonyou.tier2.28", "jokesonyou.tier2.29", "jokesonyou.tier2.30", "jokesonyou.tier2.31",
            "jokesonyou.tier2.32"
    );

    @Unique
    private static final List<String> TIER_3_FURIOUS = List.of(
            "jokesonyou.tier3.0", "jokesonyou.tier3.1", "jokesonyou.tier3.2", "jokesonyou.tier3.3",
            "jokesonyou.tier3.4", "jokesonyou.tier3.5", "jokesonyou.tier3.6", "jokesonyou.tier3.7",
            "jokesonyou.tier3.8", "jokesonyou.tier3.9", "jokesonyou.tier3.10", "jokesonyou.tier3.11",
            "jokesonyou.tier3.12", "jokesonyou.tier3.13", "jokesonyou.tier3.14", "jokesonyou.tier3.15",
            "jokesonyou.tier3.16", "jokesonyou.tier3.17", "jokesonyou.tier3.18", "jokesonyou.tier3.19",
            "jokesonyou.tier3.20", "jokesonyou.tier3.21", "jokesonyou.tier3.22", "jokesonyou.tier3.23",
            "jokesonyou.tier3.24", "jokesonyou.tier3.25", "jokesonyou.tier3.26", "jokesonyou.tier3.27",
            "jokesonyou.tier3.28", "jokesonyou.tier3.29"
    );
    @Unique private static final int MAX_ANGER = 10;
    @Unique private static final Random RANDOM = new Random();

    @Inject(method = "addAdditionalSaveData",at = @At("TAIL"))
    private void saveData(ValueOutput output, CallbackInfo ci){
        if (this.jokesOnYou$isProtected) output.putBoolean("JokesOnYouProtected", true);
        if (this.jokesOnYou$angerLevel > 0) output.putInt("JokesOnYouAnger", this.jokesOnYou$angerLevel);
    }
    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void loadData(ValueInput input, CallbackInfo ci) {
        if (input.contains("JokesOnYouProtected")) {
            this.jokesOnYou$isProtected = input.getBooleanOr("JokesOnYouProtected", false);
        }
        if (input.contains("JokesOnYouAnger")) {
            this.jokesOnYou$angerLevel = input.getIntOr("JokesOnYouAnger", 0);
        }
    }
    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void jokePickupLogic(Player player, CallbackInfo ci) {
        if (player.level().isClientSide()) return;

        if (this.jokesOnYou$isProtected) {
            if (!player.getUUID().equals(this.target)) {
                ci.cancel();
                return;
            }
        }

        if (this.pickupDelay > 0) return;

        ItemStack jokeStack = this.getItem();

        if (Utility.isJokeCard(jokeStack)) {
            JokesOnYou.PickupResult result = JokesOnYou.validatePickup((ServerPlayer) player);
            switch (result) {
                case GAME_NOT_RUNNING -> {
                    ((ServerPlayer) player).sendSystemMessage(Component.translatable("jokesonyou.game.not_running"), true);
                    this.makeFakeItem();
                    ci.cancel();
                }
                case GAME_PAUSED_RETURN_TO_JOKE -> {
                    ((ServerPlayer) player).sendSystemMessage(Component.translatable("jokesonyou.game.paused"), true);
                    this.makeFakeItem();
                    ci.cancel();
                }
                case YOU_ARE_JOKE -> {
                    return;
                }
                case NOT_JOKE -> {
                }
            }
            boolean hasSpace = player.getInventory().getFreeSlot() != -1 || player.getInventory().contains(jokeStack);

            if (hasSpace && player.getInventory().add(jokeStack)) {
                successLogic(player, ci);
            } else {
                this.jokesOnYou$incrementAnger();
                if (this.jokesOnYou$angerLevel >= MAX_ANGER) {
                    forceSwapLogic(player, jokeStack, ci);
                } else {
                    failLogic(player, ci);
                }
            }

        }
    }

    @Unique
    private void forceSwapLogic(Player player, ItemStack jokeStack, CallbackInfo ci) {
        ItemStack handItem = player.getMainHandItem();

        if (!handItem.isEmpty()) {
            ItemEntity droppedItem = player.drop(handItem.copy(), false, true);

            if (droppedItem != null) {
                droppedItem.setUnlimitedLifetime();
                droppedItem.setTarget(player.getUUID());
                droppedItem.setGlowingTag(true);
                droppedItem.setPickUpDelay(0);
                MutableComponent name = Component.translatable("jokesonyou.confiscated_title").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
                        .append(player.getName().copy().withStyle(ChatFormatting.WHITE));

                droppedItem.setCustomName(name);
                droppedItem.setCustomNameVisible(true);
                ((ProtectedItemAccessor) droppedItem).jokesOnYou$setProtected(true);
            }
        }

        player.setItemInHand(InteractionHand.MAIN_HAND, jokeStack.copy());

        if (player instanceof ServerPlayer serverPlayer) {

            Utility.sendTitle(serverPlayer, Component.translatable("jokesonyou.anger.ill_do_it_myself").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));

            Utility.sendTitle(serverPlayer, Component.translatable("jokesonyou.alert.item_was_dropped").withStyle(ChatFormatting.RED), true);

            String pettyLine = TIER_3_FURIOUS.get(RANDOM.nextInt(TIER_3_FURIOUS.size()));
            serverPlayer.sendSystemMessage(
                    Component.literal(pettyLine).withStyle(ChatFormatting.RED, ChatFormatting.ITALIC),
                    true
            );
        }

        assert player instanceof ServerPlayer;
        JokesOnYou.handleJokePass((ServerPlayer) player, this.jokesOnYou$angerLevel* 60L);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.5f, 1.0f);

        this.makeFakeItem();
        ci.cancel();
    }

    @Unique
    private void successLogic(Player player, CallbackInfo ci) {
        JokesOnYou.handleJokePass((ServerPlayer) player, this.jokesOnYou$angerLevel* 60L);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2f, 2.0f);
        this.makeFakeItem();
        ci.cancel();
    }

    @Unique
    private void failLogic(Player player, CallbackInfo ci) {
        List<String> currentList;
        ChatFormatting color;
        float pitch;

        if (this.jokesOnYou$angerLevel <= 3) {
            currentList = TIER_1_MILD;
            color = ChatFormatting.YELLOW;
            pitch = 1.0f;
        } else if (this.jokesOnYou$angerLevel <= 7) {
            currentList = TIER_2_ANNOYED;
            color = ChatFormatting.GOLD;
            pitch = 1.5f;
        } else {
            currentList = TIER_3_FURIOUS;
            color = ChatFormatting.RED;
            pitch = 2.0f;
        }

        String key = currentList.get(RANDOM.nextInt(currentList.size()));
        String alertKey = ALERTS.get(RANDOM.nextInt(ALERTS.size()));

        String insult = currentList.get(RANDOM.nextInt(currentList.size()));
//
//        if (player instanceof ServerPlayer serverPlayer) {
//            //Utility.sendTitle(serverPlayer, Component.literal("INVENTORY FULL!").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
//            Utility.sendTitle(serverPlayer, "INVENTORY FULL!", ChatFormatting.DARK_RED, true);
//
//            Utility.sendTitle(serverPlayer, Component.literal(ALERTS.get(RANDOM.nextInt(ALERTS.size()))).withStyle(color), true);
//            serverPlayer.sendOverlayMessage(Component.literal(insult).withStyle(color, ChatFormatting.ITALIC));
//        }
        if (player instanceof ServerPlayer serverPlayer) {
            Utility.sendTitle(serverPlayer, Component.translatable("jokesonyou.title.inventory_full").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
            Utility.sendTitle(serverPlayer, Component.translatable(alertKey).withStyle(color), true);
            serverPlayer.sendOverlayMessage(Component.translatable(key).withStyle(color, ChatFormatting.ITALIC));
        }

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.NOTE_BLOCK_PLING, SoundSource.PLAYERS, 10.0f, pitch);

        this.setPickUpDelay(60);
        ci.cancel();
    }

}
