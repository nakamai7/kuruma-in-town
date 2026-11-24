package com.kurumaintown;

import com.jdmmc.kurumamod.entity.CarEntity;
import com.jdmmc.kurumamod.physics.CarHull;
import net.minecraft.world.phys.AABB;

/** Server-side pairwise vehicle collision impulses, using Kuruma's CarHull. */
public final class TownCarCollision {
    private TownCarCollision() {}
    private static final double MIN_CLOSING_SPEED = 0.03D;

    public static void resolve(CarEntity car) {
        if (car.level().isClientSide || car.isRemoved()) return;
        CarHull hull = car.currentBody();
        double[] b = hull.bounds();
        AABB box = new AABB(b[0], b[1], b[2], b[3], b[4], b[5]);
        for (CarEntity other : car.level().getEntitiesOfClass(CarEntity.class, box.inflate(2.0D))) {
            // Resolve each unordered pair only once, irrespective of entity tick order.
            if (other == car || other.isRemoved() || car.getId() >= other.getId()) continue;
            CarHull otherHull = other.currentBody();
            double[] ob = otherHull.bounds();
            AABB otherBox = new AABB(ob[0], ob[1], ob[2], ob[3], ob[4], ob[5]);
            if (!box.intersects(otherBox)) continue;
            CarHull.Push contact = hull.against(ob);
            CarHull.Push reverse = otherHull.against(b);
            if ((contact == null || contact.depth() <= 1.0E-6D) &&
                (reverse == null || reverse.depth() <= 1.0E-6D)) continue;

            // Horizontal contact normal from this vehicle toward the other.
            // Use the hull contact axis where possible, then centers as a fallback.
            double nx = otherBox.getCenter().x - box.getCenter().x;
            double nz = otherBox.getCenter().z - box.getCenter().z;
            CarHull.Push push = contact != null && contact.depth() > 1.0E-6D ? contact : reverse;
            double px = push.nx(), pz = push.nz();
            double pl = Math.hypot(px, pz);
            if (pl > 1.0E-5D) {
                px /= pl; pz /= pl;
                if (px * nx + pz * nz < 0.0D) { px = -px; pz = -pz; }
                nx = px; nz = pz;
            } else {
                double length = Math.hypot(nx, nz);
                if (length < 1.0E-6D) continue;
                nx /= length; nz /= length;
            }
            // Approximate the contact point from the overlap of the two hull AABBs.
            // An off-center hit produces both linear impulse and a yaw impulse.
            double cx = (Math.max(box.minX, otherBox.minX) + Math.min(box.maxX, otherBox.maxX)) * 0.5D;
            double cz = (Math.max(box.minZ, otherBox.minZ) + Math.min(box.maxZ, otherBox.maxZ)) * 0.5D;
            double r1x = cx - car.getX(), r1z = cz - car.getZ();
            double r2x = cx - other.getX(), r2z = cz - other.getZ();
            double cross1 = r1x * nz - r1z * nx;
            double cross2 = r2x * nz - r2z * nx;
            double m1 = Math.max(1.0D, car.getSpec().mass());
            double m2 = Math.max(1.0D, other.getSpec().mass());
            // Rectangle yaw moment of inertia around the vehicle center.
            double w1 = Math.max(0.5D, box.maxX - box.minX);
            double l1 = Math.max(0.5D, box.maxZ - box.minZ);
            double w2 = Math.max(0.5D, otherBox.maxX - otherBox.minX);
            double l2 = Math.max(0.5D, otherBox.maxZ - otherBox.minZ);
            double i1 = Math.max(1.0D, m1 * (w1 * w1 + l1 * l1) / 12.0D);
            double i2 = Math.max(1.0D, m2 * (w2 * w2 + l2 * l2) / 12.0D);
            CarStateImpulseAccess a = (CarStateImpulseAccess)(Object)car;
            CarStateImpulseAccess bAccess = (CarStateImpulseAccess)(Object)other;
            // Contact point velocity includes the tangential velocity from yaw.
            // Yaw rate and world velocity use the same simulation time basis.
            double v1x = car.worldVelocityX() - a.town$getCollisionYawRate() * r1z;
            double v1z = car.worldVelocityZ() + a.town$getCollisionYawRate() * r1x;
            double v2x = other.worldVelocityX() - bAccess.town$getCollisionYawRate() * r2z;
            double v2z = other.worldVelocityZ() + bAccess.town$getCollisionYawRate() * r2x;
            double closing = (v1x - v2x) * nx + (v1z - v2z) * nz;
            if (closing <= MIN_CLOSING_SPEED) continue;
            double invEffectiveMass = 1.0D / m1 + 1.0D / m2
                    + cross1 * cross1 / i1 + cross2 * cross2 / i2;
            double j = (1.0D + TownCollisionPhysics.CAR_RESTITUTION) * closing / invEffectiveMass;
            // Apply the impulse at the contact point, not at each vehicle's center.
            a.town$addCollisionVelocity(-nx * j / m1, -nz * j / m1);
            bAccess.town$addCollisionVelocity(nx * j / m2, nz * j / m2);
            a.town$addCollisionYawImpulse(-cross1 * j / i1);
            bAccess.town$addCollisionYawImpulse(cross2 * j / i2);
        }
    }
}
