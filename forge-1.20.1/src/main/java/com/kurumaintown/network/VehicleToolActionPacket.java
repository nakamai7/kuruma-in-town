package com.kurumaintown.network;

import com.kurumaintown.TownCarData;
import com.kurumaintown.TownVehicleTasks;
import com.jdmmc.kurumamod.entity.CarEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record VehicleToolActionPacket(int entityId,int action) {
    public VehicleToolActionPacket(FriendlyByteBuf b){this(b.readInt(),b.readInt());}
    public void encode(FriendlyByteBuf b){b.writeInt(entityId);b.writeInt(action);}
    public void handle(Supplier<NetworkEvent.Context> supplier){NetworkEvent.Context c=supplier.get();c.enqueueWork(()->{
        ServerPlayer p=c.getSender();if(p==null)return;Entity e=p.serverLevel().getEntity(entityId);if(!(e instanceof CarEntity car)||p.distanceToSqr(car)>36)return;
        if(action>=16 && action<32){
            // Server-authoritative paint selection, usable only with a repair kit in hand.
            boolean hasKit = p.getMainHandItem().getItem() instanceof com.kurumaintown.ColorSprayItem
                    || p.getOffhandItem().getItem() instanceof com.kurumaintown.ColorSprayItem;
            if(hasKit && VehicleToolPacket.isLookingAt(p,car,1.5D)) TownVehicleTasks.startPaint(p,car,action-16);
            return;
        }
        if(action==4){TownVehicleTasks.startFuel(p,car);return;}
        if(action<0||action>2)return;TownVehicleTasks.startRepair(p,car,action);
    });c.setPacketHandled(true);}
}
