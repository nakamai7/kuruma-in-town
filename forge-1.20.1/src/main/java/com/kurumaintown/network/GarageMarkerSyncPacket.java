package com.kurumaintown.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class GarageMarkerSyncPacket {
    private final ResourceLocation dimension;
    private final List<Marker> markers;
    public record Marker(double x, double y, double z, float yaw, int job) {}
    public GarageMarkerSyncPacket(ResourceLocation dimension, List<Marker> markers) {
        this.dimension = dimension; this.markers = List.copyOf(markers);
    }
    public GarageMarkerSyncPacket(FriendlyByteBuf buf) {
        this.dimension = buf.readResourceLocation();
        int count = Math.max(0, Math.min(buf.readVarInt(), 4096));
        List<Marker> decoded = new ArrayList<>(count);
        for (int i = 0; i < count; i++) decoded.add(new Marker(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat(), buf.readVarInt()));
        this.markers = List.copyOf(decoded);
    }
    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(dimension); buf.writeVarInt(markers.size());
        for (Marker marker : markers) { buf.writeDouble(marker.x()); buf.writeDouble(marker.y()); buf.writeDouble(marker.z()); buf.writeFloat(marker.yaw()); buf.writeVarInt(marker.job()); }
    }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.kurumaintown.client.GarageMarkerSyncClientHandler.setMarkers(dimension, markers)));
        context.setPacketHandled(true);
    }
}
