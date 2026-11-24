package com.kurumaintown;

import com.jdmmc.kurumamod.entity.CarEntity;
import com.jdmmc.kurumamod.physics.CarHull;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Player-only response to Kuruma's actual CarHull SAT contact. */
@Mod.EventBusSubscriber(modid = KurumaInTown.MODID)
public final class TownPlayerCollision {
    private TownPlayerCollision() {}
    private static final double SIDE_MARGIN = 0.10D;
    private static final double ROOF_PROBE = 0.16D;
    private static final double PLAYER_MASS = 100.0D;
    private static final double PLAYER_RESTITUTION = TownCollisionPhysics.PLAYER_RESTITUTION;
    private static final double CAR_RESTITUTION = TownCollisionPhysics.CAR_RESTITUTION;
    // A single contact restitution is required by the impulse equation.
    private static final double PLAYER_CAR_RESTITUTION =
            TownCollisionPhysics.restitution(PLAYER_RESTITUTION, CAR_RESTITUTION);
    private static final double COLLISION_THRESHOLD = 0.015D;
    private static final java.util.Map<java.util.UUID, java.util.Map<java.util.UUID, Long>> IMPACT_TICKS =
            new java.util.HashMap<>();

    private static long lastImpactTick(Player player, CarEntity car) {
        return IMPACT_TICKS.getOrDefault(player.getUUID(), java.util.Collections.emptyMap())
                .getOrDefault(car.getUUID(), Long.MIN_VALUE);
    }
    private static void markImpactTick(Player player, CarEntity car) {
        IMPACT_TICKS.computeIfAbsent(player.getUUID(), ignored -> new java.util.HashMap<>())
                .put(car.getUUID(), player.level().getGameTime());
    }

    private static CarHull.Push feetContact(CarHull hull, AABB body, Player player) {
        AABB feet = player.getBoundingBox();
        // Only feet close to the roof may be grounded, not a player inside the car.
        if (feet.minY < body.maxY - 0.32D || feet.minY > body.maxY + 0.20D) return null;
        CarHull.Push push = hull.against(new double[] {
                feet.minX, feet.minY - ROOF_PROBE, feet.minZ,
                feet.maxX, feet.minY + 0.025D, feet.maxZ
        });
        // The SAT normal points in the direction the car would be moved.
        // An upward-facing roof produces a downward car separation normal.
        if (push == null || push.depth() <= 1.0E-7D || push.ny() > -0.45D) return null;
        return push;
    }

    /**
     * Horizontal side normal from Kuruma's CarHull SAT result. If SAT selects
     * the roof/floor axis for a partly embedded player, use the closest
     * horizontal face of the hull's bounds instead of skipping collision.
     */
    private static Vec3 sideNormal(CarHull.Push contact, AABB hullBox, AABB playerBox) {
        double nx = -contact.nx(), nz = -contact.nz();
        double length = Math.hypot(nx, nz);
        if (length > 1.0E-4D) return new Vec3(nx / length, 0.0D, nz / length);

        double west = Math.abs(playerBox.maxX - hullBox.minX);
        double east = Math.abs(hullBox.maxX - playerBox.minX);
        double north = Math.abs(playerBox.maxZ - hullBox.minZ);
        double south = Math.abs(hullBox.maxZ - playerBox.minZ);
        double nearest = Math.min(Math.min(west, east), Math.min(north, south));
        if (nearest == west) return new Vec3(-1.0D, 0.0D, 0.0D);
        if (nearest == east) return new Vec3(1.0D, 0.0D, 0.0D);
        if (nearest == north) return new Vec3(0.0D, 0.0D, -1.0D);
        return new Vec3(0.0D, 0.0D, 1.0D);
    }

