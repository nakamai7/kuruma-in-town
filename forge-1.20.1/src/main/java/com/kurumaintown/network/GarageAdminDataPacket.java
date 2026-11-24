package com.kurumaintown.network;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
public record GarageAdminDataPacket(CompoundTag players,String selected){
    public GarageAdminDataPacket(FriendlyByteBuf b){this(read(b),b.readUtf(40));}
    private static CompoundTag read(FriendlyByteBuf b){CompoundTag t=b.readNbt();return t==null?new CompoundTag():t;}
    public void encode(FriendlyByteBuf b){b.writeNbt(players);b.writeUtf(selected);}
    public void handle(Supplier<NetworkEvent.Context> supplier){NetworkEvent.Context ctx=supplier.get();ctx.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.kurumaintown.client.GarageAdminClient.open(players,selected)));ctx.setPacketHandled(true);}
}
