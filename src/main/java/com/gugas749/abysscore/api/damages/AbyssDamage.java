package com.gugas749.abysscore.api.damages;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;

public class AbyssDamage {

    public static void deal(LivingEntity target, DamageSource source, float damage) {
        target.hurt(source, damage);
    }

    /**
     * Duration example: 100 is 5 seconds
     * Amplifier example: if the amplifier value given is 0 then the effect level is 1
     *                    if the amplifier value given is 1 then the effect level is 2
     */
    public static void deal(LivingEntity target, DamageSource source, float damage, Holder<MobEffect> effect, int duration, int amplifier) {
        if (target.hurt(source, damage)) {
            target.addEffect(new MobEffectInstance(effect, duration, amplifier));
        }
    }
}
