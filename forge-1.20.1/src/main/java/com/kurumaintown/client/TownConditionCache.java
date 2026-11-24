package com.kurumaintown.client;

import com.jdmmc.kurumamod.entity.CarEntity;
import com.kurumaintown.TownCarData;
import net.minecraft.client.Minecraft;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class TownConditionCache {
    public record Data(float engine,float body,float battery,float fuel,float maxFuel,float armor,float maxArmor) {}
    private static final Map<Integer,Data> DATA=new ConcurrentHashMap<>();
    private TownConditionCache(){}
    public static void put(int id,float engine,float body,float battery,float fuel,float maxFuel,float armor,float maxArmor){
        DATA.put(id,new Data(engine,body,battery,fuel,maxFuel,armor,maxArmor));
        Minecraft mc=Minecraft.getInstance();
        if(mc.level!=null && mc.level.getEntity(id) instanceof CarEntity car && car instanceof TownCarData data){
            data.town$setEngine(engine); data.town$setBody(body); data.town$setBattery(battery);
            data.town$setMaxFuel(maxFuel); data.town$setFuel(fuel); data.town$setArmor(armor);
        }
    }
    public static Data get(int id){return DATA.get(id);}
}
