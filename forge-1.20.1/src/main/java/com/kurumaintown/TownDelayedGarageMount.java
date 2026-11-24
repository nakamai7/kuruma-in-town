package com.kurumaintown;

import com.jdmmc.kurumamod.entity.CarEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;

/** Diagnostic: allow the native item-style spawn to finish before mounting. */
@Mod.EventBusSubscriber(modid = KurumaInTown.MODID)
public final class TownDelayedGarageMount {
    private static final Map<UUID, Pending> PENDING = new HashMap<>();
    private TownDelayedGarageMount() {}

    private static final class Pending {
        final UUID carId;
        int ticks = 0;
        Pending(UUID carId) { this.carId = carId; }
    }

    public static void schedule(ServerPlayer player, CarEntity car) {
        PENDING.put(player.getUUID(), new Pending(car.getUUID()));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        Iterator<Map.Entry<UUID, Pending>> iterator = PENDING.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Pending> item = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(item.getKey());
            if (player == null || !player.isAlive()) { iterator.remove(); continue; }
            Entity entity = player.serverLevel().getEntity(item.getValue().carId);
            if (!(entity instanceof CarEntity car) || !car.isAlive()) { iterator.remove(); continue; }
            Pending pending = item.getValue();
            if (++pending.ticks < 3) continue;
            iterator.remove();
            if (player.getVehicle() != null || player.distanceToSqr(car) > 64.0D) continue;
            player.startRiding(car, true);
        }
    }
    /** Restore only addon data; never reload entity NBT or change its rotation/physics. */
    public static void restoreTown(CarEntity car, CompoundTag town) {
        if (town.isEmpty()) return;
        TownCarData d = (TownCarData) car;
        CompoundTag data = town.getCompound("cardata");
        CompoundTag custom = data.getCompound("custom");
        d.town$setJob(town.getInt("job"));
        if (town.getTagType("owner") == net.minecraft.nbt.Tag.TAG_STRING)
            d.town$setOwner(town.getString("owner"));
        d.town$setCarNumber(town.getString("carnumber"));
        d.town$setInGarage(0);
        if (data.contains("enginelevel")) d.town$setEngine(data.getFloat("enginelevel"));
        if (data.contains("bodylevel")) d.town$setBody(data.getFloat("bodylevel"));
        if (data.contains("batterylevel")) d.town$setBattery(data.getFloat("batterylevel"));
        if (data.contains("basefuellevel")) d.town$setBaseFuel(data.getFloat("basefuellevel"));
        else if (data.contains("maxfuellevel")) d.town$setBaseFuel(data.getFloat("maxfuellevel"));
        for (String key : new String[]{"enginecustom", "steeringcustom", "suspensioncustom", "tankcustom", "armorcustom", "brakecustom"}) {
            if (custom.contains("max" + key)) d.town$setCustomMax(key, custom.getInt("max" + key));
            if (custom.contains(key)) d.town$setCustom(key, custom.getInt(key));
        }
        if (data.contains("fuellevel")) d.town$setFuel(data.getFloat("fuellevel"));
        if (data.contains("enginemulti")) d.town$setEngineMulti(data.getFloat("enginemulti"));
        if (data.contains("bodymulti")) d.town$setBodyMulti(data.getFloat("bodymulti"));
        if (data.contains("batterymulti")) d.town$setBatteryMulti(data.getFloat("batterymulti"));
        if (data.contains("fuelmulti")) d.town$setFuelMulti(data.getFloat("fuelmulti"));
        if (data.contains("paintcolor")) d.town$setPaintColor(data.getInt("paintcolor"));
    }

}
