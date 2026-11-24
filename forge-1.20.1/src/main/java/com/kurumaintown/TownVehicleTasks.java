package com.kurumaintown;

import com.jdmmc.kurumamod.entity.CarEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid=KurumaInTown.MODID)
public final class TownVehicleTasks {
    private static final Map<UUID,Task> TASKS = new HashMap<>();
    private TownVehicleTasks() {}

    private static final class Task {
        final UUID playerId, carId;
        final double px, py, pz, cx, cy, cz;
        final int action;
        final int paint;
        final int customIndex, customValue;
        int ticks = 0;
        Task(ServerPlayer p, CarEntity c, int action, int paint) {
            playerId=p.getUUID(); carId=c.getUUID(); px=p.getX(); py=p.getY(); pz=p.getZ();
            cx=c.getX(); cy=c.getY(); cz=c.getZ(); this.action=action; this.paint=paint; this.customIndex=-1; this.customValue=-1;
        }
        Task(ServerPlayer p, CarEntity c, int index, int value, boolean customTask) {
        playerId=p.getUUID(); carId=c.getUUID(); px=p.getX(); py=p.getY(); pz=p.getZ();
        cx=c.getX(); cy=c.getY(); cz=c.getZ(); action=6; paint=-1;
        customIndex=index; customValue=value;
        }
    }

    public static void startCustom(ServerPlayer p, CarEntity car, int index, int value) {
        if(index<0 || index>=6 || TASKS.containsKey(p.getUUID()) || TASKS.values().stream().anyMatch(t -> t.carId.equals(car.getUUID()))) return;
        TownCarData d=(TownCarData)car;
        String key=TownCustomDefaults.key(index);
        if(value<0 || value>d.town$getCustomMax(key) || value==d.town$getCustom(key)) return;
        if (p.getTags().contains("admin")) {
            d.town$setCustom(key, value);
            return;
        }
        TASKS.put(p.getUUID(), new Task(p,car,index,value,true));
    }

    public static void startRepair(ServerPlayer p, CarEntity car, int action) {
        if (TASKS.containsKey(p.getUUID()) || TASKS.values().stream().anyMatch(t -> t.carId.equals(car.getUUID()))) return;
        TownCarData d=(TownCarData)car;
        if (action<0 || action>2) return;
        if ((action==0&&d.town$getEngine()>=100) || (action==1&&d.town$getBody()>=100) || (action==2&&d.town$getBattery()>=100)) return;
        if (p.getTags().contains("admin")) {
            switch (action) {
                case 0 -> d.town$setEngine(100);
                case 1 -> d.town$setBody(100);
                case 2 -> d.town$setBattery(100);
            }
            return;
        }
        TASKS.put(p.getUUID(), new Task(p,car,action,-1));
    }

    public static void startFuel(ServerPlayer p, CarEntity car) {
        if (TASKS.containsKey(p.getUUID()) || TASKS.values().stream().anyMatch(t -> t.carId.equals(car.getUUID()))) return;
        TownCarData d=(TownCarData)car;
        if (d.town$getFuel()>=d.town$getMaxFuel()) return;
        TASKS.put(p.getUUID(), new Task(p,car,4,-1));
    }

    /** Admins paint immediately; regular players are locked for 200 ticks. */
    public static void startPaint(ServerPlayer p, CarEntity car, int paint) {
        if (p.getTags().contains("admin")) {
            ((TownCarData)car).town$setPaintColor(paint);
            return;
        }
        if (TASKS.containsKey(p.getUUID()) || TASKS.values().stream().anyMatch(t -> t.carId.equals(car.getUUID()))) return;
        TASKS.put(p.getUUID(), new Task(p, car, 5, paint));
    }

    private static String progressBar(int done, int total) {
        int filled = Math.max(0, Math.min(20, (int)Math.floor(done * 20.0 / Math.max(1,total))));
        return "§8[§a" + "▌".repeat(filled) + "§7" + "▌".repeat(20-filled) + "§8]";
    }

    @SubscribeEvent public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase!=TickEvent.Phase.END) return;
        MinecraftServer server=event.getServer();
        Iterator<Map.Entry<UUID,Task>> it=TASKS.entrySet().iterator();
        while(it.hasNext()) {
            Task t=it.next().getValue();
            ServerPlayer p=server.getPlayerList().getPlayer(t.playerId);
            if (p==null) { it.remove(); continue; }
            Entity e=p.serverLevel().getEntity(t.carId);
            if (!(e instanceof CarEntity car) || !car.isAlive()) { it.remove(); continue; }
            if (p.isShiftKeyDown()) {
                p.displayClientMessage(Component.literal("§c作業を中断しました"), true);
                it.remove();
                continue;
            }
            p.teleportTo(p.serverLevel(),t.px,t.py,t.pz,p.getYRot(),p.getXRot());
            p.setDeltaMovement(Vec3.ZERO); p.hurtMarked=true;
            car.teleportTo(t.cx,t.cy,t.cz); car.setDeltaMovement(Vec3.ZERO); car.hurtMarked=true;
            t.ticks++;
            TownCarData d=(TownCarData)car;
            if (t.action==5) {
                int percent = Math.min(100, t.ticks / 2);
                p.displayClientMessage(Component.literal(progressBar(t.ticks,200)+" §f"+percent+"%"),true);
                if (t.ticks>=200) {
                    d.town$setPaintColor(t.paint);
                    it.remove();
                }
            } else if (t.action==6) {
                int percent=Math.min(100,t.ticks/2);
                p.displayClientMessage(Component.literal(progressBar(t.ticks,200)+" §f"+percent+"%"),true);
                if(t.ticks>=200) {
                    String key=TownCustomDefaults.key(t.customIndex);
                    if(t.customValue>=0 && t.customValue<=d.town$getCustomMax(key)) d.town$setCustom(key,t.customValue);
                    it.remove();
                }
            } else if (t.action==4) {
                if (t.ticks%4==0) d.town$setFuel(Math.min(d.town$getMaxFuel(),d.town$getFuel()+0.5F));
                double max=Math.max(0.1,d.town$getMaxFuel());
                int percent=(int)Math.min(100,Math.round(d.town$getFuel()/max*100.0));
                p.displayClientMessage(Component.literal(progressBar(percent,100)+" §f"+String.format(java.util.Locale.ROOT,"%.1f / %.1f",d.town$getFuel(),d.town$getMaxFuel())),true);
                if (d.town$getFuel()>=d.town$getMaxFuel()) it.remove();
            } else {
                int percent=(int)Math.min(100,t.ticks);
                p.displayClientMessage(Component.literal(progressBar(percent,100)+" §f"+percent+"%"),true);
                if (t.ticks>=100) {
                    switch(t.action) {
                        case 0 -> d.town$setEngine(100);
                        case 1 -> d.town$setBody(100);
                        case 2 -> d.town$setBattery(100);
                    }
                    it.remove();
                }
            }
        }
    }
}
