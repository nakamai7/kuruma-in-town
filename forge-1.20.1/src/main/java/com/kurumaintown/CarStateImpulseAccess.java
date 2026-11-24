package com.kurumaintown;

/** Public bridge to Kuruma's CarState velocity for collision impulses. */
public interface CarStateImpulseAccess {
    void town$addCollisionVelocity(double worldX, double worldZ);
    void town$addCollisionYawImpulse(double yawDelta);
    double town$getCollisionYawRate();
}
