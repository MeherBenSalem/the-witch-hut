package tn.nightbeam.betterwitchhuts.game;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import tn.nightbeam.betterwitchhuts.Constants;
import tn.nightbeam.betterwitchhuts.core.PlayerRecord;

/** The Coven's memory of each player: one JSON file per player in {@code <world>/data/better_witch_huts/players/}. */
public final class Records {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Map<UUID, PlayerRecord> LOADED = new ConcurrentHashMap<>();
    private static Path dir;

    private Records() {
    }

    public static void start(MinecraftServer server) {
        LOADED.clear();
        dir = server.getWorldPath(LevelResource.ROOT).resolve("data").resolve(Constants.MOD_ID).resolve("players");
    }

    public static void stop() {
        saveAll(false);
        LOADED.clear();
        dir = null;
    }

    public static PlayerRecord get(ServerPlayer player) {
        return get(player.getUUID());
    }

    public static PlayerRecord get(UUID id) {
        return LOADED.computeIfAbsent(id, Records::load);
    }

    public static void unload(UUID id) {
        PlayerRecord record = LOADED.remove(id);
        if (record != null && record.dirty) {
            save(id, record);
        }
    }

    public static void saveAll(boolean onlyDirty) {
        LOADED.forEach((id, record) -> {
            if (!onlyDirty || record.dirty) {
                save(id, record);
            }
        });
    }

    private static PlayerRecord load(UUID id) {
        if (dir != null) {
            Path file = dir.resolve(id + ".json");
            if (Files.exists(file)) {
                try {
                    PlayerRecord record = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), PlayerRecord.class);
                    if (record != null) {
                        return record.sanitize();
                    }
                } catch (IOException | RuntimeException e) {
                    Constants.LOG.error("Could not read {}; starting a new record (the file is kept as .broken)", file, e);
                    try {
                        Files.move(file, file.resolveSibling(id + ".json.broken"), StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException ignored) {
                        // Leave it; it will be overwritten on the next save.
                    }
                }
            }
        }
        return new PlayerRecord();
    }

    private static void save(UUID id, PlayerRecord record) {
        if (dir == null) {
            return;
        }
        try {
            Files.createDirectories(dir);
            Path file = dir.resolve(id + ".json");
            Path tmp = dir.resolve(id + ".json.tmp");
            Files.writeString(tmp, GSON.toJson(record), StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            record.dirty = false;
        } catch (IOException e) {
            Constants.LOG.error("Could not save the Coven record of {}", id, e);
        }
    }
}
