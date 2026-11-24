package com.kurumaintown.mixin;

import com.jdmmc.kurumamod.entity.CarEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class CarDismountPositionMixin {
    /** Prefer the vehicle's right side (from the driver's perspective) rather than vanilla's position above the vehicle. */
    @Inject(
        method = "getDismountLocationForPassenger(Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/phys/Vec3;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void town$dismountBesideDriver(LivingEntity passenger, CallbackInfoReturnable<Vec3> cir) {
        Entity vehicle = (Entity)(Object)this;
        if (!(vehicle instanceof CarEntity)) return;

        // Minecraft yaw 0 faces +Z. At that orientation, +X is vehicle-left, so negate the lateral vector for vehicle-right.
        double yaw = Math.toRadians(vehicle.getYRot());
        double sideX = -Math.cos(yaw);
        double sideZ = -Math.sin(yaw);
        double offset = vehicle.getBbWidth() * 0.5D + passenger.getBbWidth() * 0.5D + 0.18D;

        Vec3 rightSide = new Vec3(vehicle.getX() + sideX * offset, vehicle.getY(), vehicle.getZ() + sideZ * offset);
        if (town$canDismountAt(vehicle, passenger, rightSide)) {
            cir.setReturnValue(rightSide);
            return;
        }

        // If the vehicle-right side is blocked, try farther out on that side, then try the left side.
        Vec3 fartherRightSide = new Vec3(vehicle.getX() + sideX * (offset + 0.65D), vehicle.getY(), vehicle.getZ() + sideZ * (offset + 0.65D));
        if (town$canDismountAt(vehicle, passenger, fartherRightSide)) {
            cir.setReturnValue(fartherRightSide);
            return;
        }

        Vec3 leftSide = new Vec3(vehicle.getX() - sideX * offset, vehicle.getY(), vehicle.getZ() - sideZ * offset);
        if (town$canDismountAt(vehicle, passenger, leftSide)) {
            cir.setReturnValue(leftSide);
        }
    }

    private static boolean town$canDismountAt(Entity vehicle, LivingEntity passenger, Vec3 position) {
        Vec3 delta = position.subtract(passenger.position());
        AABB movedBox = passenger.getBoundingBox().move(delta);
        return vehicle.level().noCollision(passenger, movedBox);
    }
}
