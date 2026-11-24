package com.kurumaintown;
import com.jdmmc.kurumamod.entity.CarEntity;
import com.kurumaintown.network.GarageAdminDataPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.PacketDistributor;
import java.util.UUID;
public final class GarageAdminService {
    private GarageAdminService(){}
    public static final UUID JOB1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID JOB2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    public static boolean authorized(ServerPlayer admin){
        return admin.getTags().contains("admin") && (admin.getMainHandItem().is(TownItems.GARAGE_ADMIN.get()) || admin.getOffhandItem().is(TownItems.GARAGE_ADMIN.get()));
    }
    public static ServerPlayer target(ServerPlayer admin,UUID uuid){
        ServerPlayer target=admin.getServer().getPlayerList().getPlayer(uuid);
        return target!=null && target.serverLevel()==admin.serverLevel() && target.distanceToSqr(admin)<=25.0D ? target:null;
    }
    public static void openRegistry(ServerPlayer admin) {
        if (!authorized(admin)) return;
        CompoundTag storage = admin.getServer().getCommandStorage().get(new net.minecraft.resources.ResourceLocation("kuruma_in_town", "storage"));
        TownNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> admin),
            new GarageAdminDataPacket(storage.getCompound("carlist").copy(), "__world_carlist__"));
    }
    public static void open(ServerPlayer admin,UUID selected){
        if(!authorized(admin))return;
        CompoundTag players=new CompoundTag();
        for(ServerPlayer p:admin.serverLevel().players()){
            if(p.distanceToSqr(admin)>25.0D)continue;
            CompoundTag entry=new CompoundTag();entry.putString("name",p.getGameProfile().getName());
            if(selected!=null && p.getUUID().equals(selected))entry.put("cars",p.getPersistentData().getCompound("carlist").copy());
            players.put(p.getUUID().toString(),entry);
        }
        if (selected != null && (selected.equals(JOB1) || selected.equals(JOB2))) {
            int job = selected.equals(JOB1) ? 1 : 2;
            CompoundTag entry = new CompoundTag();
            entry.putString("name", "ジョブ" + job);
            entry.put("cars", TownJobStorage.getJobList(admin.getServer(), job));
            players.put(selected.toString(), entry);
        }
        TownNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(()->admin),new GarageAdminDataPacket(players,selected==null?"":selected.toString()));
    }
    public static void action(ServerPlayer admin,UUID uuid,String number,boolean delete){
        if(!authorized(admin))return;
        boolean jobView = uuid.equals(JOB1) || uuid.equals(JOB2);
        ServerPlayer target = jobView ? null : target(admin,uuid);
        if ((!jobView && target==null) || number.isBlank()) return;
        int job = uuid.equals(JOB1) ? 1 : uuid.equals(JOB2) ? 2 : 0;
        CompoundTag list = jobView ? TownJobStorage.getJobList(admin.getServer(),job) : target.getPersistentData().getCompound("carlist");
        if(!list.contains(number,10))return;
        if(!delete && list.getCompound(number).getInt("ingarage")==1)return;
        if(delete){list.remove(number);}
        else {
            // Only vehicles belonging to this target and matching the exact carnumber may be removed.
            for(net.minecraft.server.level.ServerLevel level:admin.getServer().getAllLevels()){
                for(Entity entity:level.getAllEntities()){
                    if(entity instanceof CarEntity car){
                        TownCarData data=(TownCarData)car;
                        if(number.equals(data.town$getCarNumber()) && (jobView ? TownJobStorage.vehicleJob(list.getCompound(number)) == job : uuid.toString().equals(data.town$getOwner()))) car.discard();
                    }
                }
            }
            CompoundTag entry=list.getCompound(number);
            entry.putInt("ingarage",1);
            CompoundTag vehicle=entry.getCompound("vehicledata");
            CompoundTag town=vehicle.getCompound("town");
            town.putInt("ingarage",1);
            if(town.contains("cardata",10))town.getCompound("cardata").putInt("ingarage",1);
            vehicle.put("town",town);entry.put("vehicledata",vehicle);
            if(entry.contains("cardata",10))entry.getCompound("cardata").putInt("ingarage",1);
            list.put(number,entry);
        }
        if (jobView) TownJobStorage.putJobList(admin.getServer(),job,list);
        else target.getPersistentData().put("carlist",list);
    }
}
