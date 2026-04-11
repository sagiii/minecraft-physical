package com.example.gpiobridge;

import com.example.gpiobridge.entity.CameraEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class ModEntities {

    public static final ResourceKey<EntityType<?>> CAMERA_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath("mp_bridge", "camera"));

    public static final EntityType<CameraEntity> CAMERA =
            Registry.register(
                    BuiltInRegistries.ENTITY_TYPE,
                    CAMERA_KEY,
                    EntityType.Builder.of(CameraEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .clientTrackingRange(128)
                            .build(CAMERA_KEY));

    public static void initialize() {
        // Registration happens in static field initializers above.
        // Calling this method ensures the class is loaded.
    }
}
