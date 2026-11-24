package com.kurumaintown.network;
import com.kurumaintown.TownItems;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Marker;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
public record GarageRotatePacket(float yaw) {
    public GarageRotatePacket(FriendlyByteBuf b){this(b.readFloat());}
    public void encode(FriendlyByteBuf b){b.writeFloat(yaw);}
    public void handle(Supplier<NetworkEvent.Context> supplier){
        NetworkEvent.Context ctx=supplier.get();
        ctx.enqueueWork(()->{
            ServerPlayer p=ctx.getSender();
            if(p==null || !Float.isFinite(yaw) || !(p.getMainHandItem().is(TownItems.GARAGE.get()) || p.getOffhandItem().is(TownItems.GARAGE.get())))return;
            Marker nearest=null;double distance=9.0;
            for(Entity e:p.serverLevel().getAllEntities()){
                if(!(e instanceof Marker marker) || !e.getTags().contains("cargarage"))continue;
                double d=e.distanceToSqr(p);
                if(d<=distance){nearest=marker;distance=d;}
            }
            if(nearest==null){p.displayClientMessage(Component.literal("3m以内にガレージがありません"),true);return;}
            nearest.setYRot(yaw);
            p.displayClientMessage(Component.literal("ガレージの向きを変更しました"),true);
        });ctx.setPacketHandled(true);
    }
}
