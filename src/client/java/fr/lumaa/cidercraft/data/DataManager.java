package fr.lumaa.cidercraft.data;

import com.google.gson.Gson;
import fr.lumaa.cidercraft.CiderCraft;
import net.minecraft.client.MinecraftClient;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

public class DataManager {
    public MinecraftClient client;

    public DataManager(MinecraftClient client) {
        this.client = client;
    }

    public <T extends EditableData> void writeJson(T data) {
        Path filePath = this.getPath().resolve(CiderCraft.MOD_ID + ".json");
        try {
            Files.writeString(filePath, data.encodeJson());
            CiderCraft.LOGGER.info("[CiderCraft] Successfully saved data to " + CiderCraft.MOD_ID + ".json");
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    private Path getPath() {
        return this.client.runDirectory.toPath();
    }

    public <T extends EditableData> T readJson(Class<T> clazz) {
        Path filePath = this.getPath().resolve(CiderCraft.MOD_ID + ".json");
        try (Reader reader = Files.newBufferedReader(filePath, java.nio.charset.StandardCharsets.UTF_8)) {
            Gson gson = new Gson();
            return gson.fromJson(reader, clazz);
        } catch (java.io.IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
