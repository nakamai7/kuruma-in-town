package com.kurumaintown;

import com.kurumaintown.network.GarageDataPacket;
import com.kurumaintown.network.GarageOpenPacket;
import com.kurumaintown.network.GarageRidePacket;
import com.kurumaintown.network.GarageMarkerSyncPacket;
import com.kurumaintown.network.TownConditionPacket;
import com.kurumaintown.network.VehicleToolPacket;
import com.kurumaintown.network.VehicleToolActionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class TownNetwork {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("kuruma_in_town", "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
    private static int id;
    private TownNetwork() {}
    public static void register() {
        CHANNEL.messageBuilder(com.kurumaintown.network.WorldCarDeletePacket.class,id++,net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
            .encoder(com.kurumaintown.network.WorldCarDeletePacket::encode).decoder(com.kurumaintown.network.WorldCarDeletePacket::new).consumerMainThread(com.kurumaintown.network.WorldCarDeletePacket::handle).add();
        CHANNEL.messageBuilder(com.kurumaintown.network.GarageAdminRequestPacket.class,id++,net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
            .encoder(com.kurumaintown.network.GarageAdminRequestPacket::encode).decoder(com.kurumaintown.network.GarageAdminRequestPacket::new).consumerMainThread(com.kurumaintown.network.GarageAdminRequestPacket::handle).add();
        CHANNEL.messageBuilder(com.kurumaintown.network.GarageAdminActionPacket.class,id++,net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
            .encoder(com.kurumaintown.network.GarageAdminActionPacket::encode).decoder(com.kurumaintown.network.GarageAdminActionPacket::new).consumerMainThread(com.kurumaintown.network.GarageAdminActionPacket::handle).add();
        CHANNEL.messageBuilder(com.kurumaintown.network.GarageAdminDataPacket.class,id++,net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
            .encoder(com.kurumaintown.network.GarageAdminDataPacket::encode).decoder(com.kurumaintown.network.GarageAdminDataPacket::new).consumerMainThread(com.kurumaintown.network.GarageAdminDataPacket::handle).add();
        CHANNEL.messageBuilder(com.kurumaintown.network.GarageDeletePacket.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                .encoder(com.kurumaintown.network.GarageDeletePacket::encode).decoder(com.kurumaintown.network.GarageDeletePacket::new).consumerMainThread(com.kurumaintown.network.GarageDeletePacket::handle).add();
        CHANNEL.messageBuilder(com.kurumaintown.network.GarageRotatePacket.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                .encoder(com.kurumaintown.network.GarageRotatePacket::encode).decoder(com.kurumaintown.network.GarageRotatePacket::new).consumerMainThread(com.kurumaintown.network.GarageRotatePacket::handle).add();
        CHANNEL.messageBuilder(com.kurumaintown.network.GaragePlacePacket.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                .encoder(com.kurumaintown.network.GaragePlacePacket::encode).decoder(com.kurumaintown.network.GaragePlacePacket::new).consumerMainThread(com.kurumaintown.network.GaragePlacePacket::handle).add();
        CHANNEL.messageBuilder(GarageOpenPacket.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                .encoder(GarageOpenPacket::encode).decoder(GarageOpenPacket::new).consumerMainThread(GarageOpenPacket::handle).add();
        CHANNEL.messageBuilder(GarageDataPacket.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                .encoder(GarageDataPacket::encode).decoder(GarageDataPacket::new).consumerMainThread(GarageDataPacket::handle).add();
        CHANNEL.messageBuilder(GarageRidePacket.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                .encoder(GarageRidePacket::encode).decoder(GarageRidePacket::new).consumerMainThread(GarageRidePacket::handle).add();
        CHANNEL.messageBuilder(TownConditionPacket.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                .encoder(TownConditionPacket::encode).decoder(TownConditionPacket::new).consumerMainThread(TownConditionPacket::handle).add();
        CHANNEL.messageBuilder(GarageMarkerSyncPacket.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                .encoder(GarageMarkerSyncPacket::encode).decoder(GarageMarkerSyncPacket::new).consumerMainThread(GarageMarkerSyncPacket::handle).add();
        CHANNEL.messageBuilder(VehicleToolPacket.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                .encoder(VehicleToolPacket::encode).decoder(VehicleToolPacket::new).consumerMainThread(VehicleToolPacket::handle).add();
        CHANNEL.messageBuilder(VehicleToolActionPacket.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                .encoder(VehicleToolActionPacket::encode).decoder(VehicleToolActionPacket::new).consumerMainThread(VehicleToolActionPacket::handle).add();
        CHANNEL.messageBuilder(com.kurumaintown.network.VehiclePaintRgbPacket.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                .encoder(com.kurumaintown.network.VehiclePaintRgbPacket::encode).decoder(com.kurumaintown.network.VehiclePaintRgbPacket::new).consumerMainThread(com.kurumaintown.network.VehiclePaintRgbPacket::handle).add();
        CHANNEL.messageBuilder(com.kurumaintown.network.TuningAccessPacket.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                .encoder(com.kurumaintown.network.TuningAccessPacket::encode).decoder(com.kurumaintown.network.TuningAccessPacket::new).consumerMainThread(com.kurumaintown.network.TuningAccessPacket::handle).add();
        CHANNEL.messageBuilder(com.kurumaintown.network.TuningOpenPacket.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                .encoder(com.kurumaintown.network.TuningOpenPacket::encode).decoder(com.kurumaintown.network.TuningOpenPacket::new).consumerMainThread(com.kurumaintown.network.TuningOpenPacket::handle).add();
        CHANNEL.messageBuilder(com.kurumaintown.network.VehicleCustomPacket.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                .encoder(com.kurumaintown.network.VehicleCustomPacket::encode).decoder(com.kurumaintown.network.VehicleCustomPacket::new).consumerMainThread(com.kurumaintown.network.VehicleCustomPacket::handle).add();
    }
}
