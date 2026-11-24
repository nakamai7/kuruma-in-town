package com.kurumaintown.client;

import com.kurumaintown.network.VehicleToolPacket;
import net.minecraft.client.Minecraft;

public final class VehicleToolClientHandler {
    private VehicleToolClientHandler(){}
    public static void open(VehicleToolPacket p){Minecraft.getInstance().setScreen(new VehicleToolScreen(p));}
}
