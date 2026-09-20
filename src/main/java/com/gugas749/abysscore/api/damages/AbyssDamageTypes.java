package com.gugas749.abysscore.api.damages;

import com.gugas749.abysscore.Abysscore;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageType;

public class AbyssDamageTypes {
    public static final ResourceKey<DamageType>
            CORRUPTION_DAMAGE = key("corruption_damage"),
            TEST_DAMAGE = key("test_damage");

    private static ResourceKey<DamageType> key(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, Abysscore.asResource(name));
    }

    public static void bootstrap(BootstrapContext<DamageType> ctx) {
        new DamageTypeBuilder(CORRUPTION_DAMAGE).scaling(DamageScaling.ALWAYS).exhaustion(0.1f).register(ctx);
        new DamageTypeBuilder(TEST_DAMAGE).scaling(DamageScaling.ALWAYS).register(ctx);
    }
}
