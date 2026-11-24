package com.kurumaintown;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;

/** Shared job garage storage and the job dummy scoreboard objective. */
public final class TownJobStorage {
    private static final ResourceLocation STORAGE_ID = new ResourceLocation("kuruma_in_town", "storage");
    public static final String ZERO_OWNER = "00000000-0000-0000-0000-000000000000";
    private TownJobStorage() {}

    public static Objective getOrCreateJobObjective(MinecraftServer server) {
        Scoreboard board = server.getScoreboard();
        Objective objective = board.getObjective("job");
        if (objective == null) objective = board.addObjective("job", ObjectiveCriteria.DUMMY, Component.literal("job"), ObjectiveCriteria.RenderType.INTEGER);
        return objective;
    }

    public static int getPlayerJob(ServerPlayer player) {
        Objective objective = getOrCreateJobObjective(player.getServer());
        Score score = player.getServer().getScoreboard().getOrCreatePlayerScore(player.getScoreboardName(), objective);
        return Math.max(0, Math.min(2, score.getScore()));
    }

    public static CompoundTag getStorage(MinecraftServer server) {
        return server.getCommandStorage().get(STORAGE_ID);
    }

    public static void setStorage(MinecraftServer server, CompoundTag storage) {
        server.getCommandStorage().set(STORAGE_ID, storage);
    }

    public static String keyForJob(int job) { return job == 1 ? "job1" : job == 2 ? "job2" : ""; }

    public static CompoundTag getJobList(MinecraftServer server, int job) {
        String key = keyForJob(job);
        if (key.isEmpty()) return new CompoundTag();
        return getStorage(server).getCompound(key).copy();
    }

    public static void putJobList(MinecraftServer server, int job, CompoundTag list) {
        String key = keyForJob(job);
        if (key.isEmpty()) return;
        CompoundTag storage = getStorage(server);
        storage.put(key, list.copy());
        setStorage(server, storage);
    }

    /** Marker job is stored in the same dummy scoreboard objective used by players. */
    public static int markerJob(net.minecraft.world.entity.Entity marker) {
        MinecraftServer server = marker.level().getServer();
        if (server == null) return 0;
        Objective objective = getOrCreateJobObjective(server);
        Scoreboard board = server.getScoreboard();
        Score score = board.getOrCreatePlayerScore(marker.getScoreboardName(), objective);
        int job = score.getScore();
        if (job < 0 || job > 2) {
            job = 0;
            score.setScore(0);
        }
        return job;
    }

    /** All job garages are interchangeable; legacy marker job scores 1/2 still work. */
    public static boolean isJobGarage(net.minecraft.world.entity.Entity marker) {
        return marker.getTags().contains("job") || markerJob(marker) == 1 || markerJob(marker) == 2;
    }

    public static int vehicleJob(CompoundTag entry) {
        CompoundTag vehicle = entry.getCompound("vehicledata");
        CompoundTag town = vehicle.getCompound("town");
        int job;
        if (town.contains("job")) job = town.getInt("job");
        else if (town.getCompound("cardata").contains("job")) job = town.getCompound("cardata").getInt("job");
        else job = entry.getCompound("cardata").getInt("job");
        return Math.max(0, Math.min(2, job));
    }

    public static CompoundTag filterList(CompoundTag source, int job) {
        CompoundTag result = new CompoundTag();
        for (String key : source.getAllKeys()) {
            CompoundTag entry = source.getCompound(key);
            if (vehicleJob(entry) == job) result.put(key, entry.copy());
        }
        return result;
    }

    public static void initializePlayer(ServerPlayer player) {
        Objective objective = getOrCreateJobObjective(player.getServer());
        Scoreboard board = player.getServer().getScoreboard();
        // A newly created player's score defaults to 0; preserve any existing 1/2 assignment.
        board.getOrCreatePlayerScore(player.getScoreboardName(), objective);
    }
}
