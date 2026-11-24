package com.kurumaintown.network;

import com.kurumaintown.client.TuningOpenClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record TuningOpenPacket() {
    public TuningOpenPacket(FriendlyByteBuf buf) { this(); }
    public void encode(FriendlyByteBuf buf) { }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> TuningOpenClient::open));
        ctx.setPacketHandled(true);
    }
}
