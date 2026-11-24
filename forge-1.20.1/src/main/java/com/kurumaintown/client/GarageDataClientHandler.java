package com.kurumaintown.client;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

/** Client-only endpoint for opening the garage HUD from a server packet. */
public final class GarageDataClientHandler {
    private GarageDataClientHandler() {}
    public static void open(CompoundTag carlist, double x, double y, double z, float yaw, float pitch) {
        Minecraft.getInstance().setScreen(new GarageScreen(carlist, x, y, z, yaw, pitch));
    }
}
