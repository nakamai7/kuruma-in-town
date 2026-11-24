package com.kurumaintown.client;
import net.minecraft.client.Minecraft;
public final class GaragePlacementClient {
    private GaragePlacementClient() {}
    public static void open() { Minecraft.getInstance().setScreen(new GaragePlacementScreen(GarageMarkerParticles.nearest(Minecraft.getInstance().player.getX(),Minecraft.getInstance().player.getY(),Minecraft.getInstance().player.getZ()))); }
}
