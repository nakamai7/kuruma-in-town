package com.kurumaintown.mixin;

import com.jdmmc.kurumamod.physics.CarInput;
import com.jdmmc.kurumamod.physics.CarSpec;
import com.jdmmc.kurumamod.physics.CarState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Prevent automatic transmission from engaging a forward gear while coasting backwards. */
@Mixin(value = com.jdmmc.kurumamod.physics.CarPhysics.class, remap = false)
public abstract class CarPhysicsReverseMixin {
    @Inject(method = "selectGear(Lcom/jdmmc/kurumamod/physics/CarSpec;Lcom/jdmmc/kurumamod/physics/CarState;DLcom/jdmmc/kurumamod/physics/CarInput;)I", at = @At("HEAD"), cancellable = true, remap = false)
    private static void town$holdReverseWhileCoasting(CarSpec spec, CarState state, double dt, CarInput input, CallbackInfoReturnable<Integer> cir) {
        if (!spec.isManual() && state.gear == -1 && state.forwardSpeed < -0.3D && !input.throttle() && !input.brake()) {
            cir.setReturnValue(-1);
        }
    }
}
