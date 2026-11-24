package com.kurumaintown.mixin;

import com.jdmmc.kurumamod.car.CarCore;
import com.jdmmc.kurumamod.car.CarHost;
import com.jdmmc.kurumamod.physics.CarSpec;
import com.kurumaintown.TownCarData;
import com.kurumaintown.TownCustomPhysics;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** CarCore.physicsTick calls spec() directly, bypassing CarEntity.getSpec(). */
@Mixin(value = CarCore.class, remap = false)
public abstract class CarCoreCustomMixin {
    @Shadow @Final private CarHost host;

    @Inject(method = "spec", at = @At("RETURN"), cancellable = true, remap = false)
    private void town$applyCustomToPhysics(CallbackInfoReturnable<CarSpec> cir) {
        if (host instanceof TownCarData data) {
            cir.setReturnValue(TownCustomPhysics.apply(cir.getReturnValue(), data));
        }
    }
}
