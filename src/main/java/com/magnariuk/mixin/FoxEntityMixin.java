package com.magnariuk.mixin;

import com.magnariuk.JokesOnYou;
import com.magnariuk.Utility;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.passive.FoxEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(FoxEntity.class)
public abstract class FoxEntityMixin {

    @Inject(method = "canPickupItem", at = @At("HEAD"), cancellable = true)
    private void denyJokePickup(ItemStack itemStack, CallbackInfoReturnable<Boolean> cir) {
        if(ItemStack.areEqual(itemStack, Utility.getJokeCard())) {
            cir.setReturnValue(false);
        }
    }
}
