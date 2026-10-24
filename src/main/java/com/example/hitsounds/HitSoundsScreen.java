package com.example.hitsounds;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;

public class HitSoundsScreen extends Screen {
    // палитра
    private static final int BG = 0xF0141420;
    private static final int BORDER = 0xFF2E2E45;
    private static final int ACCENT = 0xFF8B6CFF;
    private static final int ACCENT_DIM = 0xFF4A3B96;
    private static final int TRACK = 0xFF2A2A3E;
    private static final int ROW_BG = 0xFF1C1C2C;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int MUTED = 0xFF9A9AB5;
    private static final int OFF = 0xFF4A4A60;

    // размеры
    private static final int PANEL_W = 270;
    private static final int PANEL_H = 262;
    private static final int PAD = 16;
    private static final int INNER_W = PANEL_W - PAD * 2;

    private final HitSoundsConfig cfg = HitSoundsConfig.get();
    private int px, py;

    public HitSoundsScreen() {
        super(Text.translatable("hitsounds.title"));
    }

    @Override
    protected void init() {
        px = (width - PANEL_W) / 2;
        py = (height - PANEL_H) / 2;
        int x = px + PAD;

        addRow(x, py + 54, "hitsounds.crit", cfg.crit, v -> cfg.crit = v, SoundEvents.ENTITY_PLAYER_ATTACK_CRIT);
        addRow(x, py + 90, "hitsounds.sweep", cfg.sweep, v -> cfg.sweep = v, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP);
        addRow(x, py + 126, "hitsounds.sprint", cfg.sprint, v -> cfg.sprint = v, SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK);

        addDrawableChild(new ToggleRow(x, py + 180, INNER_W, 22, Text.translatable("hitsounds.visual"),
                () -> cfg.visual, () -> cfg.visual = !cfg.visual));
        addDrawableChild(new ToggleRow(x, py + 206, INNER_W, 22, Text.translatable("hitsounds.enabled"),
                () -> cfg.enabled, () -> cfg.enabled = !cfg.enabled));

        addDrawableChild(new StyledButton(x, py + 234, INNER_W, 20, Text.translatable("hitsounds.done"), b -> close()));
    }

    private void addRow(int x, int y, String key, int initial, IntConsumer setter, SoundEvent sound) {
        int testW = 52;
        PingSlider slider = new PingSlider(x, y, INNER_W - testW - 6, 30, Text.translatable(key), initial, setter);
        addDrawableChild(slider);
        addDrawableChild(new StyledButton(x + INNER_W - testW, y + 5, testW, 20, Text.translatable("hitsounds.test"), b -> {
            if (client != null && client.player != null) {
                // тест: звук с выбранной задержкой
                int delay = (int) Math.round(slider.getValue() * 100);
                double ex = client.player.getX(), ey = client.player.getY(), ez = client.player.getZ();
                HitPredictor.runAfter(delay, () -> HitPredictor.playSound(sound, ex, ey, ez));
            }
        }));
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);

        roundedFill(context, px - 1, py - 1, PANEL_W + 2, PANEL_H + 2, BORDER);
        roundedFill(context, px, py, PANEL_W, PANEL_H, BG);

        TextRenderer tr = textRenderer;
        context.drawTextWithShadow(tr, Text.translatable("hitsounds.title").formatted(Formatting.BOLD),
                px + PAD, py + 12, WHITE);
        context.drawTextWithShadow(tr, Text.translatable("hitsounds.subtitle"), px + PAD, py + 25, MUTED);
        context.fill(px + PAD, py + 38, px + PAD + 28, py + 40, ACCENT);

