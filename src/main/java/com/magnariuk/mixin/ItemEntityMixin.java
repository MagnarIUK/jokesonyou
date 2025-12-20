package com.magnariuk.mixin;

import com.magnariuk.JokesOnYou;
import com.magnariuk.Utility;
import com.magnariuk.abstracts.ProtectedItemAccessor;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
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
    @Shadow public abstract ItemStack getStack();
    @Shadow public abstract void setPickupDelay(int pickupDelay);
    @Shadow private int pickupDelay;
    @Shadow public abstract void setDespawnImmediately();
    @Shadow private UUID owner;

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
            "You're supposed to be the Joke, but you're FULL! >:(",
            "INVENTORY FULL! The Joke can't fit! >:(",
            "Nice try, but you need an empty slot! >:(",
            "Hey! You can't hide behind full pockets! >:("
    );
    @Unique
    private static final List<String> TIER_1_MILD = List.of(
            "Be a dear and clear a slot, won't you?",
            "Hello, it is quite important to take that joke over there",
            "Look buddy, your inventory- I mean inbox is full.",
            "Por favor, recoja el trozo de papel.",
            "You just got a letter~",
            "The 'Q' key. It is next to 'W'. Please use it.",
            "Do you need a tutorial on how to drop items?",
            "Have you tried turning your greed off and on again?",
            "Oh, take your time. The whole server loves waiting.",
            "Space is finite, unlike your stubbornness.",
            "I see a lot of Dirt. Do you really need that?",
            "Just one slot. That's all I ask.",
            "Aww, your pockets are full. How unfortunate.",
            "Did you forget how to throw things away?",
            "Wee woo, joke inbound.",
            "Hey, you dropped something.",
            "Be a good protagonist and advance the plot.",
            "Behold! Your tax documents! Pick them up please."
    );

    @Unique
    private static final List<String> TIER_2_ANNOYED = List.of(
            "Don't be difficult. Pick. It. Up.",
            "HEY YOU! You dropped something. Yeah, that paper.",
            "You now have FOMO. Cure it with that joke on the floor.",
            "Your inventory management skills are tragic.",
            "Inventory Tetris won't save you from me.",
            "Hoarders: Minecraft Edition is looking great.",
            "Nobody loves cobblestone that much. Let it go.",
            "Are you running a pawn shop? CLEAR A SLOT.",
            "One man's trash is... well, apparently your whole inventory.",
            "You will die in about 30s. Stop it by picking up the paper.",
            "You hoard items like you hoard bad decisions.",
            "Nice exploit. Did you find that on Reddit?",
            "Error 404: Empty Slot Not Found. User is hopeless.",
            "You're not slick, you're just messy.",
            "I'm about to report you for 'Being Annoying'.",
            "Imagine being afraid of a piece of paper.",
            "Wow. So much junk. So little skill.",
            "Idiotic creature, cling to that paper with your life."
    );

    @Unique
    private static final List<String> TIER_3_FURIOUS = List.of(
            "Stop stalling and accept your fate.",
            "Drop your trash and pick up the special joke!",
            "JUST TAKE THE FU**ING THING!",
            "Make room, or I will make room FOR you.",
            "I'm giving you three seconds to stop being annoying.",
            "Every second you wait, I get angrier.",
            "I AM NOT ASKING ANYMORE.",
            "DROP IT OR I DROP YOU.",
            "YOUR ITEMS ARE FORFEIT.",
            "RESISTANCE IS FUTILE.",
            "I AM IN YOUR WALLS (AND SOON YOUR INVENTORY).",
            "DO NOT TEST ME, MORTAL.",
            "TICK TOCK. TICK TOCK.",
            "TAKE THE LETTER, THE GODS DEMAND IT",
            "You little-shit, wipe yourself with that paper on the floor."
    );
    @Unique private static final int MAX_ANGER = 10;
    @Unique private static final Random RANDOM = new Random();

    @Inject(method = "writeCustomDataToNbt",at = @At("TAIL"))
    private void saveData(NbtCompound nbtCompound, CallbackInfo ci){
        if (this.jokesOnYou$isProtected) nbtCompound.putBoolean("JokesOnYouProtected", true);
        if (this.jokesOnYou$angerLevel > 0) nbtCompound.putInt("JokesOnYouAnger", this.jokesOnYou$angerLevel);
    }
    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void loadData(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains("JokesOnYouProtected")) this.jokesOnYou$isProtected = nbt.getBoolean("JokesOnYouProtected");
        if (nbt.contains("JokesOnYouAnger")) this.jokesOnYou$angerLevel = nbt.getInt("JokesOnYouAnger");
    }

    @Inject(method = "onPlayerCollision", at = @At("HEAD"), cancellable = true)
    private void jokePickupLogic(PlayerEntity player, CallbackInfo ci) {
        if (player.getWorld().isClient) return;

        if (this.jokesOnYou$isProtected) {
            if (!player.getUuid().equals(this.owner)) {
                ci.cancel();
                return;
            }
        }

        if (this.pickupDelay > 0) return;

        ItemStack jokeStack = this.getStack();

        if (Utility.isJokeCard(jokeStack)) {
            JokesOnYou.PickupResult result = JokesOnYou.validatePickup((ServerPlayerEntity) player);
            switch (result) {
                case GAME_NOT_RUNNING -> {
                    player.sendMessage(Text.of("Game is not running."), true);
                    this.setDespawnImmediately();
                    ci.cancel();
                }
                case GAME_PAUSED_RETURN_TO_JOKE -> {
                    player.sendMessage(Text.of("Game is paused!"), true);
                    this.setDespawnImmediately();
                    ci.cancel();
                }
                case YOU_ARE_JOKE -> {
                    return;
                }
                case NOT_JOKE -> {
                    break;
                }
            }
            boolean success = player.getInventory().insertStack(jokeStack);

            if (success) {
                successLogic(player, ci);
            } else {
                this.jokesOnYou$incrementAnger();
                if(this.jokesOnYou$angerLevel >= MAX_ANGER){
                    forceSwapLogic(player, jokeStack, ci);
                } else {
                    failLogic(player, ci);
                }
            }

        }
    }

    @Unique
    private void forceSwapLogic(PlayerEntity player, ItemStack jokeStack, CallbackInfo ci) {
        ItemStack handItem = player.getMainHandStack();

        if (!handItem.isEmpty()) {
            ItemEntity droppedItem = player.dropItem(handItem.copy(), false, true);

            if (droppedItem != null) {
                droppedItem.setNeverDespawn();
                droppedItem.setOwner(player.getUuid());
                droppedItem.setGlowing(true);
                droppedItem.setPickupDelay(0);
                MutableText name = Text.literal("CONFISCATED: ").formatted(Formatting.RED, Formatting.BOLD)
                        .append(player.getName().copy().formatted(Formatting.WHITE));

                droppedItem.setCustomName(name);
                droppedItem.setCustomNameVisible(true);
                ((ProtectedItemAccessor) droppedItem).jokesOnYou$setProtected(true);
            }
        }

        player.setStackInHand(Hand.MAIN_HAND, jokeStack.copy());

        if (player instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.networkHandler.sendPacket(new TitleS2CPacket(
                    Text.literal("FINE, I'LL DO IT MYSELF!").formatted(Formatting.DARK_PURPLE, Formatting.BOLD)
            ));

            serverPlayer.networkHandler.sendPacket(new SubtitleS2CPacket(
                    Text.literal("(Your item was dropped)").formatted(Formatting.RED)
            ));

            String pettyLine = TIER_3_FURIOUS.get(RANDOM.nextInt(TIER_3_FURIOUS.size()));
            serverPlayer.sendMessage(
                    Text.literal(pettyLine).formatted(Formatting.RED, Formatting.ITALIC),
                    true
            );
        }

        assert player instanceof ServerPlayerEntity;
        JokesOnYou.handleJokePass((ServerPlayerEntity) player);
        player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.5f, 1.0f);

        this.setDespawnImmediately();
        ci.cancel();
    }

    @Unique
    private void successLogic(PlayerEntity player, CallbackInfo ci) {
        JokesOnYou.handleJokePass((ServerPlayerEntity) player);
        player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 0.2f, 2.0f);
        this.setDespawnImmediately();
        ci.cancel();
    }

    @Unique
    private void failLogic(PlayerEntity player, CallbackInfo ci) {
        List<String> currentList;
        Formatting color;
        float pitch;

        if (this.jokesOnYou$angerLevel <= 3) {
            currentList = TIER_1_MILD;
            color = Formatting.YELLOW;
            pitch = 1.0f;
        } else if (this.jokesOnYou$angerLevel <= 7) {
            currentList = TIER_2_ANNOYED;
            color = Formatting.GOLD;
            pitch = 1.5f;
        } else {
            currentList = TIER_3_FURIOUS;
            color = Formatting.RED;
            pitch = 2.0f;
        }

        String insult = currentList.get(RANDOM.nextInt(currentList.size()));

        if (player instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.networkHandler.sendPacket(new TitleS2CPacket(
                    Text.literal("INVENTORY FULL!").formatted(Formatting.DARK_RED, Formatting.BOLD)
            ));

            serverPlayer.networkHandler.sendPacket(new SubtitleS2CPacket(
                    Text.literal(ALERTS.get(RANDOM.nextInt(ALERTS.size()))).formatted(color)
            ));

            serverPlayer.sendMessage(
                    Text.literal(insult).formatted(color, Formatting.ITALIC),
                    true
            );
        }

        player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLOCK_NOTE_BLOCK_PLING, SoundCategory.PLAYERS, 1.0f, pitch);

        this.setPickupDelay(60);
        ci.cancel();
    }

}
