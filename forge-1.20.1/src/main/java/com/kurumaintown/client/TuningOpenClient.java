package com.kurumaintown.client;

import com.jdmmc.kurumamod.client.CarTuning;
import com.jdmmc.kurumamod.client.screen.CarTuningScreen;
import com.jdmmc.kurumamod.car.CarTypes;
import com.jdmmc.kurumamod.entity.CarEntity;
import net.minecraft.client.Minecraft;
import com.jdmmc.kurumamod.platform.McUiScreen;

public final class TuningOpenClient {
    private TuningOpenClient() { }
    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || !(mc.player.getVehicle() instanceof CarEntity car)) return;
        if (car.getControllingPassenger() != mc.player && CarTuning.spec() != car.getSpec())
            CarTuning.adopt(car.getSpec(), CarTypes.get(car.getCarId()).spec());
        mc.setScreen(new McUiScreen(new CarTuningScreen()));
    }
}
