package com.kurumaintown;

import com.jdmmc.kurumamod.entity.CarEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=KurumaInTown.MODID)
public final class TownGarageEvents {
    private TownGarageEvents() {}

    @SubscribeEvent public static void onMount(EntityMountEvent event) {
        if (!event.isMounting() || !(event.getEntityBeingMounted() instanceof CarEntity car)
                || !(event.getEntityMounting() instanceof Player player) || player.level().isClientSide) return;
        ((TownCarData)car).town$claim(player.getUUID().toString());
    }

    /** Store the driven vehicle at a compatible nearby cargarage marker. */
    public static void storeIfEligible(CarEntity car, Player player) {
        if (car.level().isClientSide || !(player instanceof ServerPlayer serverPlayer)
                || !(player.level() instanceof ServerLevel level)) return;
        TownCarData data=(TownCarData)car;
        if (car.getFirstPassenger()!=player) return;
        Entity marker=null; double nearest=9.0D;
        for (Entity candidate:level.getAllEntities()) {
            if (candidate==car || !candidate.getTags().contains("cargarage")) continue;
            double d=candidate.distanceToSqr(car);
            if (d<=nearest) { nearest=d; marker=candidate; }
        }
        if (marker==null) return;
        boolean jobGarage=TownJobStorage.isJobGarage(marker);
        int vehicleJob=data.town$getJob();
        int playerJob=TownJobStorage.getPlayerJob(serverPlayer);
        // Shared job markers accept either job score, but vehicles are kept in
        // the matching job's world-storage list. Personal cars use personal markers.
        if (jobGarage) {
            if ((playerJob!=1 && playerJob!=2) || vehicleJob!=playerJob) return;
        } else {
            if (vehicleJob!=0 || !data.town$getOwner().equals(player.getUUID().toString())) return;
        }

        CompoundTag vehicleData=car.saveWithoutId(new CompoundTag());
        CompoundTag town=vehicleData.getCompound("town");
        town.putInt("ingarage",1);
        vehicleData.put("town",town);
        CompoundTag entry=new CompoundTag();
        entry.putString("owner",vehicleJob==0?data.town$getOwner():TownJobStorage.ZERO_OWNER);
        entry.putString("carnumber",data.town$getCarNumber());
        entry.putInt("ingarage",1);
        entry.put("vehicledata",vehicleData);
        entry.put("cardata",town.getCompound("cardata").copy());
        if (vehicleJob==0) {
            CompoundTag carlist=player.getPersistentData().getCompound("carlist");
            carlist.put(data.town$getCarNumber(),entry);
            player.getPersistentData().put("carlist",carlist);
        } else {
            CompoundTag list=TownJobStorage.getJobList(player.getServer(),vehicleJob);
            list.put(data.town$getCarNumber(),entry);
            TownJobStorage.putJobList(player.getServer(),vehicleJob,list);
        }
        data.town$setInGarage(1);
        if (player.getServer()!=null) player.getServer().execute(car::discard);
    }
}
