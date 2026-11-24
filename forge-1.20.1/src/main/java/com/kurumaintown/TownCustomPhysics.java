package com.kurumaintown;

import com.jdmmc.kurumamod.physics.CarSpec;

/** Apply vehicle upgrades to the specification consumed by CarCore physics. */
public final class TownCustomPhysics {
    private TownCustomPhysics() {}

    public static CarSpec apply(CarSpec spec, TownCarData data) {
        if (spec == null || data == null) return spec;
        int engine = data.town$getCustom("enginecustom");
        int steering = data.town$getCustom("steeringcustom");
        int suspension = data.town$getCustom("suspensioncustom");
        int brake = data.town$getCustom("brakecustom");
        if (engine == 0 && steering == 0 && suspension == 0 && brake == 0) return spec;
        var b = spec.toBuilder();
        if (engine > 0) {
            b.peakTorque(spec.peakTorque() * (1.0 + engine));
            b.peakTorqueRpm(spec.peakTorqueRpm() * (1.0 + engine));
            b.peakPowerRpm(spec.peakPowerRpm() * (1.0 + engine));
            b.redlineRpm(spec.redlineRpm() * (1.0 + 0.30 * engine));
            b.gearCount(spec.gearCount() + engine);
            b.firstGearRatio(spec.firstGearRatio() * (1.0 + 0.15 * engine));
            b.shiftSeconds(Math.max(0.01, spec.shiftSeconds() - 0.05 * engine));
        }
        if (steering > 0) b.steerGripMargin(spec.steerGripMargin() * (1.0 + 0.15 * steering));
        if (suspension > 0) {
            double factor = 1.0 + 0.15 * suspension;
            b.frontBumpDamping(spec.frontBumpDamping() * factor);
            b.rearBumpDamping(spec.rearBumpDamping() * factor);
            b.frontReboundDamping(spec.frontReboundDamping() * factor);
            b.rearReboundDamping(spec.rearReboundDamping() * factor);
        }
        if (brake > 0) {
            b.tireFriction(spec.tireFriction() * (1.0 + 0.20 * brake));
            b.longitudinalStiffness(spec.longitudinalStiffness() * (1.0 + 0.20 * brake));
        }
        return b.build();
    }
}
