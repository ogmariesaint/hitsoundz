package com.example.hitsounds;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import java.util.function.IntConsumer;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.random.Random;

public class HitSoundsScreen extends Screen {
    private final HitSoundsConfig cfg = HitSoundsConfig.get();

    public HitSoundsScreen() {
        super(Text.translatable("hitsounds.title"));
    }

    @Override
    protected void init() {
        int w = 200, sw = 140, tw = 56;
        int x = width / 2 - (sw + 4 + tw) / 2;
        int y = height / 2 - 60;

        addRow(x, y, sw, tw, "hitsounds.crit", cfg.crit, v -> cfg.crit = v, SoundEvents.ENTITY_PLAYER_ATTACK_CRIT);
        addRow(x, y + 26, sw, tw, "hitsounds.sweep", cfg.sweep, v -> cfg.sweep = v, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP);
        addRow(x, y + 52, sw, tw, "hitsounds.sprint", cfg.sprint, v -> cfg.sprint = v, SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK);

        addDrawableChild(ButtonWidget.builder(toggleText(), b -> {
            cfg.enabled = !cfg.enabled;
            b.setMessage(toggleText());
        }).dimensions(width / 2 - w / 2, y + 84, w, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("hitsounds.done"), b -> close())
                .dimensions(width / 2 - w / 2, y + 110, w, 20).build());
    }

    private void addRow(int x, int y, int sw, int tw, String key, int initial, IntConsumer setter, SoundEvent sound) {
        addDrawableChild(new PitchSlider(x, y, sw, Text.translatable(key), initial, setter));
        addDrawableChild(ButtonWidget.builder(Text.translatable("hitsounds.test"), b -> {
            // pitch подменит миксин по значению из конфига
            if (client.player != null) {
    client.getSoundManager().play(new PositionedSoundInstance(
            sound, SoundCategory.PLAYERS, 1.0f, 1.0f,
            Random.create(), client.player.getX(), client.player.getY(), client.player.getZ()));
}
        }).dimensions(x + sw + 4, y, tw, 20).build());
    }

    private Text toggleText() {
        return Text.translatable("hitsounds.enabled").append(": " + (cfg.enabled ? "ON" : "OFF"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 2 - 90, 0xFFFFFFFF);
    }

    @Override
    public void removed() {
        cfg.save();
    }

    private static class PitchSlider extends SliderWidget {
        private final Text label;
        private final IntConsumer setter;

        PitchSlider(int x, int y, int w, Text label, int initial, IntConsumer setter) {
            super(x, y, w, 20, Text.empty(), initial / 100.0);
            this.label = label;
            this.setter = setter;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            if (label == null) return; // вызов из конструктора родителя
            setMessage(label.copy().append(": " + Math.round(value * 100)));
        }

        @Override
        protected void applyValue() {
            setter.accept((int) Math.round(value * 100));
        }
    }
}
