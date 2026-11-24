package com.kurumaintown.client;
import com.kurumaintown.KurumaInTown;
import com.kurumaintown.TownItems;
import com.kurumaintown.network.GarageMarkerSyncPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.List;
@Mod.EventBusSubscriber(modid=KurumaInTown.MODID,value=Dist.CLIENT)
public final class GarageMarkerParticles {
    private static ResourceLocation dimension;
    private static List<GarageMarkerSyncPacket.Marker> markers=List.of();
    private static int ticks;
    private GarageMarkerParticles(){}
    public static GarageMarkerSyncPacket.Marker nearest(double x,double y,double z) {
        Minecraft mc=Minecraft.getInstance();
        if(mc.level==null || dimension==null || !dimension.equals(mc.level.dimension().location()))return null;
        GarageMarkerSyncPacket.Marker best=null; double bestDistance=9.0;
        for(var m:markers){double dx=m.x()-x,dy=m.y()-y,dz=m.z()-z;double d=dx*dx+dy*dy+dz*dz;
            if(d<=bestDistance){best=m;bestDistance=d;}}
        return best;
    }
    public static void update(ResourceLocation d,List<GarageMarkerSyncPacket.Marker> m){dimension=d;markers=List.copyOf(m);}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END || ++ticks%5!=0)return;
        Minecraft mc=Minecraft.getInstance();
        if(mc.level==null || mc.player==null || dimension==null || !dimension.equals(mc.level.dimension().location()))return;
        if(!mc.player.getMainHandItem().is(TownItems.GARAGE.get()) && !mc.player.getOffhandItem().is(TownItems.GARAGE.get()))return;
        for(var m:markers){
            if(mc.player.distanceToSqr(m.x(),m.y(),m.z())>48*48)continue;
            for(int i=0;i<36;i++){
                double a=2*Math.PI*i/36.0;
                mc.level.addParticle(ParticleTypes.END_ROD,m.x()+3*Math.cos(a),m.y()+0.12,m.z()+3*Math.sin(a),0,0,0);
            }
            double a=Math.toRadians(m.yaw());
            for(int i=1;i<=4;i++){
                double d=i*0.3;
                mc.level.addParticle(ParticleTypes.HAPPY_VILLAGER,m.x()-Math.sin(a)*d,m.y()+0.15,m.z()+Math.cos(a)*d,0,0,0);
            }
        }
    }
}
