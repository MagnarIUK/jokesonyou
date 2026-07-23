package com.magnariuk.mixin;

import com.magnariuk.Utility;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(Fox.class)
public abstract class FoxEntityMixin {

    @Inject(method = "canHoldItem", at = @At("HEAD"), cancellable = true)
    private void denyJokePickup(ItemStack itemStack, CallbackInfoReturnable<Boolean> cir) {
        if(Utility.isJokeCard(itemStack)) {
            cir.setReturnValue(false);
        }
    }
}
