package com.magnariuk.mixin;

import com.magnariuk.JokesOnYou;
import com.magnariuk.Utility;
import com.magnariuk.records.TimeMap;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.magnariuk.JokesOnYou.TIME_DATA;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {

    private void discardDrop(CallbackInfoReturnable<ItemEntity> cir){
        this.discardDrop(cir, false);
    }

    private void discardDrop(CallbackInfoReturnable<ItemEntity> cir, boolean giveItemBack){
        cir.cancel();
        if (giveItemBack) {
            ((PlayerEntity)(Object)this).getInventory().insertStack(Utility.getJokeCard());
        }
    }

    @Inject(method = "dropItem(Lnet/minecraft/item/ItemStack;ZZ)Lnet/minecraft/entity/ItemEntity;", at = @At("HEAD"), cancellable = true)
    private void CancelDroppingJoke(ItemStack itemStack, boolean bl, boolean bl2, CallbackInfoReturnable<ItemEntity> cir){
        if(ItemStack.areEqual(itemStack, Utility.getJokeCard())){
            MinecraftServer server = ((PlayerEntity)(Object)this).getServer();
            ServerWorld world = server.getWorld(World.OVERWORLD);
            TimeMap timeMap = world.getAttached(TIME_DATA);
            if(timeMap != null){
                if(timeMap.isPaused()){
                    discardDrop(cir, true);
                }
            } else{
                discardDrop(cir);
            }
        }
    }
}
