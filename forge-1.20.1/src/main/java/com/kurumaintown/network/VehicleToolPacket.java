package com.kurumaintown.network;

import com.kurumaintown.TownCarData;
import com.kurumaintown.TownNetwork;
import com.kurumaintown.client.VehicleToolClientHandler;
import com.jdmmc.kurumamod.entity.CarEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.fml.DistExecutor;
import java.util.function.Supplier;

public class VehicleToolPacket {
    public static final int MODE_TABLET=0, MODE_REPAIR=1, MODE_FUEL=2, MODE_PAINT=3, MODE_CUSTOM=4;
    public final int mode, entityId;
    public final boolean admin;
    public final int[] custom, customMax;
    public final float engine, body, battery, armor, fuel, maxFuel;
    public VehicleToolPacket(int mode,int entityId,float engine,float body,float battery,float armor,float fuel,float maxFuel,boolean admin,int[] custom,int[] customMax){
        this.admin=admin;this.mode=mode;this.entityId=entityId;this.engine=engine;this.body=body;this.battery=battery;this.armor=armor;this.fuel=fuel;this.maxFuel=maxFuel;
        this.custom=custom.clone();this.customMax=customMax.clone();
    }
    public VehicleToolPacket(FriendlyByteBuf b){
        this.mode=b.readInt();this.entityId=b.readInt();this.engine=b.readFloat();this.body=b.readFloat();this.battery=b.readFloat();this.armor=b.readFloat();this.fuel=b.readFloat();this.maxFuel=b.readFloat();this.admin=b.readBoolean();
        this.custom=new int[6];this.customMax=new int[6];for(int i=0;i<6;i++){custom[i]=b.readInt();customMax[i]=b.readInt();}
    }
    public void encode(FriendlyByteBuf b){
        b.writeInt(mode);b.writeInt(entityId);b.writeFloat(engine);b.writeFloat(body);b.writeFloat(battery);b.writeFloat(armor);b.writeFloat(fuel);b.writeFloat(maxFuel);b.writeBoolean(admin);
        for(int i=0;i<6;i++){b.writeInt(custom[i]);b.writeInt(customMax[i]);}
    }
    public void handle(Supplier<NetworkEvent.Context> supplier){NetworkEvent.Context c=supplier.get();c.enqueueWork(()->DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->VehicleToolClientHandler.open(this)));c.setPacketHandled(true);}
    public static void send(ServerPlayer p,CarEntity car,int mode){TownCarData d=(TownCarData)car;TownNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new VehicleToolPacket(mode,car.getId(),d.town$getEngine(),d.town$getBody(),d.town$getBattery(),d.town$getArmor(),d.town$getFuel(),d.town$getMaxFuel(),p.getTags().contains("admin"),customValues(d),customMaxValues(d)));}
    private static int[] customValues(TownCarData d){int[] a=new int[6];for(int i=0;i<6;i++)a[i]=d.town$getCustom(com.kurumaintown.TownCustomDefaults.key(i));return a;}
    private static int[] customMaxValues(TownCarData d){int[] a=new int[6];for(int i=0;i<6;i++)a[i]=d.town$getCustomMax(com.kurumaintown.TownCustomDefaults.key(i));return a;}
    /** Ray test against the actual Kuruma hull, not the entity's approximate AABB. */
    public static boolean isLookingAt(ServerPlayer player, CarEntity car, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 dir = player.getLookAngle();
        com.jdmmc.kurumamod.physics.CarHull hull = car.currentBody();
        if (hull == null) return false;
        double[] bounds = hull.bounds();
        AABB broad = new AABB(bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5]);
        if (broad.inflate(0.06).clip(start, start.add(dir.scale(range))).isEmpty()
                && !broad.inflate(0.06).contains(start)) return false;
        // Short steps with a small probe box detect the oriented CarHull geometry.
        for (double distance = 0; distance <= range; distance += 0.025D) {
            Vec3 point = start.add(dir.scale(distance));
            double epsilon = 0.025D;
            com.jdmmc.kurumamod.physics.CarHull.Push contact = hull.against(new double[]{
                point.x-epsilon, point.y-epsilon, point.z-epsilon,
                point.x+epsilon, point.y+epsilon, point.z+epsilon});
            if (contact != null && contact.depth() > 0.0D) return true;
        }
        return false;
    }
    public static void openLookedAt(ServerPlayer p,double range,int mode){
        range = Math.min(1.5D, range);
        Vec3 start=p.getEyePosition(), end=start.add(p.getLookAngle().scale(range));
        AABB box=new AABB(start,end).inflate(3.0D);
        CarEntity best=null; double bestD=Double.MAX_VALUE;
        for (Entity entity:p.serverLevel().getEntities(p,box,e->e instanceof CarEntity)) {
            CarEntity car=(CarEntity)entity;
            if (!isLookingAt(p,car,range)) continue;
            double d=car.distanceToSqr(p);
            if (d<bestD) {bestD=d;best=car;}
        }
        if(best!=null)send(p,best,mode);
        else p.displayClientMessage(net.minecraft.network.chat.Component.literal("近くに車両がありません"),true);
    }
    public static void openNearest(ServerPlayer p,double x,double y,double z,double range,int mode){
        CarEntity best=null;double bd=range*range;
        for(Entity e:p.serverLevel().getEntities(p,new AABB(x-range,y-range,z-range,x+range,y+range,z+range),q->q instanceof CarEntity)){double d=e.distanceToSqr(x,y,z);if(d<bd){bd=d;best=(CarEntity)e;}}
        if(best!=null)send(p,best,mode);else p.displayClientMessage(net.minecraft.network.chat.Component.literal("近くに車両がありません"),true);
    }
}
