package fr.lumaa.cidercraft;

import fr.lumaa.cidercraft.data.CiderCraftConfig;
import fr.lumaa.cidercraft.data.DataManager;
import fr.lumaa.cidercraft.websocket.CiderData;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;

public class CiderCraftClient implements ClientModInitializer {
	public static DataManager dataManager;
	public static CiderCraftConfig config;
	public static CiderData websocket;

	@Override
	public void onInitializeClient() {
		CiderCraftClient.dataManager = new DataManager(MinecraftClient.getInstance());
		CiderCraftClient.config = CiderCraftClient.dataManager.readJson(CiderCraftConfig.class);

		if (CiderCraftClient.config == null) {
			CiderCraft.LOGGER.error("[CiderCraft] Couldn't decode CiderCraftConfig from config file. Restored default settings.");
			CiderCraftClient.config = CiderCraftConfig.defaultSettings();
			CiderCraftClient.dataManager.writeJson(CiderCraftConfig.defaultSettings());
		}

		CiderCraftClient.websocket = new CiderData(CiderCraftClient.config);
        CiderCraftClient.websocket.connectWebSocket();
    }
}