package com.gugas749.abysscore.api.effects;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.*;

public class AbyssEffectHandler {

    // UUID → list of effects to keep active on this entity
    private static final Map<UUID, List<MobEffectInstance>> continuousEffects = new HashMap<>();

    private static final int REFRESH_THRESHOLD = 200;

    public static void registerContinuousEffect(UUID target, List<MobEffectInstance> effects) {
        continuousEffects
                .computeIfAbsent(target, k -> new ArrayList<>())
                .addAll(effects);
    }

    public static void registerContinuousEffect(UUID target, MobEffectInstance effect) {
        registerContinuousEffect(target, List.of(effect));
    }

    public static void unregisterEveryContinuousEffect(UUID target) {
        continuousEffects.remove(target);
    }

    public static void unregisterContinuousEffect(UUID target, MobEffectInstance effect) {
        List<MobEffectInstance> effects = continuousEffects.get(target);
        if (effects != null) {
            effects.removeIf(e -> e.getEffect().equals(effect.getEffect()));
            if (effects.isEmpty()) {
                continuousEffects.remove(target);
            }
        }
    }

    @SubscribeEvent
    public void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;
        if (entity.level().isClientSide()) return;

        List<MobEffectInstance> effects = continuousEffects.get(entity.getUUID());
        if (effects == null) return;

        for (MobEffectInstance ce : effects) {
            var current = entity.getEffect(ce.getEffect());
            if (current == null || current.getDuration() < REFRESH_THRESHOLD) {
                entity.addEffect(new MobEffectInstance(ce));
            }
        }
    }

    // Player leave handler so the continous effects are removed
    @SubscribeEvent
    public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        continuousEffects.remove(event.getEntity().getUUID());
    }
}
