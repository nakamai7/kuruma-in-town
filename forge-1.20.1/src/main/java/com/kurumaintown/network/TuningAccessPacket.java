package com.kurumaintown.network;

import com.kurumaintown.TownNetwork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import java.util.function.Supplier;

/** Client asks server to verify the current scoreboard entity tag. */
public record TuningAccessPacket() {
    public TuningAccessPacket(FriendlyByteBuf buf) { this(); }
    public void encode(FriendlyByteBuf buf) { }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null && player.getTags().contains("admin"))
                TownNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new TuningOpenPacket());
        });
        ctx.setPacketHandled(true);
    }
}
