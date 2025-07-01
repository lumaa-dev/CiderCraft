package fr.lumaa.cidercraft.data;

import com.google.gson.Gson;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import fr.lumaa.cidercraft.CiderCraftClient;
import fr.lumaa.cidercraft.websocket.CiderData;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;

public class CiderCraftConfig implements EditableData {
    public String token;
    public String url;

    @Override
    public String encodeJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    @Override
    public void decodeJson(File fromFile) {
        try (Reader reader = new InputStreamReader(new java.io.FileInputStream(fromFile), java.nio.charset.StandardCharsets.UTF_8)) {
            Gson gson = new Gson();
            CiderCraftConfig loaded = gson.fromJson(reader, CiderCraftConfig.class);
            if (loaded != null) {
                this.token = loaded.token;
            } else {
                CiderCraftConfig restored = CiderCraftConfig.defaultSettings();
                this.token = restored.token;
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    public static CiderCraftConfig defaultSettings() {
        CiderCraftConfig newConfig = new CiderCraftConfig();
        newConfig.token = "";
        newConfig.url = "http://localhost:10767";

        return newConfig;
    }

    public Screen createSettings(Screen parent) {
        return YetAnotherConfigLib.createBuilder()
                .title(Text.literal("CiderCraft"))
                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("Core"))
                        .tooltip(Text.literal("Settings that make CiderCraft work properly, the core settings of the mod"))
                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("Authentication"))
                                .description(OptionDescription.of(Text.literal("Authenticate CiderCraft by creating a Cider token, or change the default request URL")))
                                .option(Option.<String>createBuilder()
                                        .name(Text.literal("Cider Token"))
                                        .description(OptionDescription.of(Text.literal("The Cider Token allows you to see your playing track, change tracks, etc... Without one, you cannot do anything.")))
                                        .binding("", () -> this.token, newVal -> this.token = newVal)
                                        .controller(StringControllerBuilder::create)
                                        .build())
                                .option(Option.<String>createBuilder()
                                        .name(Text.literal("URL to request"))
                                        .description(OptionDescription.createBuilder()
                                                .build())
                                        .binding("http://localhost:10767", () -> this.url, newVal -> this.url = newVal)
                                        .controller(StringControllerBuilder::create)
                                        .build())
                                .build())
                        .build())
                .save(() -> {
                    CiderCraftClient.dataManager.writeJson(this);

                    CiderCraftClient.websocket = new CiderData(this);
                    CiderCraftClient.websocket.connectWebSocket();
                })
                .build()
                .generateScreen(parent);
    }
}
