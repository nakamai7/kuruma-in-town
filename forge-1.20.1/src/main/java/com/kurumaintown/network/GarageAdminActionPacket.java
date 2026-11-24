package com.kurumaintown.network;
import com.kurumaintown.GarageAdminService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.UUID;
import java.util.function.Supplier;
public record GarageAdminActionPacket(String target,String carnumber,boolean delete){
    public GarageAdminActionPacket(FriendlyByteBuf b){this(b.readUtf(40),b.readUtf(64),b.readBoolean());}
    public void encode(FriendlyByteBuf b){b.writeUtf(target);b.writeUtf(carnumber);b.writeBoolean(delete);}
    public void handle(Supplier<NetworkEvent.Context> supplier){NetworkEvent.Context ctx=supplier.get();ctx.enqueueWork(()->{
        ServerPlayer admin=ctx.getSender();if(admin==null)return;
        try{GarageAdminService.action(admin,UUID.fromString(target),carnumber,delete);}catch(IllegalArgumentException ignored){}
    });ctx.setPacketHandled(true);}
}
