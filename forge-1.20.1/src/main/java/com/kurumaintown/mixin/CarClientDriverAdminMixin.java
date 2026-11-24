package com.kurumaintown.mixin;

import com.jdmmc.kurumamod.client.KurumaKeys;
import com.jdmmc.kurumamod.client.input.CarDriver;
import com.kurumaintown.TownNetwork;
import com.kurumaintown.network.TuningAccessPacket;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Kurumamod 2.1.0 moved tuning dispatch into core CarDriver.
 * Intercept the Forge Host's click consumption so core does not open
 * its own tuning screen before the server verifies the admin tag.
 */
@Mixin(targets = "com.jdmmc.kurumamod.client.CarClientDriver$Host", remap = false)
public abstract class CarClientDriverAdminMixin {
    @Inject(method = "clicks(Lcom/jdmmc/kurumamod/client/input/CarDriver$Press;)I", at = @At("HEAD"), cancellable = true, remap = false)
    private void town$adminTuning(CarDriver.Press key, CallbackInfoReturnable<Integer> cir) {
        if (key != CarDriver.Press.OPEN_TUNING) return;
        boolean pressed = false;
        while (KurumaKeys.OPEN_TUNING.consumeClick()) pressed = true;
        Minecraft mc = Minecraft.getInstance();
        if (pressed && mc.screen == null && mc.player != null)
            TownNetwork.CHANNEL.sendToServer(new TuningAccessPacket());
        // Prevent the upstream screen from opening without server authorization.
        cir.setReturnValue(0);
    }
}
