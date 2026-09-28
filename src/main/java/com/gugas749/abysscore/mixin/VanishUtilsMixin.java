package com.gugas749.abysscore.mixin;

import com.gugas749.abysscore.features.vanish.ACVanishExtras;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// @Pseudo: Vanishmod is NOT on AbyssCore's compile classpath (it's only installed on the server).
// Without this, the Mixin annotation processor fails the build because it can't find the target class.
// At runtime: if Vanishmod is present the mixin applies, if not it's silently skipped.
@Pseudo
@Mixin(targets = "redstonedubstep.mods.vanishmod.VanishUtil", remap = false)
public class VanishUtilsMixin {

    @Inject(
            method = "playerAllowedToSeeOther(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;ZZ)Z",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void abysscore$customVisibility(
            Entity subject,
            Entity otherPlayer,
            boolean isSubjectVanished,
            boolean isOtherVanished,
            CallbackInfoReturnable<Boolean> cir) {

        if (!isOtherVanished) return;

        Boolean override = ACVanishExtras.checkVisibility(
                otherPlayer.getUUID(),  // the vanished player
                subject.getUUID()       // the viewer trying to see them
        );

        if (override != null) {
            cir.setReturnValue(override);
        }
    }
}