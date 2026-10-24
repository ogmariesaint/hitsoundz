package com.example.hitsounds.mixin;

import com.example.hitsounds.HitSoundsConfig;
import net.minecraft.client.sound.AbstractSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Название файла осталось старым, но класс теперь целится в AbstractSoundInstance
@Mixin(AbstractSoundInstance.class)
public class SoundManagerMixin {

    @Inject(method = "getPitch()F", at = @At("HEAD"), cancellable = true)
    private void hitsounds$pitch(CallbackInfoReturnable<Float> cir) {
        HitSoundsConfig cfg = HitSoundsConfig.get();
        if (!cfg.enabled) return;

        Identifier id = ((SoundInstance) (Object) this).getId();
        if (id == null || !"minecraft".equals(id.getNamespace())) return;

        Integer value = switch (id.getPath()) {
            case "entity.player.attack.crit" -> cfg.crit;
            case "entity.player.attack.sweep" -> cfg.sweep;
            case "entity.player.attack.knockback" -> cfg.sprint;
            default -> null;
        };

        if (value != null) {
            cir.setReturnValue(HitSoundsConfig.toPitch(value));
        }
    }
}
