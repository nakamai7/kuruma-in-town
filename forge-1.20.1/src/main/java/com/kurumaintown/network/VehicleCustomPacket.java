package com.kurumaintown.network;

import com.jdmmc.kurumamod.entity.CarEntity;
import com.kurumaintown.CustomToolItem;
import com.kurumaintown.TownCarData;
import com.kurumaintown.TownCustomDefaults;
import com.kurumaintown.TownNetwork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record VehicleCustomPacket(int entityId, int index, int value) {
    public VehicleCustomPacket(FriendlyByteBuf buf) { this(buf.readInt(),buf.readInt(),buf.readInt()); }
    public void encode(FriendlyByteBuf buf) { buf.writeInt(entityId);buf.writeInt(index);buf.writeInt(value); }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx=supplier.get();
        ctx.enqueueWork(()->{
            ServerPlayer p=ctx.getSender();
            if(p==null || index<0 || index>=6)return;
            Entity e=p.serverLevel().getEntity(entityId);
            if(!(e instanceof CarEntity car) || p.distanceToSqr(car)>36.0D)return;
            if(!(p.getMainHandItem().getItem() instanceof CustomToolItem) && !(p.getOffhandItem().getItem() instanceof CustomToolItem))return;
            if(!VehicleToolPacket.isLookingAt(p,car,1.5D))return;
            TownCarData data=(TownCarData)car;
            String key=TownCustomDefaults.key(index);
            if(value<0 || value>data.town$getCustomMax(key))return;
            com.kurumaintown.TownVehicleTasks.startCustom(p,car,index,value);
        });
        ctx.setPacketHandled(true);
    }
}
