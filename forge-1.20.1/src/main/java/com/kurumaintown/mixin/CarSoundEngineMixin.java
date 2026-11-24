package com.kurumaintown.mixin;

import com.jdmmc.kurumamod.client.CarSound;
import com.jdmmc.kurumamod.client.sound.CarSoundVoice;
import com.jdmmc.kurumamod.entity.CarEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Silence engine/exhaust loops until a driver has finished starting the engine. */
@Mixin(value = CarSound.class, remap = false)
public abstract class CarSoundEngineMixin {
    @Shadow @Final private CarEntity car;
    @Shadow @Final private CarSoundVoice voice;

    @Redirect(method = "m_7788_", at = @At(value = "INVOKE", target = "Lcom/jdmmc/kurumamod/client/sound/CarSoundVoice;volume()F", remap = false), remap = false)
    private float town$muteEngineWhenOff(CarSoundVoice sound) {
        if (voice.role().isEngine() || voice.role().name().startsWith("EXHAUST")) {
            if (!(car.getControllingPassenger() instanceof Player) ||
                    !((com.kurumaintown.TownEngineState)car).town$isEngineRunning()) return 0.0F;
        }
        return sound.volume();
    }
}
