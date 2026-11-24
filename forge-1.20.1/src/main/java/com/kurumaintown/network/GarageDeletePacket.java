package com.kurumaintown.network;

import com.kurumaintown.TownItems;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Marker;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record GarageDeletePacket() {
    public GarageDeletePacket(FriendlyByteBuf buf) { this(); }
    public void encode(FriendlyByteBuf buf) {}
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null || !(player.getMainHandItem().is(TownItems.GARAGE.get()) || player.getOffhandItem().is(TownItems.GARAGE.get()))) return;
            Marker nearest = null;
            double distance = 9.0D;
            for (Entity entity : player.serverLevel().getAllEntities()) {
                if (!(entity instanceof Marker marker) || !entity.getTags().contains("cargarage")) continue;
                double current = entity.distanceToSqr(player);
                if (current <= distance) { nearest = marker; distance = current; }
            }
            if (nearest == null) {
                player.displayClientMessage(Component.literal("3m以内にガレージマーカーがありません"), true);
                return;
            }
            nearest.discard();
            player.displayClientMessage(Component.literal("ガレージマーカーを削除しました"), true);
        });
        ctx.setPacketHandled(true);
    }
}