        context.drawTextWithShadow(tr, Text.translatable("hitsounds.hint"), px + PAD, py + 164, MUTED);
    }

    @Override
    public void removed() {
        cfg.save();
    }

    // ---------- рисование ----------

    static void roundedFill(DrawContext c, int x, int y, int w, int h, int color) {
        c.fill(x + 2, y, x + w - 2, y + h, color);
        c.fill(x + 1, y + 1, x + w - 1, y + h - 1, color);
        c.fill(x, y + 2, x + w, y + h - 2, color);
    }

    // ---------- виджеты ----------

    private static class PingSlider extends SliderWidget {
        private final Text label;
        private final IntConsumer setter;

        PingSlider(int x, int y, int w, int h, Text label, int initial, IntConsumer setter) {
            super(x, y, w, h, Text.empty(), initial / 100.0);
            this.label = label;
            this.setter = setter;
            updateMessage();
        }

        double getValue() {
            return value;
        }

        @Override
        protected void updateMessage() {
            if (label == null) return; // вызов из конструктора родителя
            setMessage(label.copy().append(": " + Math.round(value * 100) + " ")
                    .append(Text.translatable("hitsounds.ms")));
        }

        @Override
        protected void applyValue() {
            setter.accept((int) Math.round(value * 100));
        }

        @Override
        public void renderWidget(DrawContext c, int mouseX, int mouseY, float delta) {
            TextRenderer tr = MinecraftClient.getInstance().textRenderer;
            int x = getX(), y = getY(), w = getWidth();

            c.drawTextWithShadow(tr, label, x, y + 2, WHITE);
            Text val = Text.literal(Math.round(value * 100) + " ").append(Text.translatable("hitsounds.ms"));
            c.drawTextWithShadow(tr, val, x + w - tr.getWidth(val), y + 2, ACCENT);

            // дорожка совпадает с зоной клика ванильного слайдера (отступ 4 px)
            int tx = x + 4, tw = w - 8, ty = y + 17, th = 6;
            roundedFill(c, tx, ty, tw, th, TRACK);
            int filled = (int) Math.round(tw * value);
            if (filled > 0) roundedFill(c, tx, ty, Math.max(filled, 4), th, ACCENT_DIM);

            int kx = tx + filled - 3;
            int kc = (isHovered() || isFocused()) ? WHITE : 0xFFD8D0FF;
            roundedFill(c, kx, ty - 3, 6, 12, kc);
        }
    }

    private static class StyledButton extends ButtonWidget {
        private final Text label;

        StyledButton(int x, int y, int w, int h, Text label, PressAction action) {
            super(x, y, w, h, label, action, DEFAULT_NARRATION_SUPPLIER);
            this.label = label;
        }

        @Override
        public void renderWidget(DrawContext c, int mouseX, int mouseY, float delta) {
            TextRenderer tr = MinecraftClient.getInstance().textRenderer;
            roundedFill(c, getX(), getY(), getWidth(), getHeight(), isHovered() ? ACCENT : ACCENT_DIM);
            c.drawCenteredTextWithShadow(tr, label, getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2, WHITE);
        }
    }

    private static class ToggleRow extends ButtonWidget {
        private final Text label;
        private final BooleanSupplier state;

        ToggleRow(int x, int y, int w, int h, Text label, BooleanSupplier state, Runnable toggle) {
            super(x, y, w, h, label, b -> toggle.run(), DEFAULT_NARRATION_SUPPLIER);
            this.label = label;
            this.state = state;
        }

        @Override
        public void renderWidget(DrawContext c, int mouseX, int mouseY, float delta) {
            TextRenderer tr = MinecraftClient.getInstance().textRenderer;
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            boolean on = state.getAsBoolean();

            roundedFill(c, x, y, w, h, isHovered() ? 0xFF24243A : ROW_BG);
            c.drawTextWithShadow(tr, label, x + 8, y + (h - 8) / 2, WHITE);

            // переключатель-«таблетка»
            int sw = 28, sh = 12;
            int sx = x + w - sw - 8, sy = y + (h - sh) / 2;
            roundedFill(c, sx, sy, sw, sh, on ? ACCENT : OFF);
            int kx = on ? sx + sw - sh + 1 : sx + 1;
            roundedFill(c, kx, sy + 1, sh - 2, sh - 2, WHITE);
        }
    }
}
