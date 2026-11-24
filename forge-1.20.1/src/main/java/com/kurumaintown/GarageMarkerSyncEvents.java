package com.kurumaintown;

import com.kurumaintown.network.GarageMarkerSyncPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = KurumaInTown.MODID)
public final class GarageMarkerSyncEvents {
    private static int ticks;
    private GarageMarkerSyncEvents() {}

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        if (++ticks % 20 != 0) return;
        List<GarageMarkerSyncPacket.Marker> markers = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity.getTags().contains("cargarage")) {
                markers.add(new GarageMarkerSyncPacket.Marker(entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), TownJobStorage.isJobGarage(entity) ? 1 : 0));
            }
        }
        // Filter on the server: the "job" scoreboard objective need not be
        // synchronized to clients, and unauthorized players receive no job markers.
        for (ServerPlayer player : level.players()) {
            int playerJob = TownJobStorage.getPlayerJob(player);
            List<GarageMarkerSyncPacket.Marker> visible = playerJob == 1 || playerJob == 2
                    ? markers
                    : markers.stream().filter(marker -> marker.job() == 0).toList();
            GarageMarkerSyncPacket packet = new GarageMarkerSyncPacket(level.dimension().location(), visible);
            TownNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player), packet);
        }
    }
}
