package com.kurumaintown.mixin;

import com.jdmmc.kurumamod.network.CarPacketHandlers;
import com.jdmmc.kurumamod.network.CarTuningPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.function.Supplier;

/** Prevent unauthorized tuning packets before the base mod queues their application. */
@Mixin(value = CarPacketHandlers.class, remap = false)
public abstract class CarTuningAdminMixin {
    @Inject(method = "handleCarTuning", at = @At("HEAD"), cancellable = true)
    private static void town$restrictTuning(CarTuningPacket packet,
                                             Supplier<NetworkEvent.Context> supplier,
                                             CallbackInfo ci) {
        NetworkEvent.Context ctx = supplier.get();
        ServerPlayer player = ctx.getSender();
        if (player == null || !player.getTags().contains("admin")) {
            ctx.setPacketHandled(true);
            ci.cancel();
        }
    }
}
