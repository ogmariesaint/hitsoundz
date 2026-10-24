package com.example.hitsounds;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class HitSoundsConfig {
    private static final int VERSION = 3;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("hitsounds.json");
    private static HitSoundsConfig instance;

    public int version = 0;         // старые файлы (другой формат) сбрасываются
    public boolean enabled = true;
    public boolean visual = true;   // красная вспышка урона у цели
    public int delay = 0;           // задержка эффекта в мс, 0..100 (0 = сразу после клика)

    public static HitSoundsConfig get() {
        if (instance == null) instance = load();
        return instance;
    }

    private static HitSoundsConfig load() {
        try {
            if (Files.exists(PATH)) {
                HitSoundsConfig c = GSON.fromJson(Files.readString(PATH), HitSoundsConfig.class);
                if (c != null && c.version >= VERSION) {
                    c.delay = Math.max(0, Math.min(100, c.delay));
                    return c;
                }
            }
        } catch (Exception ignored) {}
        HitSoundsConfig fresh = new HitSoundsConfig();
        fresh.version = VERSION;
        return fresh;
    }

    public void save() {
        try {
            Files.writeString(PATH, GSON.toJson(this));
        } catch (IOException ignored) {}
    }
}
