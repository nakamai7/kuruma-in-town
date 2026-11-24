package com.kurumaintown.client;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
public final class GarageAdminClient{
    private GarageAdminClient(){}
    public static void open(CompoundTag players,String selected){Minecraft.getInstance().setScreen("__world_carlist__".equals(selected) ? new WorldCarListScreen(players) : new GarageAdminScreen(players,selected));}
}
