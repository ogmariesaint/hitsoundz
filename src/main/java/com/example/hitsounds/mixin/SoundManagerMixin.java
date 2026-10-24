package com.example.hitsounds.mixin;

import com.example.hitsounds.HitSoundsConfig;
import net.minecraft.client.sound.AbstractSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(SoundManager.class)
public class SoundManagerMixin {

    @ModifyVariable(
            method = "play(Lnet/minecraft/client/sound/SoundInstance;)",
            at = @At("HEAD"),
            argsOnly = true
    )
    private SoundInstance hitsounds$adjustPitch(SoundInstance sound) {
        HitSoundsConfig cfg = HitSoundsConfig.get();
        if (!cfg.enabled || !(sound instanceof AbstractSoundInstance)) return sound;

        Identifier id = sound.getId();
        if (!"minecraft".equals(id.getNamespace())) return sound;

        Integer value = switch (id.getPath()) {
            case "entity.player.attack.crit" -> cfg.crit;
            case "entity.player.attack.sweep" -> cfg.sweep;
            case "entity.player.attack.knockback" -> cfg.sprint;
            default -> null;
        };

        if (value != null) {
            ((AbstractSoundInstanceAccessor) sound).hitsounds$setPitch(HitSoundsConfig.toPitch(value));
        }
        return sound;
    }
}
