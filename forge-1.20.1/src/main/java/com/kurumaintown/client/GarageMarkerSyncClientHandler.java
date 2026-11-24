package com.kurumaintown.client;

import com.kurumaintown.network.GarageMarkerSyncPacket;
import net.minecraft.resources.ResourceLocation;
import java.util.List;

/** Client-only endpoint for synchronized garage marker positions. */
public final class GarageMarkerSyncClientHandler {
    private GarageMarkerSyncClientHandler() {}
    public static void setMarkers(ResourceLocation dimension, List<GarageMarkerSyncPacket.Marker> markers) {
        GarageMarkerIconRenderer.setMarkers(dimension, markers);
        GarageMarkerParticles.update(dimension, markers);
    }
}
