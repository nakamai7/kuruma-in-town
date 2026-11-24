package com.kurumaintown;

import com.jdmmc.kurumamod.entity.CarEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import java.util.concurrent.ThreadLocalRandom;

/** Permanent world-wide registry of issued vehicle numbers. */
public final class TownCarRegistry {
    private static final ResourceLocation STORAGE = new ResourceLocation("kuruma_in_town", "storage");
    private TownCarRegistry() {}

    public static String issue(CarEntity car, String owner) {
        if (!(car.level() instanceof ServerLevel level)) return "00000000";
        MinecraftServer server = level.getServer();
        CompoundTag storage = server.getCommandStorage().get(STORAGE);
        CompoundTag list = storage.getCompound("carlist");
        String number;
        do {
            StringBuilder id = new StringBuilder(8);
            for (int i = 0; i < 4; i++) id.append((char)('A' + ThreadLocalRandom.current().nextInt(26)));
            for (int i = 0; i < 4; i++) id.append(ThreadLocalRandom.current().nextInt(10));
            number = id.toString();
        } while (list.contains(number));
        CompoundTag entry = new CompoundTag();
        entry.putString("cartype", car.getCarId().toString());
        entry.putString("owner", owner == null ? "" : owner);
        String ownerName = "";
        if (owner != null && !owner.isBlank()) {
            try {
                java.util.UUID uuid = java.util.UUID.fromString(owner);
                net.minecraft.server.level.ServerPlayer player = server.getPlayerList().getPlayer(uuid);
                if (player != null) ownerName = player.getGameProfile().getName();
                else {
                    com.mojang.authlib.GameProfile profile = server.getProfileCache() == null ? null : server.getProfileCache().get(uuid).orElse(null);
                    if (profile != null && profile.getName() != null) ownerName = profile.getName();
                }
            } catch (IllegalArgumentException ignored) {}
        }
        entry.putString("ownername", ownerName);
        list.put(number, entry);
        storage.put("carlist", list);
        server.getCommandStorage().set(STORAGE, storage);
        return number;
    }
}
