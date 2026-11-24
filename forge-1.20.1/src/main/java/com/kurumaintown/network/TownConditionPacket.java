package com.kurumaintown.network;

import com.kurumaintown.client.TownConditionCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record TownConditionPacket(int entityId,float engine,float body,float battery,float fuel,float maxFuel,float armor,float maxArmor) {
    public TownConditionPacket(FriendlyByteBuf b){this(b.readVarInt(),b.readFloat(),b.readFloat(),b.readFloat(),b.readFloat(),b.readFloat(),b.readFloat(),b.readFloat());}
    public void encode(FriendlyByteBuf b){b.writeVarInt(entityId);b.writeFloat(engine);b.writeFloat(body);b.writeFloat(battery);b.writeFloat(fuel);b.writeFloat(maxFuel);b.writeFloat(armor);b.writeFloat(maxArmor);}
    public void handle(Supplier<NetworkEvent.Context> supplier){
        NetworkEvent.Context ctx=supplier.get();
        ctx.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->TownConditionCache.put(entityId,engine,body,battery,fuel,maxFuel,armor,maxArmor)));
        ctx.setPacketHandled(true);
    }
}
