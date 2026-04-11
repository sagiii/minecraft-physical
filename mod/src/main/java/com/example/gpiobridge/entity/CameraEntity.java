package com.example.gpiobridge.entity;

import com.example.gpiobridge.network.MqttBridgeClient;
import com.example.gpiobridge.screen.CameraIdScreenData;
import com.example.gpiobridge.screen.CameraIdScreenHandler;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Camera entity — an invisible marker entity that defines a streaming camera.
 *
 * The server subscribes to mp/bridge/m/cam/{id}/ctrl for parameter updates.
 * The client captures frames (currently: player's view) and publishes them to
 * mp/bridge/m/cam/{id}/frame at the configured FPS.
 *
 * Spawn with: /summon gpio_bridge:camera ~ ~ ~ {camId:1}
 */
public class CameraEntity extends Entity implements ExtendedMenuProvider<CameraIdScreenData> {

    // ---------- synced data ----------
    private static final EntityDataAccessor<Integer> DATA_CAM_ID =
            SynchedEntityData.defineId(CameraEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_WIDTH =
            SynchedEntityData.defineId(CameraEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_HEIGHT =
            SynchedEntityData.defineId(CameraEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_FPS =
            SynchedEntityData.defineId(CameraEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_PAN =
            SynchedEntityData.defineId(CameraEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_TILT =
            SynchedEntityData.defineId(CameraEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_FOV =
            SynchedEntityData.defineId(CameraEntity.class, EntityDataSerializers.FLOAT);

    public CameraEntity(EntityType<?> type, Level level) {
        super(type, level);
        setInvulnerable(true);
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_CAM_ID, 1);
        builder.define(DATA_WIDTH,  240);
        builder.define(DATA_HEIGHT, 135);
        builder.define(DATA_FPS,    1.0f);
        builder.define(DATA_PAN,    0.0f);
        builder.define(DATA_TILT,   0.0f);
        builder.define(DATA_FOV,    70.0f);
    }

    // ---------- persistent data ----------

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("camId",  getCamId());
        output.putInt("width",  getWidth());
        output.putInt("height", getHeight());
        output.putFloat("fps",  getFps());
        output.putFloat("pan",  getPan());
        output.putFloat("tilt", getTilt());
        output.putFloat("fov",  getFov());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        setCamId(input.getIntOr("camId", 1));
        setWidth(input.getIntOr("width", 240));
        setHeight(input.getIntOr("height", 135));
        setFps(input.getFloatOr("fps", 1.0f));
        setPan(input.getFloatOr("pan", 0.0f));
        setTilt(input.getFloatOr("tilt", 0.0f));
        setFov(input.getFloatOr("fov", 70.0f));
        // Sync pan/tilt to native entity rotation used by the camera renderer
        setYRot(getPan());
        setXRot(getTilt());
    }

    // ---------- lifecycle ----------

    @Override
    protected void setLevel(Level newLevel) {
        super.setLevel(newLevel);
        if (!newLevel.isClientSide() && newLevel instanceof ServerLevel sl) {
            MqttBridgeClient.INSTANCE.registerCamera(getCamId(), this);
            // Force-load the chunk so the camera keeps ticking even when the player is far away
            sl.setChunkForced(blockPosition().getX() >> 4, blockPosition().getZ() >> 4, true);
        }
    }

    @Override
    public void onRemoval(RemovalReason reason) {
        if (!level().isClientSide() && level() instanceof ServerLevel sl) {
            MqttBridgeClient.INSTANCE.unregisterCamera(getCamId());
            sl.setChunkForced(blockPosition().getX() >> 4, blockPosition().getZ() >> 4, false);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        // Left-click by a player removes the entity
        if (source.getEntity() instanceof Player) {
            discard();
            return true;
        }
        return false;
    }

    /** Called from UseEntityCallback registered in GpioBridgeMod. */
    public void openIdScreen(ServerPlayer player) {
        player.openMenu(this);
    }

    // ---------- ExtendedMenuProvider ----------

    @Override
    public CameraIdScreenData getScreenOpeningData(ServerPlayer player) {
        return new CameraIdScreenData(getId(), getCamId());
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Camera #" + getCamId());
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player player) {
        return new CameraIdScreenHandler(syncId, inv, getId(), getCamId());
    }

    // ---------- getters / setters ----------

    public int getCamId()   { return entityData.get(DATA_CAM_ID); }
    public int getWidth()   { return entityData.get(DATA_WIDTH); }
    public int getHeight()  { return entityData.get(DATA_HEIGHT); }
    public float getFps()   { return entityData.get(DATA_FPS); }
    public float getPan()   { return entityData.get(DATA_PAN); }
    public float getTilt()  { return entityData.get(DATA_TILT); }
    public float getFov()   { return entityData.get(DATA_FOV); }

    public void setCamId(int v)    { entityData.set(DATA_CAM_ID, v); }
    public void setWidth(int v)    { entityData.set(DATA_WIDTH, Math.max(16, Math.min(1920, v))); }
    public void setHeight(int v)   { entityData.set(DATA_HEIGHT, Math.max(16, Math.min(1080, v))); }
    public void setFps(float v)    { entityData.set(DATA_FPS, Math.max(0.1f, Math.min(20.0f, v))); }
    public void setPan(float v)    { entityData.set(DATA_PAN, v); }
    public void setTilt(float v)   { entityData.set(DATA_TILT, v); }
    public void setFov(float v)    { entityData.set(DATA_FOV, Math.max(10.0f, Math.min(170.0f, v))); }

    // ---------- apply ctrl payload ----------

    /**
     * Called on the server thread when the player confirms a new camId from the GUI.
     * Unregisters the old camId from MQTT and registers the new one.
     */
    public void applyNewCamId(int newId) {
        int oldId = getCamId();
        if (oldId == newId) return;
        MqttBridgeClient.INSTANCE.unregisterCamera(oldId);
        setCamId(newId);
        MqttBridgeClient.INSTANCE.registerCamera(newId, this);
    }

    /**
     * Called on the server thread when a ctrl MQTT message arrives.
     * Updates all provided non-null fields.
     */
    public void applyCtrl(Integer width, Integer height, Float fps,
                          Float pan, Float tilt, Float fov) {
        if (width  != null) setWidth(width);
        if (height != null) setHeight(height);
        if (fps    != null) setFps(fps);
        if (pan    != null) { setPan(pan);   setYRot(pan); }
        if (tilt   != null) { setTilt(tilt); setXRot(tilt); }
        if (fov    != null) setFov(fov);
        // Persist changes immediately
        if (level() instanceof ServerLevel sl) {
            sl.getChunkSource().blockChanged(blockPosition());
        }
    }
}
