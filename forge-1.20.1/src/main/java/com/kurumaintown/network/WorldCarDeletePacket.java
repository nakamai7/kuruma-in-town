package com.kurumaintown.network;

import com.kurumaintown.GarageAdminService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Delete only the world registry entry, never the actual vehicle or garage record. */
public record WorldCarDeletePacket(String carnumber) {
    public WorldCarDeletePacket(FriendlyByteBuf b) { this(b.readUtf(64)); }
    public void encode(FriendlyByteBuf b) { b.writeUtf(carnumber); }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer admin = ctx.getSender();
            if (admin == null || !GarageAdminService.authorized(admin) || !admin.isShiftKeyDown()) return;
            if (!carnumber.matches("[A-Z]{4}[0-9]{4}")) return;
            ResourceLocation key = new ResourceLocation("kuruma_in_town", "storage");
            CompoundTag storage = admin.getServer().getCommandStorage().get(key);
            CompoundTag registry = storage.getCompound("carlist");
            if (!registry.contains(carnumber, 10)) return;
            registry.remove(carnumber);
            storage.put("carlist", registry);
            admin.getServer().getCommandStorage().set(key, storage);
            GarageAdminService.openRegistry(admin);
        });
        ctx.setPacketHandled(true);
    }
}