    /**
     * Remove ALL motion perpendicular to the contacted side (both signs),
     * preserving the exact tangent. When the real player box penetrates the
     * hull, add outward motion after the projection. No teleport is used.
     */
    private static void applySide(CarEntity car, CarHull hull, AABB hullBox, Player player, boolean pushOut) {
        if (feetContact(hull, hullBox, player) != null) return;
        AABB box = player.getBoundingBox();
        CarHull.Push proximity = hull.against(new double[] {
                box.minX - SIDE_MARGIN, box.minY, box.minZ - SIDE_MARGIN,
                box.maxX + SIDE_MARGIN, box.maxY, box.maxZ + SIDE_MARGIN
        });
        if (proximity == null || proximity.depth() <= 1.0E-7D) return;

        CarHull.Push penetration = hull.against(new double[] {
                box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ
        });
        boolean inside = penetration != null && penetration.depth() > 1.0E-5D;
        Vec3 normal = sideNormal(inside ? penetration : proximity, hullBox, box);
        Vec3 v = player.getDeltaMovement();
        double normalSpeed = v.x * normal.x + v.z * normal.z;
        double vx = v.x - normalSpeed * normal.x;
        double vz = v.z - normalSpeed * normal.z;

        // Resolve the incoming relative normal velocity with a 1D impulse.
        // The normal points from the hull toward the player; tangent is unchanged.
        // Avoid applying vehicle impulses on the client or multiple times per tick.
        double carNormalSpeed = car.worldVelocityX() * normal.x + car.worldVelocityZ() * normal.z;
        double closingSpeed = carNormalSpeed - normalSpeed;
        if (pushOut && !player.level().isClientSide &&
                player.level().getGameTime() != lastImpactTick(player, car) &&
                closingSpeed > COLLISION_THRESHOLD) {
            markImpactTick(player, car);
            double carMass = Math.max(1.0D, car.getSpec().mass());
            double impulse = TownCollisionPhysics.impulse(closingSpeed,
                    1.0D / PLAYER_MASS, 1.0D / carMass, PLAYER_CAR_RESTITUTION);
            double playerDelta = impulse / PLAYER_MASS;
            double carDelta = -impulse / carMass;
            vx += normal.x * playerDelta;
            vz += normal.z * playerDelta;
            // CarEntity uses its own CarState, not Entity.setDeltaMovement().
            ((com.kurumaintown.CarStateImpulseAccess)(Object)car)
                    .town$addCollisionVelocity(normal.x * carDelta, normal.z * carDelta);
        }

        if (inside && pushOut) {
            // Penetration recovery uses only outward motion; tangent stays
            // untouched. This also works when SAT selects a vertical axis.
            double overlapX = Math.min(box.maxX, hullBox.maxX) - Math.max(box.minX, hullBox.minX);
            double overlapZ = Math.min(box.maxZ, hullBox.maxZ) - Math.max(box.minZ, hullBox.minZ);
            double depth = Math.max(0.0D, Math.min(overlapX, overlapZ));
            double outward = Math.min(0.08D, Math.max(0.012D,
                    Math.max(depth, penetration.depth()) * 0.20D));
            vx += normal.x * outward;
            vz += normal.z * outward;
            // Motion alone is applied after collision has already occurred.
            // Recover existing overlap immediately as well; otherwise a held
            // movement key can carry the player deeper before the next tick.
            // Player.move is collision-aware and does not teleport the camera.
            double clearance = Math.min(0.08D, Math.max(0.0D, depth) * 0.25D + 0.003D);
            if (clearance > 0.0D) {
                player.move(MoverType.SELF, new Vec3(normal.x * clearance, 0.0D,
                        normal.z * clearance));
            }
        }
        if (Math.abs(vx - v.x) > 1.0E-8D || Math.abs(vz - v.z) > 1.0E-8D) {
            player.setDeltaMovement(vx, v.y, vz);
            player.hasImpulse = true;
            if (!player.level().isClientSide) player.hurtMarked = true;
        }
    }

    /** Called each vehicle tick; only the player is affected. */
    public static void resolveSide(CarEntity car) {
        if (car.isRemoved() || car.level().isClientSide) return;
        CarHull hull = car.currentBody();
        double[] b = hull.bounds();
        AABB body = new AABB(b[0], b[1], b[2], b[3], b[4], b[5]);
        for (Player player : car.level().getEntitiesOfClass(Player.class, body.inflate(3.0D))) {
            if (player.isSpectator() || player.isPassenger()) continue;
            applySide(car, hull, body, player, true);
        }
    }

    /** Run before vanilla player movement, including the local client. */
    @SubscribeEvent
    public static void onPlayerTickStart(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        Player player = event.player;
        if (player.isSpectator() || player.isPassenger() || !player.isAlive()) return;
        AABB box = player.getBoundingBox();
        for (CarEntity car : player.level().getEntitiesOfClass(CarEntity.class, box.inflate(3.0D))) {
            if (car.isRemoved()) continue;
            CarHull hull = car.currentBody();
            double[] b = hull.bounds();
            AABB body = new AABB(b[0], b[1], b[2], b[3], b[4], b[5]);
            applySide(car, hull, body, player, !player.level().isClientSide);
        }
    }

    /** Player tick end: restore ground contact after vanilla player movement. */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (player.isSpectator() || player.isPassenger() || !player.isAlive()) return;
        AABB feet = player.getBoundingBox();
        AABB query = feet.inflate(3.0D, 0.5D, 3.0D);
        // Movement input is applied by vanilla between START and END.
        // Recheck the NEW bounding box here, even when the player is jumping.
        for (CarEntity car : player.level().getEntitiesOfClass(CarEntity.class, query)) {
            if (car.isRemoved()) continue;
            CarHull hull = car.currentBody();
            double[] b = hull.bounds();
            AABB body = new AABB(b[0], b[1], b[2], b[3], b[4], b[5]);
            applySide(car, hull, body, player, true);
        }
        Vec3 velocity = player.getDeltaMovement();
        // Never latch the player to the roof during a jump.
        if (velocity.y > 0.015D) return;
        feet = player.getBoundingBox();
        for (CarEntity car : player.level().getEntitiesOfClass(CarEntity.class, query)) {
            if (car.isRemoved()) continue;
            CarHull hull = car.currentBody();
            double[] b = hull.bounds();
            AABB body = new AABB(b[0], b[1], b[2], b[3], b[4], b[5]);
            if (feetContact(hull, body, player) == null) continue;
            // Move only the small vertical gap. Never teleport or affect X/Z.
            double rise = body.maxY - feet.minY + 0.002D;
            if (rise > 0.0D && rise <= 0.22D) {
                player.move(MoverType.SELF, new Vec3(0.0D, rise, 0.0D));
            }
            player.setDeltaMovement(velocity.x, Math.max(0.0D, velocity.y), velocity.z);
            player.setOnGround(true);
            player.fallDistance = 0.0F;
            player.hasImpulse = true;
            return;
        }
    }
}
