package com.kurumaintown;

/** Normal impulse for a frictionless collision. Velocities use blocks/tick. */
public final class TownCollisionPhysics {
    private TownCollisionPhysics() {}
    public static final double CAR_RESTITUTION = 0.90D;
    public static final double PLAYER_RESTITUTION = 0.80D;
    public static final double BLOCK_RESTITUTION = 0.90D;
    public static final double PLAYER_MASS = 100.0D;

    /** The effective coefficient for two colliding objects. */
    public static double restitution(double a, double b) {
        return Math.min(1.0D, Math.max(0.0D, (a + b) * 0.5D));
    }

    /**
     * Positive impulse magnitude when the objects approach each other.
     * normal points from A to B; relative closing speed is (vA-vB).normal.
     * An immovable block has inverseMassB=0.
     */
    public static double impulse(double closingSpeed, double inverseMassA,
                                 double inverseMassB, double restitution) {
        double totalInverseMass = inverseMassA + inverseMassB;
        if (!Double.isFinite(closingSpeed) || closingSpeed <= 0.0D ||
                totalInverseMass <= 0.0D) return 0.0D;
        return (1.0D + restitution) * closingSpeed / totalInverseMass;
    }
}
