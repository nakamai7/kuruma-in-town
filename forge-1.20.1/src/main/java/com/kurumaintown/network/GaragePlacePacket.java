package com.kurumaintown.network;
import com.kurumaintown.TownItems;
import com.kurumaintown.TownJobStorage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
public record GaragePlacePacket(boolean job) {
    public GaragePlacePacket(FriendlyByteBuf b){this(b.readBoolean());}
    public void encode(FriendlyByteBuf b){b.writeBoolean(job);}
    public void handle(Supplier<NetworkEvent.Context> supplier){
        NetworkEvent.Context ctx=supplier.get();
        ctx.enqueueWork(()->{
            ServerPlayer p=ctx.getSender();
            if(p==null || !(p.getMainHandItem().is(TownItems.GARAGE.get()) || p.getOffhandItem().is(TownItems.GARAGE.get())))return;
            int jobId=job?TownJobStorage.getPlayerJob(p):0;
            if(job && jobId==0){p.displayClientMessage(Component.literal("jobガレージにはjob 1または2が必要です"),true);return;}
            // Place at the player's feet with initial marker yaw fixed to zero.
            Marker marker = EntityType.MARKER.create(p.serverLevel());
            if (marker == null) return;
            marker.moveTo(p.getX(), p.getY(), p.getZ(), 0.0F, 0.0F);
            marker.addTag("cargarage");
            p.serverLevel().addFreshEntity(marker);
            Objective obj=TownJobStorage.getOrCreateJobObjective(p.getServer());
            Score score=p.getServer().getScoreboard().getOrCreatePlayerScore(marker.getScoreboardName(),obj);
            score.setScore(jobId);
            p.displayClientMessage(Component.literal(jobId==0?"ガレージを設置しました":"jobガレージを設置しました (job "+jobId+")"),true);
        });ctx.setPacketHandled(true);
    }
}
