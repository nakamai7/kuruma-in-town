package com.kurumaintown.network;
import com.kurumaintown.GarageAdminService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.UUID;
import java.util.function.Supplier;
public record GarageAdminRequestPacket(String target){
    public GarageAdminRequestPacket(FriendlyByteBuf b){this(b.readUtf(40));}
    public void encode(FriendlyByteBuf b){b.writeUtf(target);}
    public void handle(Supplier<NetworkEvent.Context> supplier){NetworkEvent.Context ctx=supplier.get();ctx.enqueueWork(()->{
        ServerPlayer admin=ctx.getSender();if(admin==null)return;
        if(target.isEmpty()){GarageAdminService.open(admin,null);return;}
        try{UUID id=UUID.fromString(target);if(id.equals(GarageAdminService.JOB1) || id.equals(GarageAdminService.JOB2) || GarageAdminService.target(admin,id)!=null)GarageAdminService.open(admin,id);}catch(IllegalArgumentException ignored){}
    });ctx.setPacketHandled(true);}
}
