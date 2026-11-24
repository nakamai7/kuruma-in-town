package com.kurumaintown.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Network-safe payload. Client GUI code is isolated in a client-only class. */
public record GarageDataPacket(CompoundTag carlist,double x,double y,double z,float yaw,float pitch) {
    public GarageDataPacket(FriendlyByteBuf b){this(readTag(b),b.readDouble(),b.readDouble(),b.readDouble(),b.readFloat(),b.readFloat());}
    private static CompoundTag readTag(FriendlyByteBuf b){CompoundTag t=b.readNbt();return t==null?new CompoundTag():t;}
    public void encode(FriendlyByteBuf b){b.writeNbt(carlist);b.writeDouble(x);b.writeDouble(y);b.writeDouble(z);b.writeFloat(yaw);b.writeFloat(pitch);}
    public void handle(Supplier<NetworkEvent.Context> supplier){
        NetworkEvent.Context ctx=supplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.kurumaintown.client.GarageDataClientHandler.open(carlist, x, y, z, yaw, pitch)));
        ctx.setPacketHandled(true);
    }
}
