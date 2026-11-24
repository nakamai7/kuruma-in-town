package com.kurumaintown.mixin;

import com.kurumaintown.TownCarData;
import com.kurumaintown.TownCustomDefaults;
import com.kurumaintown.TownCollisionPhysics;
import com.kurumaintown.CarStateImpulseAccess;
import com.kurumaintown.TownNetwork;
import com.kurumaintown.network.TownConditionPacket;
import com.jdmmc.kurumamod.entity.CarEntity;
import com.jdmmc.kurumamod.physics.CarInput;
import com.jdmmc.kurumamod.physics.CarHull;
import com.jdmmc.kurumamod.physics.CarSpec;
import com.jdmmc.kurumamod.physics.CarState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.kurumaintown.TownCarRegistry;

@Mixin(CarEntity.class)
public abstract class CarEntityMixin implements TownCarData, CarStateImpulseAccess, com.kurumaintown.GarageYawAccess, com.kurumaintown.TownEngineState {
    @Shadow(remap=false) @Final private CarState state;
    @Unique private static final EntityDataAccessor<Integer> TOWN_CUSTOM_SYNC = SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.INT);
    @Unique private static final EntityDataAccessor<Integer> TOWN_BRAKE_SYNC = SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.INT);
    @Unique private static final EntityDataAccessor<Integer> TOWN_START_DELAY = SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.INT);
    @Unique private boolean town$hadDriver = false;
    @Unique private int town$startDelay = 0;
    @Unique private static final EntityDataAccessor<Integer> TOWN_PAINT = SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.INT);
    @Inject(method="defineSynchedData", at=@At("TAIL"))
    private void town$definePaint(CallbackInfo ci) { ((CarEntity)(Object)this).getEntityData().define(TOWN_START_DELAY, 0); ((CarEntity)(Object)this).getEntityData().define(TOWN_PAINT, -1); ((CarEntity)(Object)this).getEntityData().define(TOWN_CUSTOM_SYNC, 0); ((CarEntity)(Object)this).getEntityData().define(TOWN_BRAKE_SYNC, 0); }
    @Override public int town$getPaintColor() { return ((CarEntity)(Object)this).getEntityData().get(TOWN_PAINT); }
    @Override public void town$setPaintColor(int color) { ((CarEntity)(Object)this).getEntityData().set(TOWN_PAINT, (color >= 0 && color < 16) || (color <= -2 && color >= -0x1000001) ? color : -1); }
    @Unique private float town$engine = 100.0F;
    @Unique private float town$body = 100.0F;
    @Unique private float town$battery = 100.0F;
    @Unique private float town$fuel = 100.0F;
    @Unique private float town$maxFuel = 100.0F;
    @Unique private float town$baseFuel = 100.0F;
    @Unique private final int[] town$custom = new int[6];
    @Unique private final int[] town$customMax = TownCustomDefaults.maxValues();
    @Unique private float town$engineMulti = 1.0F;
    @Unique private float town$bodyMulti = 1.0F;
    @Unique private float town$batteryMulti = 1.0F;
    @Unique private float town$fuelMulti = 1.0F;
    @Unique private float town$armor = 0.0F;
    @Unique private String town$owner = "";
    @Unique private int town$job = 0;
    @Unique private int town$inGarage = 0;
    @Unique private String town$carNumber = "00000000";
    @Unique private double town$lastX = Double.NaN;
    @Unique private double town$lastZ = Double.NaN;
    @Unique private double town$lastSpeed = 0.0D;
    @Unique private int town$impactCooldown = 0;
    @Unique private int town$syncCounter = 9;

    @Override public void town$addCollisionVelocity(double worldX, double worldZ) {
        state.addWorldVelocity(worldX, worldZ);
    }

    @Override public void town$addCollisionYawImpulse(double yawDelta) {
        state.yawRate = Math.max(-0.65D, Math.min(0.65D, state.yawRate + yawDelta));
    }

    @Override public double town$getCollisionYawRate() { return state.yawRate; }

    @Override public void town$setGarageYaw(float yawDegrees) {
        // Kuruma drives its vehicle heading from CarState.yaw (radians).
        // setYRot alone is overwritten by the next physics update.
        state.yaw = Math.toRadians(yawDegrees);
        state.yawRate = 0.0D;
        CarEntity car = (CarEntity)(Object)this;
        car.setYRot(yawDegrees);
        car.setYHeadRot(yawDegrees);
        car.setYBodyRot(yawDegrees);
        car.yRotO = yawDegrees;
    }

    @Unique private double town$preBlockVX;
    @Unique private double town$preBlockVZ;

    @Unique private int town$calculateStartDelay() {
        double product = Math.min(10000.0D, Math.max(0.0D, town$engine) * Math.max(0.0D, town$battery));
        return Math.max(10, (int)Math.ceil(110.0D - product * 0.01D));
    }

    @Override public boolean town$isEngineRunning() { return town$engineReady(); }

    @Unique private boolean town$engineReady() {
        CarEntity car = (CarEntity)(Object)this;
        return car.getControllingPassenger() instanceof Player
                && car.getEntityData().get(TOWN_START_DELAY) <= 0
                && town$engine > 0.0F && town$battery > 0.0F && town$fuel > 0.0F;
    }

    @Inject(method="tick", at=@At("HEAD"))
    private void town$updateEngineState(CallbackInfo ci) {
        CarEntity car = (CarEntity)(Object)this;
        if (!car.level().isClientSide) {
            boolean driver = car.getControllingPassenger() instanceof Player;
            if (driver && !town$hadDriver) town$startDelay = town$calculateStartDelay();
            if (!driver) town$startDelay = 0;
            else if (town$startDelay > 0) --town$startDelay;
            town$hadDriver = driver;
            if (car.getEntityData().get(TOWN_START_DELAY) != town$startDelay)
                car.getEntityData().set(TOWN_START_DELAY, town$startDelay);
        }
        if (!town$engineReady()) car.setHeadlights(false);
    }

    @Inject(method="tick", at=@At("TAIL"))
    private void town$forceLightsOff(CallbackInfo ci) {
        CarEntity car = (CarEntity)(Object)this;
        if (!town$engineReady()) car.setHeadlights(false);
    }

    @Inject(method="tick", at=@At("HEAD"))
    private void town$recordBlockVelocity(CallbackInfo ci) {
        CarEntity car = (CarEntity)(Object)this;
        if (!car.level().isClientSide) {
            town$preBlockVX = car.worldVelocityX();
            town$preBlockVZ = car.worldVelocityZ();
        }
    }

    @Override public float town$getEngine(){return town$engine;}
    @Override public void town$setEngine(float v){town$engine=town$clamp(v);}
    @Override public float town$getBody(){return town$body;}
    @Override public void town$setBody(float v){town$body=town$clamp(v);}
    @Override public float town$getBattery(){return town$battery;}
    @Override public void town$setBattery(float v){town$battery=town$clamp(v);}
    @Override public float town$getFuel(){return town$fuel;}
    @Override public void town$setFuel(float v){town$fuel=town$clampFuel(v);}
    @Override public float town$getBaseFuel(){return town$baseFuel;}
    @Override public void town$setBaseFuel(float v){town$baseFuel=Math.max(0,Math.min(1_000_000,v));town$updateTank();}
    @Unique private void town$syncCustom() {
        int packed = (town$custom[0] & 127) | ((town$custom[1] & 127) << 7) | ((town$custom[2] & 127) << 14);
        ((CarEntity)(Object)this).getEntityData().set(TOWN_CUSTOM_SYNC, packed);
        ((CarEntity)(Object)this).getEntityData().set(TOWN_BRAKE_SYNC, town$custom[5]);
    }
    @Unique private int town$physicsCustom(int index) {
        if (((CarEntity)(Object)this).level().isClientSide && index == 5) return ((CarEntity)(Object)this).getEntityData().get(TOWN_BRAKE_SYNC);
        if (((CarEntity)(Object)this).level().isClientSide && index < 3) {
            return (((CarEntity)(Object)this).getEntityData().get(TOWN_CUSTOM_SYNC) >>> (7*index)) & 127;
        }
        return town$custom[index];
    }
    @Override public int town$getCustomMax(String key){for(int i=0;i<6;i++)if(TownCustomDefaults.key(i).equals(key))return town$customMax[i];return 0;}
    @Override public int town$getCustom(String key){for(int i=0;i<6;i++)if(TownCustomDefaults.key(i).equals(key))return town$physicsCustom(i);return 0;}
    @Override public void town$setCustomMax(String key,int value){for(int i=0;i<6;i++)if(TownCustomDefaults.key(i).equals(key)){town$customMax[i]=Math.max(0,Math.min(100,value));town$custom[i]=Math.min(town$custom[i],town$customMax[i]);town$syncCustom();if(i==3)town$updateTank();return;}}
    @Override public void town$setCustom(String key,int value){for(int i=0;i<6;i++)if(TownCustomDefaults.key(i).equals(key)){town$custom[i]=Math.max(0,Math.min(town$customMax[i],value));town$syncCustom();if(i==3)town$updateTank();return;}}
    @Unique private void town$updateTank(){town$maxFuel=Math.max(0,Math.min(1_000_000,town$baseFuel+10F*town$custom[3]));town$fuel=Math.min(town$fuel,town$maxFuel);}
    @Override public float town$getMaxFuel(){return town$maxFuel;}
    @Override public void town$setMaxFuel(float v){town$maxFuel=Math.max(0.0F,Math.min(1_000_000.0F,v)); town$fuel=Math.min(town$fuel,town$maxFuel);}
    @Override public float town$getEngineMulti(){return town$engineMulti;}
    @Override public void town$setEngineMulti(float v){town$engineMulti=Math.max(0.0F,Math.min(1.0F,v));}
    @Override public float town$getBodyMulti(){return town$bodyMulti;}
    @Override public void town$setBodyMulti(float v){town$bodyMulti=Math.max(0.0F,Math.min(1.0F,v));}
    @Override public float town$getBatteryMulti(){return town$batteryMulti;}
    @Override public void town$setBatteryMulti(float v){town$batteryMulti=Math.max(0.0F,Math.min(1.0F,v));}
    @Override public float town$getFuelMulti(){return town$fuelMulti;}
    @Override public void town$setFuelMulti(float v){town$fuelMulti=Math.max(0.0F,Math.min(2.0F,v));}
    @Override public float town$getArmor(){return town$armor;}
    @Override public void town$setArmor(float v){town$custom[4]=Math.max(0,Math.min(town$customMax[4],Math.round(v)));town$armor=town$custom[4];}
    @Override public String town$getOwner(){return town$owner;}
    @Override public void town$setOwner(String v){town$owner=v==null?"":v;}
    @Override public int town$getJob(){return town$job;}
    @Override public void town$setJob(int v){town$job=(v>=0&&v<=2)?v:0;}
    @Override public int town$getInGarage(){return town$inGarage;}
    @Override public void town$setInGarage(int v){town$inGarage=v==1?1:0;}
    @Override public String town$getCarNumber(){return town$carNumber;}
    @Override public void town$setCarNumber(String v){town$carNumber=(v==null||v.isBlank())?"00000000":v;}

    @Unique private static float town$clamp(float v){return Math.max(0.0F,Math.min(100.0F,v));}
    @Unique private float town$clampFuel(float v){return Math.max(0.0F,Math.min(town$maxFuel,v));}
    @Unique public void town$claim(String uuid) {
        if (town$job != 0) {
            town$owner = "00000000-0000-0000-0000-000000000000";
            return;
        }
        if (town$owner.isEmpty()) {
            town$owner = uuid;
            town$carNumber = TownCarRegistry.issue((CarEntity)(Object)this, town$owner);
        }
    }

    @Inject(method="readAdditionalSaveData", at=@At("TAIL"))
    private void town$read(CompoundTag tag, CallbackInfo ci) {
        CompoundTag town=tag.getCompound("town");
        CompoundTag data=town.getCompound("cardata");
        town$job=town.contains("job")?Math.max(0,Math.min(2,town.getInt("job"))):(data.contains("job")?Math.max(0,Math.min(2,data.getInt("job"))):0);
        town$owner=town.getTagType("owner")==net.minecraft.nbt.Tag.TAG_INT_ARRAY
                ? "00000000-0000-0000-0000-000000000000" : town.getString("owner");
        if (town$job != 0) town$owner="00000000-0000-0000-0000-000000000000";
        town$inGarage=town.getInt("ingarage");
        town$carNumber=town.contains("carnumber")?town.getString("carnumber"):"00000000";
        town$engine=data.contains("enginelevel")?data.getFloat("enginelevel"):100.0F;
        town$body=data.contains("bodylevel")?data.getFloat("bodylevel"):100.0F;
        town$battery=data.contains("batterylevel")?data.getFloat("batterylevel"):100.0F;
        town$baseFuel=data.contains("basefuellevel")?Math.max(0,Math.min(1_000_000,data.getFloat("basefuellevel"))):(data.contains("maxfuellevel")?Math.max(0,Math.min(1_000_000,data.getFloat("maxfuellevel"))):100F);
        CompoundTag custom=data.getCompound("custom");
        for(int i=0;i<6;i++)town$customMax[i]=Math.max(0,Math.min(100,custom.contains("max"+TownCustomDefaults.key(i))?custom.getInt("max"+TownCustomDefaults.key(i)):(i==4?5:3)));
        for(int i=0;i<6;i++)town$custom[i]=Math.max(0,Math.min(town$customMax[i],custom.contains(TownCustomDefaults.key(i))?custom.getInt(TownCustomDefaults.key(i)):(i==4?Math.round(data.getFloat("armorlevel")):0)));
        town$armor=town$custom[4];
        town$syncCustom();
        town$updateTank();
        town$fuel=data.contains("fuellevel")?town$clampFuel(data.getFloat("fuellevel")):town$maxFuel;
        town$engineMulti=data.contains("enginemulti")?data.getFloat("enginemulti"):1.0F;
        town$bodyMulti=data.contains("bodymulti")?data.getFloat("bodymulti"):1.0F;
        town$batteryMulti=data.contains("batterymulti")?data.getFloat("batterymulti"):1.0F;
        town$fuelMulti=data.contains("fuelmulti")?data.getFloat("fuelmulti"):1.0F;

        town$setPaintColor(data.contains("paintcolor") ? data.getInt("paintcolor") : -1);
    }

    @Inject(method="addAdditionalSaveData", at=@At("TAIL"))
    private void town$write(CompoundTag tag, CallbackInfo ci) {
        CompoundTag town=new CompoundTag();
        if (town$job == 1 || town$job == 2) town.putIntArray("owner", new int[]{0,0,0,0});
        else town.putString("owner",town$owner);
        town.putInt("ingarage",town$inGarage);
        town.putInt("job",town$job);
        town.putString("carnumber",town$carNumber);
        CompoundTag data=new CompoundTag();
        data.putFloat("enginelevel",town$engine);
        data.putFloat("bodylevel",town$body);
        data.putFloat("batterylevel",town$battery);
        data.putFloat("fuellevel",town$fuel);
        data.putFloat("basefuellevel",town$baseFuel);
        data.putFloat("maxfuellevel",town$maxFuel);
        CompoundTag custom=new CompoundTag();
        for(int i=0;i<6;i++){custom.putInt("max"+TownCustomDefaults.key(i),town$customMax[i]);custom.putInt(TownCustomDefaults.key(i),town$custom[i]);}
        data.put("custom",custom);
        data.putFloat("enginemulti",town$engineMulti);
        data.putFloat("bodymulti",town$bodyMulti);
        data.putFloat("batterymulti",town$batteryMulti);
        data.putFloat("fuelmulti",town$fuelMulti);

        data.putInt("paintcolor",town$getPaintColor());
        town.put("cardata",data);
        tag.put("town",town);
    }

    // physicsTick already calls getSpec() (verified in Kuruma 1.10.0 bytecode).
    // The getSpec RETURN injector above modifies the same spec used by CarPhysics.step.

    // Kuruma 2.1.0 moved physicsTick from CarEntity to CarCore.
    // Intercept the input at CarEntity.tick's delegation to CarCore instead.
    @org.spongepowered.asm.mixin.injection.ModifyArg(
        method="tick",
        at=@At(value="INVOKE", target="Lcom/jdmmc/kurumamod/car/CarCore;physicsTick(Lcom/jdmmc/kurumamod/physics/CarInput;)V", remap=false),
        index=0)
    private CarInput town$gatePhysicsInput(CarInput input) {
        if (state.gear == -1 && !input.brake() && (!input.analog() || input.brakeAxis() <= 0.001D)) {
            state.brake = 0.0D;
            state.throttle = 0.0D;
        }
        return town$engineReady() ? input : CarInput.NONE;
    }

    @Inject(method="interact(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;", at=@At("HEAD"), cancellable=true)
    private void town$blockBoarding(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (town$body<=0.0F) {
            if (!player.level().isClientSide) player.displayClientMessage(net.minecraft.network.chat.Component.literal("この車はボディが破損しているため乗車できません"), true);
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(method="tick", at=@At("TAIL"))
    private void town$tick(CallbackInfo ci) {
        CarEntity car=(CarEntity)(Object)this;
        if (!car.level().isClientSide) {
            town$applyBlockRestitution(car);
            com.kurumaintown.TownCarCollision.resolve(car);
        }
        town$resolvePlayerCollision(car);
        if (car.level().isClientSide) return;
        if (town$impactCooldown>0) town$impactCooldown--;
        double x=car.getX(), z=car.getZ();
        if (Double.isNaN(town$lastX)) { town$lastX=x; town$lastZ=z; }
        double distance=Math.sqrt((x-town$lastX)*(x-town$lastX)+(z-town$lastZ)*(z-town$lastZ));
        if (!car.getPassengers().isEmpty() && town$fuel>0.0F) town$fuel=town$clampFuel(town$fuel-(float)(distance*0.0005D*town$fuelMulti));
        double speed=Math.sqrt(car.worldVelocityX()*car.worldVelocityX()+car.worldVelocityZ()*car.worldVelocityZ());
        double loss=town$lastSpeed-speed;
        if (loss>2.5D && town$impactCooldown<=0) {
            double kmh=Math.max(0.0D,town$lastSpeed*3.6D-8.0D);
            float damage=(float)Math.max(1.0D,Math.min(35.0D,kmh*kmh*0.0015D));
            float armor=Math.max(0.0F,town$armor);
            town$engine=town$clamp(town$engine-damage*0.7F*town$engineMulti*(1.0F-Math.min(0.95F,armor*0.07F)));
            town$body=town$clamp(town$body-damage*0.9F*town$bodyMulti*(1.0F-Math.min(0.95F,armor*0.09F)));
            town$battery=town$clamp(town$battery-damage*0.5F*town$batteryMulti*(1.0F-Math.min(0.95F,armor*0.05F)));
            town$impactCooldown=10;
            // Strong deceleration stalls the engine and requires a restart.
            if (loss >= 4.0D && car.getControllingPassenger() instanceof Player) {
                town$startDelay = Math.max(town$startDelay, town$calculateStartDelay());
                car.getEntityData().set(TOWN_START_DELAY, town$startDelay);
                car.setHeadlights(false);
            }
        }
        town$lastX=x; town$lastZ=z; town$lastSpeed=speed;
        if (++town$syncCounter>=10) {
            town$syncCounter=0;
            TownConditionPacket packet=new TownConditionPacket(car.getId(),town$engine,town$body,town$battery,town$fuel,town$maxFuel,town$custom[4],town$customMax[4]);
            TownNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> car),packet);
            for (Entity passenger : car.getPassengers()) {
                if (passenger instanceof ServerPlayer serverPlayer) {
                    TownNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),packet);
                }
            }
        }
    }

    /** Block is an immovable body (inverse mass zero); preserve tangential velocity. */
    @Unique private void town$applyBlockRestitution(CarEntity car) {
        if (!car.horizontalCollision) return;
        double beforeX = town$preBlockVX, beforeZ = town$preBlockVZ;
        double afterX = car.worldVelocityX(), afterZ = car.worldVelocityZ();
        // The velocity removed by Kuruma's block collision points into the wall.
        double lostX = beforeX - afterX, lostZ = beforeZ - afterZ;
        double lostLength = Math.hypot(lostX, lostZ);
        if (lostLength < 0.05D) return;
        double nx = lostX / lostLength, nz = lostZ / lostLength;
        double incoming = beforeX * nx + beforeZ * nz;
        double remaining = afterX * nx + afterZ * nz;
        if (incoming <= 0.0D || incoming - remaining < 0.05D) return;
        double mass = Math.max(1.0D, car.getSpec().mass());
        // Collision impulse with an immovable block (inverse mass = 0).
        // The base physics has already removed the normal velocity, so apply
        // only the difference to the target post-impact normal velocity.
        double impulse = TownCollisionPhysics.impulse(incoming, 1.0D / mass, 0.0D,
                TownCollisionPhysics.restitution(TownCollisionPhysics.CAR_RESTITUTION,
                        TownCollisionPhysics.BLOCK_RESTITUTION));
        double targetNormal = incoming - impulse / mass;
        double correction = targetNormal - remaining;
        if (correction < 0.0D) state.addWorldVelocity(nx * correction, nz * correction);
    }

    /** Use the shared Kuruma CarHull collision implementation. */
    @Unique private void town$resolvePlayerCollision(CarEntity car) {
        com.kurumaintown.TownPlayerCollision.resolveSide(car);
    }

}
