package com.kurumaintown.network;

import com.kurumaintown.TownJobStorage;
import com.kurumaintown.TownNetwork;
import com.kurumaintown.TownGarageEvents;
import com.jdmmc.kurumamod.entity.CarEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import java.util.function.Supplier;

public record GarageOpenPacket() {
    public GarageOpenPacket(net.minecraft.network.FriendlyByteBuf buf) { this(); }
    public void encode(net.minecraft.network.FriendlyByteBuf buf) {}
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx=supplier.get();
        ctx.enqueueWork(()->{
            ServerPlayer player=ctx.getSender();
            if(player==null)return;
            // While driving, the key only attempts to store the car; it never opens the HUD.
            if (player.getVehicle() instanceof CarEntity car && car.getFirstPassenger()==player) {
                TownGarageEvents.storeIfEligible(car, player);
                return;
            }
            // The nearest garage determines only whether this is a shared job garage.
            // Marker job scores 1 and 2 are equivalent (and the "job" tag is supported).
            int playerJob = TownJobStorage.getPlayerJob(player);
            Entity marker=null; double nearest=9.0D;
            for(Entity candidate:player.serverLevel().getAllEntities()) {
                if(candidate==player || !candidate.getTags().contains("cargarage")) continue;
                double d=candidate.distanceToSqr(player);
                if(d<=nearest) {marker=candidate; nearest=d;}
            }
            if(marker==null)return;
            CompoundTag list;
            if (!TownJobStorage.isJobGarage(marker)) {
                list=TownJobStorage.filterList(player.getPersistentData().getCompound("carlist"),0);
            } else {
                list=(playerJob==1 || playerJob==2)
                        ? TownJobStorage.filterList(TownJobStorage.getJobList(player.getServer(),playerJob),playerJob)
                        : new CompoundTag();
            }
            TownNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(()->player),
                    new GarageDataPacket(list,marker.getX(),marker.getY(),marker.getZ(),marker.getYRot(),marker.getXRot()));
        });
        ctx.setPacketHandled(true);
    }
}
