package com.kurumaintown.network;

import com.jdmmc.kurumamod.Kurumamod;
import com.jdmmc.kurumamod.entity.CarEntity;
import com.jdmmc.kurumamod.item.CarSpawnItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.Tag;
import com.kurumaintown.TownCarData;
import com.kurumaintown.TownJobStorage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record GarageRidePacket(String carnumber,double markerX,double markerY,double markerZ) {
    public GarageRidePacket(FriendlyByteBuf b){this(b.readUtf(32),b.readDouble(),b.readDouble(),b.readDouble());}
    public void encode(FriendlyByteBuf b){b.writeUtf(carnumber,32);b.writeDouble(markerX);b.writeDouble(markerY);b.writeDouble(markerZ);}
    public void handle(Supplier<NetworkEvent.Context> supplier){
        NetworkEvent.Context ctx=supplier.get();
        ctx.enqueueWork(()->{
            ServerPlayer player=ctx.getSender();
            if(player==null||carnumber==null||carnumber.isBlank())return;
            // Match the marker whose position was sent when the garage screen opened.
            // Never silently switch to another nearby garage with a different job.
            Entity marker=null; double best=Double.MAX_VALUE;
            for(Entity e:player.serverLevel().getAllEntities()) {
                if(e==player || !e.getTags().contains("cargarage") || e.distanceToSqr(player)>9.0D) continue;
                double d=e.distanceToSqr(markerX,markerY,markerZ);
                if(d<0.01D && d<best) {best=d;marker=e;}
            }
            if(marker==null)return;
            boolean jobGarage=TownJobStorage.isJobGarage(marker);
            int playerJob=TownJobStorage.getPlayerJob(player);
            if(jobGarage && playerJob!=1 && playerJob!=2)return;
            CompoundTag list=jobGarage
                    ? TownJobStorage.getJobList(player.getServer(),playerJob)
                    : player.getPersistentData().getCompound("carlist");
            if(!list.contains(carnumber,10))return;
            CompoundTag entry=list.getCompound(carnumber);
            if(entry.getInt("ingarage")!=1)return;
            int vehicleJob=TownJobStorage.vehicleJob(entry);
            if(jobGarage ? vehicleJob!=playerJob : vehicleJob!=0)return;
            // Personal garage entries remain owner-bound; matching job garages are shared.
            if(!jobGarage && !entry.getString("owner").isEmpty()&&!entry.getString("owner").equals(player.getUUID().toString()))return;
            CompoundTag saved=entry.getCompound("vehicledata").copy();
            if(saved.isEmpty())return;
            // Diagnostic 1.8.8: use the same creation path as CarSpawnItem.useOn.
            // Only the heading source and position differ from item placement.
            CarEntity source=Kurumamod.CAR.get().create(player.level());
            if(source==null)return;
            source.load(saved);
            ItemStack spawnItem=CarSpawnItem.stackFor(source);
            CarEntity car=Kurumamod.CAR.get().create(player.level());
            if(car==null)return;
            CarSpawnItem.applyTo(car,spawnItem);
            float garageYaw=marker.getYRot();
            car.moveTo(marker.getX(),marker.getY()+1.0D,marker.getZ(),garageYaw,0.0F);
            // Restore identity and all addon data BEFORE entity spawn and before mounting.
            // Otherwise the initial entity tick / claim may issue an orphan carnumber.
            CompoundTag savedTown = saved.getCompound("town").copy();
            savedTown.putInt("ingarage", 0);
            if (savedTown.contains("cardata", Tag.TAG_COMPOUND))
                savedTown.getCompound("cardata").putInt("ingarage", 0);
            com.kurumaintown.TownDelayedGarageMount.restoreTown(car, savedTown);
            if(!player.level().addFreshEntity(car)){car.discard();return;}
            // Keep the original three-tick mounting delay, but never delay NBT restoration.
            com.kurumaintown.TownDelayedGarageMount.schedule(player, car);
            entry.putInt("ingarage",0);
            CompoundTag entryVehicle=entry.getCompound("vehicledata");
            CompoundTag entryTown=entryVehicle.getCompound("town");entryTown.putInt("ingarage",0);entryVehicle.put("town",entryTown);
            entry.put("vehicledata",entryVehicle);list.put(carnumber,entry);
            if(!jobGarage) player.getPersistentData().put("carlist",list);
            else TownJobStorage.putJobList(player.getServer(),playerJob,list);
        });
        ctx.setPacketHandled(true);
    }
}
