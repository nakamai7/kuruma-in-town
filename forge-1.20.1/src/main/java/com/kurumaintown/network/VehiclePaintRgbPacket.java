package com.kurumaintown.network;

import com.jdmmc.kurumamod.entity.CarEntity;
import com.kurumaintown.ColorSprayItem;
import com.kurumaintown.TownCarData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record VehiclePaintRgbPacket(int entityId, int red, int green, int blue) {
    public VehiclePaintRgbPacket(FriendlyByteBuf b) { this(b.readInt(), b.readInt(), b.readInt(), b.readInt()); }
    public void encode(FriendlyByteBuf b) { b.writeInt(entityId); b.writeInt(red); b.writeInt(green); b.writeInt(blue); }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer p = context.getSender();
            if (p == null || !p.getTags().contains("admin")) return;
            if (red < 0 || red > 255 || green < 0 || green > 255 || blue < 0 || blue > 255) return;
            boolean hasSpray = p.getMainHandItem().getItem() instanceof ColorSprayItem
                    || p.getOffhandItem().getItem() instanceof ColorSprayItem;
            if (!hasSpray) return;
            Entity entity = p.serverLevel().getEntity(entityId);
            if (!(entity instanceof CarEntity car) || !VehicleToolPacket.isLookingAt(p,car,1.5D)) return;
            int rgb = (red << 16) | (green << 8) | blue;
            ((TownCarData) car).town$setPaintColor(-2 - rgb);
        });
        context.setPacketHandled(true);
    }
}
